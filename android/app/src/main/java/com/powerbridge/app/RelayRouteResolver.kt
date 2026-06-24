package com.powerbridge.app

import android.content.Context

object RelayRouteResolver {
    fun resolve(context: Context, config: AppConfig): SelectedRelayRoute {
        val snapshot = DiagnosticsStore.collectNetworkSnapshot(context)
        val localLanTransport = snapshot.transport.contains("Wi-Fi", ignoreCase = true) ||
            snapshot.transport.contains("Ethernet", ignoreCase = true)
        val subnetMatch = config.homeWifiSubnet.isNotBlank() && snapshot.phoneIp.startsWith(config.homeWifiSubnet)
        val gatewayMatch = AppConfigStore.isValidIpCandidate(config.expectedGateway) && snapshot.gateway == config.expectedGateway
        val homeMatch = localLanTransport && (subnetMatch || gatewayMatch)

        return when (config.routeMode) {
            RelayRouteMode.AUTO -> {
                if (homeMatch) {
                    SelectedRelayRoute(
                        displayName = "Home Wi-Fi Relay",
                        baseUrl = config.homeRelayBaseUrl,
                        reason = when {
                            subnetMatch && gatewayMatch ->
                                "Phone is on the local LAN with IP ${snapshot.phoneIp} matching subnet ${config.homeWifiSubnet} and gateway ${config.expectedGateway}"
                            subnetMatch ->
                                "Phone is on the local LAN with IP ${snapshot.phoneIp} matching subnet ${config.homeWifiSubnet}"
                            else ->
                                "Phone is on the local LAN with gateway ${snapshot.gateway} matching expected gateway ${config.expectedGateway}"
                        },
                        mode = config.routeMode,
                        isManualOverride = false,
                        isHomeWifiMatch = true
                    )
                } else {
                    SelectedRelayRoute(
                        displayName = "Remote Public Relay",
                        baseUrl = config.remoteRelayBaseUrl,
                        reason = when {
                            snapshot.transport.contains("Wi-Fi", ignoreCase = true) ->
                                "Phone is on Wi-Fi, but it does not match the configured home subnet/gateway."
                            snapshot.transport.contains("Ethernet", ignoreCase = true) ->
                                "Phone is on Ethernet, but it does not match the configured home subnet/gateway."
                            else -> "Phone is on cellular or not connected to a matching local LAN."
                        },
                        mode = config.routeMode,
                        isManualOverride = false,
                        isHomeWifiMatch = false
                    )
                }
            }

            RelayRouteMode.FORCE_HOME_WIFI -> {
                SelectedRelayRoute(
                    displayName = "Manual Override - Home Wi-Fi Relay",
                    baseUrl = config.homeRelayBaseUrl,
                    reason = "Manual override is forcing the home Wi-Fi relay.",
                    mode = config.routeMode,
                    isManualOverride = true,
                    isHomeWifiMatch = homeMatch,
                    mismatchWarning = if (!homeMatch) {
                        "Route mismatch detected: Phone does not appear to be on the expected local LAN, but the app is using the local relay. Use Remote Public Relay or Auto mode."
                    } else {
                        null
                    }
                )
            }

            RelayRouteMode.FORCE_REMOTE_PUBLIC -> {
                SelectedRelayRoute(
                    displayName = "Manual Override - Remote Public Relay",
                    baseUrl = config.remoteRelayBaseUrl,
                    reason = "Manual override is forcing the remote public relay.",
                    mode = config.routeMode,
                    isManualOverride = true,
                    isHomeWifiMatch = homeMatch,
                    mismatchWarning = if (homeMatch) {
                        "Route mismatch detected: Phone appears to be on the expected local LAN, but the app is using the remote public relay. Switch route mode to Auto or Home Wi-Fi Relay."
                    } else {
                        null
                    }
                )
            }
        }
    }
}
