package com.powerbridge.app

import android.content.Context
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class ActionResult(
    val status: String,
    val detail: String,
    val summary: String
)

private data class StepResult(
    val success: Boolean,
    val httpStatus: Int? = null,
    val body: String = "",
    val endpointMissing: Boolean = false,
    val errorType: String? = null,
    val errorMessage: String? = null
)

class RelayOps(
    private val context: Context,
    private val config: AppConfig
) {
    companion object {
        private const val ACTION_CHECK_RELAY = "Check Relay"
        private const val PORT = 9
        private const val HEALTH_TIMEOUT_SECONDS = 5L
        private const val STATUS_TIMEOUT_SECONDS = 5L
        private const val WAKE_TIMEOUT_SECONDS = 8L
        private const val POLL_INTERVAL_MS = 3000L
    }

    private val selectedRoute = RelayRouteResolver.resolve(context, config)

    private val healthClient = OkHttpClient.Builder()
        .connectTimeout(HEALTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HEALTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val statusClient = OkHttpClient.Builder()
        .connectTimeout(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val wakeClient = OkHttpClient.Builder()
        .connectTimeout(WAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(WAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun getSelectedRoute(): SelectedRelayRoute = selectedRoute

    fun testRelay(): ActionResult {
        DiagnosticsStore.setLastState(context, ACTION_CHECK_RELAY, "Checking relay...", "Starting relay health check.")
        recordNetworkStep(ACTION_CHECK_RELAY)
        recordRouteDecision(ACTION_CHECK_RELAY)
        val validation = validateCoreSettings(ACTION_CHECK_RELAY, requireToken = false)
        if (validation != null) return validation

        val healthUrl = AppConfigStore.healthUrl(selectedRoute.baseUrl)
        val health = executeStep(
            action = ACTION_CHECK_RELAY,
            step = "Relay Health Check",
            method = "GET",
            url = healthUrl,
            client = healthClient
        )

        if (health.success && health.httpStatus == 200) {
            val summary = "Likely issue: none detected. Relay health endpoint returned 200 via ${selectedRoute.displayName}."
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult("Relay online", "Health check passed on ${selectedRoute.displayName}.", summary)
        }

        if (health.httpStatus == 404) {
            val activate = executeStep(
                action = ACTION_CHECK_RELAY,
                step = "Legacy Activate Reachability",
                method = "POST",
                url = AppConfigStore.legacyActivateUrl(selectedRoute.baseUrl),
                body = "{}".toRequestBody("application/json".toMediaType()),
                client = wakeClient
            )
            if (activate.success && activate.httpStatus in listOf(200, 202, 401, 405, 429)) {
                val summary = "Likely issue: relay is online on ${selectedRoute.displayName}, but /health is missing."
                DiagnosticsStore.setSummary(context, summary)
                return ActionResult(
                    "Relay online",
                    "Health endpoint missing, but a compatibility path responded on ${selectedRoute.displayName}.",
                    summary
                )
            }
        }

        val summary = summaryForFailure(health, "Relay Health Check")
        DiagnosticsStore.setSummary(context, summary)
        return ActionResult(
            if (selectedRoute.isHomeWifiMatch || selectedRoute.baseUrl.startsWith("http://")) "Home relay offline." else "Remote relay offline.",
            shortFailureDetail("GET", healthUrl, health),
            summary
        )
    }

    suspend fun runWake(mode: WakeMode, onProgress: suspend (String) -> Unit): ActionResult {
        val action = mode.actionLabel
        DiagnosticsStore.setLastState(context, action, mode.sendingStatus, "Preparing request via ${selectedRoute.displayName}.")
        recordNetworkStep(action)
        recordRouteDecision(action)
        val validation = validateCoreSettings(action, requireToken = true)
        if (validation != null) return validation

        val healthUrl = AppConfigStore.healthUrl(selectedRoute.baseUrl)
        val health = executeStep(
            action = action,
            step = "Relay Health Check",
            method = "GET",
            url = healthUrl,
            client = healthClient
        )
        if (!health.success && health.httpStatus != 404) {
            val summary = summaryForFailure(health, "Relay Health Check")
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult(
                if (selectedRoute.baseUrl.startsWith("http://")) "Home relay offline." else "Remote relay offline.",
                shortFailureDetail("GET", healthUrl, health),
                summary
            )
        }

        val packetCount = if (mode == WakeMode.SLEEP) config.sleepWakePackets else config.remoteBootPackets
        val packetDelayMs = if (mode == WakeMode.SLEEP) 350L else 1000L
        val timeoutMs = (if (mode == WakeMode.SLEEP) config.sleepWakeTimeoutSeconds else config.remoteBootTimeoutSeconds) * 1000L
        val bodyBuilder = {
            JSONObject().apply {
                put("mac", config.targetMac)
                put("broadcast", config.broadcastIp)
                put("port", PORT)
                put("mode", mode.pathSegment)
                put("packets", packetCount)
                put("timeoutSeconds", timeoutMs / 1000L)
            }.toString().toRequestBody("application/json".toMediaType())
        }

        val endpoints = listOf(
            AppConfigStore.wakeModeUrl(selectedRoute.baseUrl, mode),
            AppConfigStore.wakeQueryUrl(selectedRoute.baseUrl, mode),
            AppConfigStore.legacyActivateUrl(selectedRoute.baseUrl)
        )

        var workingEndpoint: String? = null
        for (packet in 1..packetCount) {
            onProgress(mode.sendingStatus)
            val send = if (workingEndpoint != null) {
                executeStep(
                    action = action,
                    step = "Wake Packet $packet",
                    method = "POST",
                    url = workingEndpoint,
                    body = bodyBuilder(),
                    client = wakeClient
                )
            } else {
                tryWakeFallbacks(action, packet, endpoints, bodyBuilder())
            }

            if (!send.success) {
                val summary = summaryForWakeFailure(send, mode)
                DiagnosticsStore.setSummary(context, summary)
                return ActionResult(
                    if (send.httpStatus == 401 || send.httpStatus == 403) "Relay token issue." else "Wake request failed.",
                    shortFailureDetail("POST", guessedFailedUrl(endpoints), send),
                    summary
                )
            }

            if (workingEndpoint == null) {
                workingEndpoint = successfulEndpoint(endpoints, action, packet)
            }
            if (packet < packetCount) delay(packetDelayMs)
        }

        if (!canVerifyPcOnline()) {
            val summary = if (mode == WakeMode.SLEEP) {
                "Likely issue: wake request was sent through ${selectedRoute.displayName}, but target verification is unavailable because target PC IP or relay /status is missing."
            } else {
                "Likely issue: remote boot packets were sent through ${selectedRoute.displayName}, but target verification is unavailable because target PC IP or relay /status is missing."
            }
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult(
                if (mode == WakeMode.SLEEP) "Wake packet sent." else "Remote boot packets sent.",
                "Route: ${selectedRoute.displayName}. Target verification unavailable. Set Target PC IP or relay /status support.",
                summary
            )
        }

        val startedAt = System.currentTimeMillis()
        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            val stage = mode.stageStatuses[stageIndex(mode, System.currentTimeMillis() - startedAt, timeoutMs)]
            onProgress(stage)
            if (isPcOnline(action)) {
                val summary = if (mode == WakeMode.SLEEP) {
                    "Likely issue: none detected. Sleep wake flow reached desktop readiness through ${selectedRoute.displayName}."
                } else {
                    "Likely issue: none detected. Remote boot flow reached PC online state through ${selectedRoute.displayName}."
                }
                DiagnosticsStore.setSummary(context, summary)
                return ActionResult(mode.successStatus, "Route: ${selectedRoute.displayName}. $stage successful.", summary)
            }
            delay(POLL_INTERVAL_MS)
        }

        val summary = if (mode == WakeMode.SLEEP) {
            "Likely issue: relay sent wake packets through ${selectedRoute.displayName}, but the target PC did not appear online in time. Check target IP, sleep state, Ethernet, and NIC wake settings."
        } else {
            "Likely issue: relay sent boot packets through ${selectedRoute.displayName}, but the target PC did not appear online in time. Check BIOS wake support, power-state wake settings, Ethernet, and NIC Wake-on-LAN settings."
        }
        DiagnosticsStore.setSummary(context, summary)
        return ActionResult(mode.timeoutStatus, "Route: ${selectedRoute.displayName}. Timeout after ${timeoutMs / 1000L}s waiting for target readiness.", summary)
    }

    fun runDiagnostics(): ActionResult {
        val action = "Run Diagnostics"
        DiagnosticsStore.setLastState(context, action, "Running diagnostics...", "Collecting local checks and relay endpoint results.")
        recordNetworkStep(action)
        recordRouteDecision(action)
        val validation = validateCoreSettings(action, requireToken = false)
        if (validation != null) return validation

        recordConfigValidation(action)
        val health = executeStep(
            action = action,
            step = "Relay Health Check",
            method = "GET",
            url = AppConfigStore.healthUrl(selectedRoute.baseUrl),
            client = healthClient
        )
        val status = executeStep(
            action = action,
            step = "Relay Status Check",
            method = "GET",
            url = AppConfigStore.statusUrl(selectedRoute.baseUrl),
            client = statusClient,
            token = config.token.takeIf { it.isNotBlank() }
        )

        val summary = when {
            !selectedRoute.mismatchWarning.isNullOrBlank() -> selectedRoute.mismatchWarning
            !health.success && health.httpStatus == null -> summaryForFailure(health, "Relay Health Check")
            health.httpStatus == 404 -> "Likely issue: relay is online on ${selectedRoute.displayName}, but /health is missing. Check relay API paths or rely on compatibility fallback."
            status.httpStatus == 404 || status.httpStatus == 405 -> "Likely issue: relay is online on ${selectedRoute.displayName}, but /status is missing. Target verification may be limited."
            status.httpStatus == 401 || status.httpStatus == 403 -> "Likely issue: relay requires a token and the saved token is missing or wrong."
            !AppConfigStore.isValidIpCandidate(config.targetIp) -> "Likely issue: target PC IP is missing or invalid, so online verification is limited."
            else -> "Likely issue: none obvious. Review the diagnostic events for route and endpoint-specific details."
        }
        DiagnosticsStore.setSummary(context, summary)
        return ActionResult("Diagnostics ready.", "Route: ${selectedRoute.displayName}. Diagnostics complete. Review the summary or copy the log.", summary)
    }

    private fun tryWakeFallbacks(action: String, packet: Int, endpoints: List<String>, body: RequestBody): StepResult {
        var endpointMissing = false
        var lastFailure: StepResult? = null
        endpoints.forEachIndexed { index, url ->
            val step = when (index) {
                0 -> "Wake Packet $packet Preferred Endpoint"
                1 -> "Wake Packet $packet Compatibility Endpoint"
                else -> "Wake Packet $packet Legacy Endpoint"
            }
            val result = executeStep(
                action = action,
                step = step,
                method = "POST",
                url = url,
                client = wakeClient,
                token = config.token,
                body = body
            )
            if (result.success) return result
            if (result.endpointMissing) {
                endpointMissing = true
                lastFailure = result
            } else {
                return result
            }
        }
        return lastFailure ?: StepResult(false, endpointMissing = endpointMissing)
    }

    private fun successfulEndpoint(endpoints: List<String>, action: String, packet: Int): String? {
        val packetPrefix = "Wake Packet $packet"
        return DiagnosticsStore.getEvents(context)
            .lastOrNull { it.action == action && it.step.startsWith(packetPrefix) && it.success }
            ?.url
            ?: endpoints.lastOrNull()
    }

    private fun canVerifyPcOnline(): Boolean {
        return AppConfigStore.isValidIpCandidate(config.targetIp) || relaySupportsStatus()
    }

    private fun relaySupportsStatus(): Boolean {
        val result = executeStep(
            action = "Status Capability Check",
            step = "Status Capability Check",
            method = "GET",
            url = AppConfigStore.statusUrl(selectedRoute.baseUrl),
            client = statusClient,
            token = config.token.takeIf { it.isNotBlank() }
        )
        return result.success || (result.httpStatus != 404 && result.httpStatus != 405 && result.httpStatus != null)
    }

    private fun isPcOnline(action: String): Boolean {
        val statusResult = executeStep(
            action = action,
            step = "Relay Status Poll",
            method = "GET",
            url = AppConfigStore.statusUrl(selectedRoute.baseUrl),
            client = statusClient,
            token = config.token.takeIf { it.isNotBlank() }
        )
        if (statusResult.success) {
            val body = statusResult.body
            if (body.isNotBlank()) {
                runCatching {
                    val json = JSONObject(body)
                    val boolKeys = listOf("desktopReady", "online", "ready", "reachable", "awake")
                    if (boolKeys.any { json.optBoolean(it, false) }) return true
                    val state = listOf(json.optString("state"), json.optString("status"), json.optString("message"))
                        .joinToString(" ")
                        .lowercase()
                    if (state.contains("online") || state.contains("ready") || state.contains("awake")) return true
                }
            }
        }

        if (!AppConfigStore.isValidIpCandidate(config.targetIp)) return false

        val started = System.currentTimeMillis()
        val reachable = try {
            InetAddress.getByName(config.targetIp).isReachable(1500)
        } catch (_: Exception) {
            false
        }
        val duration = System.currentTimeMillis() - started
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Target PC Reachability",
                method = "PING",
                url = config.targetIp,
                success = reachable,
                durationMs = duration,
                suggestedCause = if (reachable) "Target PC responded to reachability check." else "Target PC did not respond to direct reachability check.",
                suggestedFix = if (reachable) "No action needed." else "Confirm target PC IP, Ethernet readiness, and that the PC has finished waking."
            )
        )
        return reachable
    }

    private fun validateCoreSettings(action: String, requireToken: Boolean): ActionResult? {
        if (AppConfigStore.isPlaceholderRelayBaseUrl(selectedRoute.baseUrl)) {
            val detail = if (selectedRoute.baseUrl.startsWith("https://")) {
                "Remote relay URL is still the sample placeholder. Replace it with your own relay URL or choose Local Wi-Fi Wake."
            } else {
                "Home relay URL is still the sample placeholder. Replace it with your own relay URL or choose Local Wi-Fi Wake."
            }
            recordFailureEvent(
                action,
                "Relay Placeholder Check",
                "LOCAL",
                selectedRoute.baseUrl,
                detail,
                "Sample relay URL is still in use.",
                "Replace the placeholder relay URL in Settings or switch to Local Wi-Fi Wake."
            )
            val summary = detail
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult("Setup needed.", detail, summary)
        }
        val baseUrl = AppConfigStore.validateBaseUrl(selectedRoute.baseUrl)
        if (baseUrl != null) {
            recordFailureEvent(action, "Relay Base URL Validation", "LOCAL", selectedRoute.baseUrl, baseUrl, "Invalid relay URL.", "Update the selected relay URL in Settings.")
            val summary = "Likely issue: selected relay URL is invalid."
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult(baseUrl, "Invalid relay URL. Open Settings.", summary)
        }
        if (requireToken && config.token.isBlank()) {
            recordFailureEvent(action, "Relay Token Validation", "LOCAL", "settings://token", "Relay token is missing.", "Token/auth issue.", "Add the relay token in Settings.")
            val summary = "Likely issue: relay requires a token and the saved token is missing or wrong."
            DiagnosticsStore.setSummary(context, summary)
            return ActionResult("Relay token is required.", "Token missing in Settings.", summary)
        }
        if (!selectedRoute.mismatchWarning.isNullOrBlank()) {
            DiagnosticsStore.recordEvent(
                context,
                DiagnosticEvent(
                    timestamp = DiagnosticsStore.timestampNow(),
                    action = action,
                    step = "Route Mismatch Warning",
                    method = "LOCAL",
                    url = selectedRoute.baseUrl,
                    success = false,
                    errorMessage = selectedRoute.mismatchWarning,
                    suggestedCause = "Selected route does not match the phone network context.",
                    suggestedFix = "Switch route mode to Auto or pick the route that matches the current network."
                )
            )
        }
        return null
    }

    private fun recordConfigValidation(action: String) {
        val checks = listOf(
            Triple(
                "Target PC IP Validation",
                config.targetIp,
                when {
                    config.targetIp.isBlank() -> "Target PC IP missing. Verification will be limited."
                    AppConfigStore.isPlaceholderTargetIp(config.targetIp) -> "Target PC IP is still the sample placeholder."
                    !AppConfigStore.isValidIpCandidate(config.targetIp) -> "Target PC IP invalid."
                    else -> ""
                }
            ),
            Triple(
                "Broadcast IP Validation",
                config.broadcastIp,
                when {
                    AppConfigStore.isPlaceholderBroadcastIp(config.broadcastIp) -> "Broadcast IP is still the sample placeholder."
                    !AppConfigStore.isValidIpCandidate(config.broadcastIp) -> "Broadcast IP invalid."
                    else -> ""
                }
            ),
            Triple(
                "Target MAC Validation",
                config.targetMac,
                when {
                    AppConfigStore.isPlaceholderTargetMac(config.targetMac) -> "Target MAC is still the sample placeholder."
                    !Regex("""^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$""").matches(config.targetMac) -> "Target MAC invalid."
                    else -> ""
                }
            )
        )
        checks.forEach { (step, value, failure) ->
            DiagnosticsStore.recordEvent(
                context,
                DiagnosticEvent(
                    timestamp = DiagnosticsStore.timestampNow(),
                    action = action,
                    step = step,
                    method = "LOCAL",
                    url = value.ifBlank { "Not set" },
                    success = failure.isBlank(),
                    errorMessage = failure.ifBlank { null },
                    suggestedCause = if (failure.isBlank()) "Configuration value looks valid." else failure,
                    suggestedFix = if (failure.isBlank()) "No action needed." else "Fix this value in Settings."
                )
            )
        }
    }

    private fun recordNetworkStep(action: String) {
        val snapshot = DiagnosticsStore.collectNetworkSnapshot(context)
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Phone Network Check",
                method = "LOCAL",
                url = snapshot.transport,
                success = snapshot.connected,
                errorMessage = if (snapshot.connected) null else "No active network detected.",
                suggestedCause = if (snapshot.connected) "Phone network is available." else "Phone is not connected to Wi-Fi or another active network.",
                suggestedFix = if (snapshot.connected) "No action needed." else "Connect the phone to the same LAN as your relay."
            )
        )
    }

    private fun recordRouteDecision(action: String) {
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = "Relay Route Decision",
                method = "LOCAL",
                url = selectedRoute.baseUrl,
                success = selectedRoute.mismatchWarning.isNullOrBlank(),
                errorMessage = selectedRoute.mismatchWarning,
                suggestedCause = selectedRoute.reason,
                suggestedFix = selectedRoute.mismatchWarning ?: "No action needed."
            )
        )
    }

    private fun executeStep(
        action: String,
        step: String,
        method: String,
        url: String,
        client: OkHttpClient,
        token: String? = null,
        body: RequestBody? = null
    ): StepResult {
        val started = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url)
            if (!token.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $token")
            val request = when (method) {
                "POST" -> builder
                    .addHeader("Content-Type", "application/json")
                    .post(body ?: "{}".toRequestBody("application/json".toMediaType()))
                    .build()
                else -> builder.get().build()
            }
            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - started
                val result = StepResult(
                    success = response.isSuccessful,
                    httpStatus = response.code,
                    body = response.body?.string().orEmpty(),
                    endpointMissing = response.code == 404 || response.code == 405
                )
                DiagnosticsStore.recordEvent(
                    context,
                    DiagnosticEvent(
                        timestamp = DiagnosticsStore.timestampNow(),
                        action = action,
                        step = step,
                        method = method,
                        url = url,
                        success = response.isSuccessful,
                        httpStatusCode = response.code,
                        durationMs = duration,
                        suggestedCause = causeForHttp(response.code),
                        suggestedFix = fixForHttp(response.code)
                    )
                )
                result
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - started
            val cause = causeForException(e)
            val fix = fixForException(e)
            DiagnosticsStore.recordEvent(
                context,
                DiagnosticEvent(
                    timestamp = DiagnosticsStore.timestampNow(),
                    action = action,
                    step = step,
                    method = method,
                    url = url,
                    success = false,
                    errorType = e::class.java.simpleName,
                    errorMessage = e.message,
                    durationMs = duration,
                    suggestedCause = cause,
                    suggestedFix = fix
                )
            )
            StepResult(
                success = false,
                errorType = e::class.java.simpleName,
                errorMessage = e.message
            )
        }
    }

    private fun causeForHttp(code: Int): String = when (code) {
        200, 202 -> "Relay responded successfully."
        401, 403 -> "Relay requires a token and the token may be missing or wrong."
        404, 405 -> "Relay responded, but this endpoint is missing or not allowed."
        429 -> "Relay reported a cooldown or rate limit."
        in 500..599 -> "Relay answered but failed internally."
        else -> "Relay responded with HTTP $code."
    }

    private fun fixForHttp(code: Int): String = when (code) {
        200, 202 -> "No action needed."
        401, 403 -> "Update the relay token in Settings."
        404, 405 -> "Check relay API paths or rely on compatibility fallback."
        429 -> "Wait a few seconds and try again."
        in 500..599 -> "Check the relay service logs or restart the relay service."
        else -> "Review relay configuration and endpoint support."
    }

    private fun causeForException(e: Exception): String = when (e) {
        is SocketTimeoutException -> "Relay did not respond before timeout."
        is ConnectException -> "Relay host is reachable, but nothing is listening on that port."
        is UnknownHostException -> "Relay host name could not be resolved."
        is IOException -> "Phone could not reach the relay IP/port."
        else -> "Unexpected error while contacting the relay."
    }

    private fun fixForException(e: Exception): String = when (e) {
        is SocketTimeoutException -> if (selectedRoute.baseUrl.startsWith("https://")) {
            "Remote relay timed out: check your public domain, reverse proxy, DNS, firewall, or relay tunnel/proxy service."
        } else {
            "Home relay timed out: check relay power, relay service, relay IP, Wi-Fi, firewall, and port 8787."
        }
        is ConnectException -> "Confirm the relay service is running and listening on the configured port."
        is UnknownHostException -> "Check the relay host/IP spelling in Settings."
        is IOException -> if (selectedRoute.baseUrl.startsWith("https://")) {
            "Confirm the public relay domain and remote proxy path are reachable from the phone."
        } else {
            "Confirm the phone is on the home LAN and the local relay IP/port is reachable."
        }
        else -> "Open Diagnostics and review the recorded error details."
    }

    private fun shortFailureDetail(method: String, url: String, result: StepResult): String {
        val reason = when {
            result.httpStatus != null -> {
                val bodyNote = result.body.trim().takeIf { it.isNotBlank() && it.length <= 80 }?.let { " ($it)" }.orEmpty()
                "HTTP ${result.httpStatus}$bodyNote"
            }
            !result.errorType.isNullOrBlank() -> "${result.errorType}: ${result.errorMessage.orEmpty()}".trim()
            else -> "Unknown failure"
        }
        return "Failed: $method $url. Reason: $reason. Open Diagnostics for details."
    }

    private fun summaryForFailure(result: StepResult, step: String): String {
        return when {
            !selectedRoute.mismatchWarning.isNullOrBlank() -> selectedRoute.mismatchWarning
            result.httpStatus == 404 || result.httpStatus == 405 ->
                "Likely issue: relay is online on ${selectedRoute.displayName} but the $step endpoint is missing. Next step: update the relay API path or use compatibility fallback."
            result.httpStatus == 401 || result.httpStatus == 403 ->
                "Likely issue: relay requires a token and the saved token is missing or wrong. Next step: update relay token in Settings."
            result.httpStatus in 500..599 ->
                "Likely issue: relay answered but failed internally. Next step: check the relay service on the host side."
            result.errorType == SocketTimeoutException::class.java.simpleName ->
                if (selectedRoute.baseUrl.startsWith("https://")) {
                    "Likely issue: remote public relay did not answer on port 443. Next step: check your public domain, reverse proxy, DNS, firewall, or relay tunnel/proxy service."
                } else {
                    "Likely issue: phone could not reach the home relay on the local LAN. Next step: confirm relay IP, Wi-Fi, relay service, firewall, and port 8787."
                }
            result.errorType == ConnectException::class.java.simpleName ->
                "Likely issue: ${selectedRoute.displayName} host is reachable but the service is not listening on that port. Next step: confirm relay service and port."
            else ->
                "Likely issue: phone cannot reach ${selectedRoute.displayName} at the configured address. Next step: confirm network, IP, relay service, and firewall."
        }
    }

    private fun summaryForWakeFailure(result: StepResult, mode: WakeMode): String {
        return when {
            !selectedRoute.mismatchWarning.isNullOrBlank() -> selectedRoute.mismatchWarning
            result.httpStatus == 404 || result.httpStatus == 405 ->
                "Likely issue: relay is online on ${selectedRoute.displayName}, but ${mode.pathSegment} wake endpoint is missing. Next step: update the relay server or allow fallback to /activate."
            result.httpStatus == 401 || result.httpStatus == 403 ->
                "Likely issue: relay requires a token and the saved token is missing or wrong. Next step: update relay token in Settings."
            result.errorType == SocketTimeoutException::class.java.simpleName ->
                if (selectedRoute.baseUrl.startsWith("https://")) {
                    "Likely issue: remote public relay timed out. Next step: check your public domain, reverse proxy, DNS, firewall, or relay tunnel/proxy service."
                } else {
                    "Likely issue: local home relay timed out. Next step: check relay power, relay service, relay IP, Wi-Fi, firewall, and port 8787."
                }
            result.errorType == ConnectException::class.java.simpleName ->
                "Likely issue: relay host is reachable but the wake service is not listening on that port. Next step: confirm relay service state."
            else ->
                "Likely issue: wake request failed before the relay confirmed it. Next step: review Diagnostics for the exact route, endpoint, and error."
        }
    }

    private fun guessedFailedUrl(endpoints: List<String>): String {
        val last = DiagnosticsStore.getEvents(context).lastOrNull()
        return last?.url ?: endpoints.firstOrNull().orEmpty()
    }

    private fun recordFailureEvent(action: String, step: String, method: String, url: String, message: String, cause: String, fix: String) {
        DiagnosticsStore.recordEvent(
            context,
            DiagnosticEvent(
                timestamp = DiagnosticsStore.timestampNow(),
                action = action,
                step = step,
                method = method,
                url = url,
                success = false,
                errorMessage = message,
                suggestedCause = cause,
                suggestedFix = fix
            )
        )
    }

    private fun stageIndex(mode: WakeMode, elapsedMs: Long, timeoutMs: Long): Int {
        if (mode.stageStatuses.size == 1) return 0
        val progress = (elapsedMs.toDouble() / timeoutMs.toDouble()).coerceIn(0.0, 0.999)
        return (progress * mode.stageStatuses.size).toInt().coerceIn(0, mode.stageStatuses.lastIndex)
    }
}
