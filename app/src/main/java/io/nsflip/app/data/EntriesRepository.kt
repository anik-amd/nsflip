// SPDX-License-Identifier: GPL-3.0-or-later
package io.nsflip.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "nsflip")

/** Custom entry: friendly name + DoT hostname. */
@Serializable
data class CustomEntry(val name: String, val hostname: String)

object EntriesRepository {
    private val KEY_CUSTOM = stringPreferencesKey("custom_entries")
    private val KEY_LAST_USED = stringPreferencesKey("last_used_hostname")
    private val json = Json { ignoreUnknownKeys = true }

    private fun encode(entries: List<CustomEntry>): String =
        json.encodeToString(entries)

    private fun decode(raw: String?): List<CustomEntry> =
        runCatching { json.decodeFromString<List<CustomEntry>>(raw ?: "[]") }
            .getOrElse { emptyList() }

    fun customEntries(context: Context): Flow<List<CustomEntry>> =
        context.dataStore.data.map { decode(it[KEY_CUSTOM]) }

    fun lastUsed(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_LAST_USED] }

    suspend fun lastUsedOnce(context: Context): String? =
        context.dataStore.data.first()[KEY_LAST_USED]

    suspend fun addCustom(context: Context, entry: CustomEntry) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_CUSTOM]) + entry
            prefs[KEY_CUSTOM] = encode(list)
        }
    }

    suspend fun updateCustom(context: Context, hostname: String, newName: String) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_CUSTOM]).map {
                if (it.hostname == hostname) it.copy(name = newName) else it
            }
            prefs[KEY_CUSTOM] = encode(list)
        }
    }

    suspend fun deleteCustom(context: Context, hostname: String) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_CUSTOM]).filterNot { it.hostname == hostname }
            prefs[KEY_CUSTOM] = encode(list)
        }
    }

    suspend fun setLastUsed(context: Context, hostname: String?) {
        context.dataStore.edit { prefs ->
            if (hostname == null) prefs.remove(KEY_LAST_USED) else prefs[KEY_LAST_USED] = hostname
        }
    }
}
