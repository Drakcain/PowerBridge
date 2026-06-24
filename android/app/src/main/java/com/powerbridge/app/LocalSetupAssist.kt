package com.powerbridge.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.NetworkCapabilities
import java.net.Inet4Address

data class LocalNetworkSuggestion(
    val transportLabel: String,
    val isWifi: Boolean,
    val isLocalLanCapable: Boolean,
    val phoneIpv4: String? = null,
    val prefixLength: Int? = null,
    val networkAddress: String? = null,
    val subnetDisplay: String? = null,
    val broadcastIp: String? = null,
    val gatewayIp: String? = null,
    val suggestedHomeSubnetPrefix: String? = null,
    val isPrivateLan: Boolean = false,
    val warning: String? = null
)

object LocalSetupAssist {
    fun detect(context: Context): LocalNetworkSuggestion {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork ?: return disconnectedResult("No active network")
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return disconnectedResult("Unknown network")
            val linkProperties = cm.getLinkProperties(activeNetwork)

            val transportLabel = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet connected"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular connected"
                else -> "Other network connected"
            }

            val localLanCapable = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val ipv4Address = linkProperties?.linkAddresses?.firstIpv4()
            val gateway = linkProperties?.routes
                ?.firstOrNull { it.gateway is Inet4Address }
                ?.gateway
                ?.hostAddress

            if (!localLanCapable) {
                return LocalNetworkSuggestion(
                    transportLabel = transportLabel,
                    isWifi = false,
                    isLocalLanCapable = false,
                    warning = "Local Wi-Fi Wake setup works best while this phone is connected to the same Wi-Fi as your PC."
                )
            }

            if (ipv4Address == null) {
                return LocalNetworkSuggestion(
                    transportLabel = transportLabel,
                    isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    isLocalLanCapable = true,
                    gatewayIp = gateway,
                    warning = "Connected to a local network, but PowerBridge could not read an IPv4 address for auto-fill."
                )
            }

            val ipString = ipv4Address.address.hostAddress ?: return LocalNetworkSuggestion(
                transportLabel = transportLabel,
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isLocalLanCapable = true,
                gatewayIp = gateway,
                warning = "Connected to a local network, but PowerBridge could not read an IPv4 address for auto-fill."
            )
            val prefix = ipv4Address.prefixLength.toInt()
            val networkAddress = calculateNetworkAddress(ipString, prefix)
            val broadcastIp = calculateBroadcastAddress(ipString, prefix)

            return LocalNetworkSuggestion(
                transportLabel = transportLabel,
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isLocalLanCapable = true,
                phoneIpv4 = ipString,
                prefixLength = prefix,
                networkAddress = networkAddress,
                subnetDisplay = networkAddress?.let { "$it/$prefix" },
                broadcastIp = broadcastIp,
                gatewayIp = gateway,
                suggestedHomeSubnetPrefix = networkAddress?.let { toRoutePrefix(it, prefix) },
                isPrivateLan = isPrivateLanIpv4(ipString),
                warning = when {
                    broadcastIp == null -> "PowerBridge could not calculate a broadcast IP from the current network."
                    else -> null
                }
            )
        } catch (_: Exception) {
            disconnectedResult("Network state unavailable")
        }
    }

    private fun disconnectedResult(label: String): LocalNetworkSuggestion {
        return LocalNetworkSuggestion(
            transportLabel = label,
            isWifi = false,
            isLocalLanCapable = false,
            warning = "Local Wi-Fi Wake setup works best while this phone is connected to the same Wi-Fi as your PC."
        )
    }

    private fun List<LinkAddress>.firstIpv4(): LinkAddress? {
        return firstOrNull { it.address is Inet4Address }
    }

    private fun calculateNetworkAddress(ip: String, prefixLength: Int): String? {
        val ipInt = ipv4ToInt(ip) ?: return null
        val mask = prefixToMask(prefixLength)
        return intToIpv4(ipInt and mask)
    }

    private fun calculateBroadcastAddress(ip: String, prefixLength: Int): String? {
        val ipInt = ipv4ToInt(ip) ?: return null
        val mask = prefixToMask(prefixLength)
        return intToIpv4((ipInt and mask) or mask.inv())
    }

    private fun ipv4ToInt(ip: String): Int? {
        val parts = ip.split(".")
        if (parts.size != 4) return null
        var result = 0
        for (part in parts) {
            val value = part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
            result = (result shl 8) or value
        }
        return result
    }

    private fun intToIpv4(value: Int): String {
        return listOf(
            value ushr 24 and 0xFF,
            value ushr 16 and 0xFF,
            value ushr 8 and 0xFF,
            value and 0xFF
        ).joinToString(".")
    }

    private fun prefixToMask(prefixLength: Int): Int {
        if (prefixLength <= 0) return 0
        if (prefixLength >= 32) return -1
        return (-1 shl (32 - prefixLength))
    }

    private fun isPrivateLanIpv4(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return when {
            parts[0] == 10 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            else -> false
        }
    }

    private fun toRoutePrefix(networkAddress: String, prefixLength: Int): String? {
        val octets = networkAddress.split(".")
        if (octets.size != 4) return null
        return when {
            prefixLength >= 24 -> octets.take(3).joinToString(".") + "."
            prefixLength >= 16 -> octets.take(2).joinToString(".") + "."
            prefixLength >= 8 -> octets.take(1).joinToString(".") + "."
            else -> null
        }
    }
}
