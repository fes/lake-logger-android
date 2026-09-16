package com.feslabs.lakelogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.deviceSettingsDataStore by preferencesDataStore(name = "device_settings")

/**
 * Persists the logger device's local-network IP address (e.g.
 * "10.2.12.247"), entered once by the user, so the app can talk to it
 * directly over the LAN without needing mDNS/Bonjour support (the device
 * firmware does not currently advertise itself).
 */
class DeviceSettingsStore(private val context: Context) {
    private val ipAddressKey = stringPreferencesKey("device_local_ip_address")

    suspend fun loadIpAddress(): String? =
        context.deviceSettingsDataStore.data.first()[ipAddressKey]

    suspend fun saveIpAddress(address: String?) {
        val trimmed = address?.trim()
        context.deviceSettingsDataStore.edit { prefs ->
            if (trimmed.isNullOrEmpty()) {
                prefs.remove(ipAddressKey)
            } else {
                prefs[ipAddressKey] = trimmed
            }
        }
    }
}
