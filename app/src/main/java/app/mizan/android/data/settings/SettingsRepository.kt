package app.mizan.android.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class MizanSettings(
    val displayName: String = "",
    val disclaimerAcknowledged: Boolean = false,
    val availableLumpsum: Double = 50_000.0,
    val whatIfAmount: Double = 10_000.0,
    val notificationsEnabled: Boolean = true,
    val notifyFundDips: Boolean = true,
    val notifyMetalDips: Boolean = true,
    val notifyGoldDrop: Boolean = true,
    val backfillCompleted: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mizan_settings")

@Singleton
class SettingsRepository @Inject constructor(private val context: Context) {

    val settings: Flow<MizanSettings> = context.dataStore.data.map { prefs ->
        MizanSettings(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            disclaimerAcknowledged = prefs[Keys.DISCLAIMER] ?: false,
            availableLumpsum = prefs[Keys.LUMPSUM] ?: 50_000.0,
            whatIfAmount = prefs[Keys.WHAT_IF] ?: 10_000.0,
            notificationsEnabled = prefs[Keys.NOTIFY_ALL] ?: true,
            notifyFundDips = prefs[Keys.NOTIFY_FUNDS] ?: true,
            notifyMetalDips = prefs[Keys.NOTIFY_METALS] ?: true,
            notifyGoldDrop = prefs[Keys.NOTIFY_GOLD_DROP] ?: true,
            backfillCompleted = prefs[Keys.BACKFILL_DONE] ?: false,
        )
    }

    suspend fun current(): MizanSettings = settings.first()

    suspend fun setDisplayName(value: String) = edit { it[Keys.DISPLAY_NAME] = value }

    suspend fun acknowledgeDisclaimer() = edit { it[Keys.DISCLAIMER] = true }

    suspend fun setAvailableLumpsum(value: Double) = edit { it[Keys.LUMPSUM] = value }

    suspend fun setWhatIfAmount(value: Double) = edit { it[Keys.WHAT_IF] = value }

    suspend fun setNotificationsEnabled(value: Boolean) = edit { it[Keys.NOTIFY_ALL] = value }

    suspend fun setNotifyFundDips(value: Boolean) = edit { it[Keys.NOTIFY_FUNDS] = value }

    suspend fun setNotifyMetalDips(value: Boolean) = edit { it[Keys.NOTIFY_METALS] = value }

    suspend fun setNotifyGoldDrop(value: Boolean) = edit { it[Keys.NOTIFY_GOLD_DROP] = value }

    suspend fun setBackfillCompleted(value: Boolean) = edit { it[Keys.BACKFILL_DONE] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val DISCLAIMER = booleanPreferencesKey("disclaimer_acknowledged")
        val LUMPSUM = doublePreferencesKey("available_lumpsum")
        val WHAT_IF = doublePreferencesKey("what_if_amount")
        val NOTIFY_ALL = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_FUNDS = booleanPreferencesKey("notify_fund_dips")
        val NOTIFY_METALS = booleanPreferencesKey("notify_metal_dips")
        val NOTIFY_GOLD_DROP = booleanPreferencesKey("notify_gold_drop")
        val BACKFILL_DONE = booleanPreferencesKey("backfill_completed")
    }
}
