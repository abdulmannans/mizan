package app.mizan.android.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mizan.android.data.repo.FundDetailState
import app.mizan.android.data.repo.FundRow
import app.mizan.android.data.repo.HomeState
import app.mizan.android.data.repo.LastJob
import app.mizan.android.data.repo.MetalDetailState
import app.mizan.android.data.repo.MetalRow
import app.mizan.android.data.repo.MissedState
import app.mizan.android.data.repo.MizanRepository
import app.mizan.android.data.settings.MizanSettings
import app.mizan.android.data.settings.SettingsRepository
import app.mizan.android.notify.MizanNotifier
import app.mizan.android.work.MizanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val notifier: MizanNotifier,
    private val context: Context,
) : ViewModel() {

    val state: StateFlow<MizanSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MizanSettings())

    fun createChannels() = notifier.createChannels()

    fun acknowledgeAndStart(
        displayName: String,
        notifyFundDips: Boolean,
        notifyMetalDips: Boolean,
        notifyGoldDrop: Boolean,
    ) {
        viewModelScope.launch {
            if (displayName.isNotBlank()) settings.setDisplayName(displayName.trim())
            settings.setNotifyFundDips(notifyFundDips)
            settings.setNotifyMetalDips(notifyMetalDips)
            settings.setNotifyGoldDrop(notifyGoldDrop)
            settings.acknowledgeDisclaimer()
            MizanScheduler.startBackfill(context)
        }
    }
}

data class HomeUiState(
    val home: HomeState = HomeState(),
    val backfillRunning: Boolean = false,
    val refreshing: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: MizanRepository,
    private val context: Context,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.observeHomeWithMissed(),
        MizanScheduler.observeBackfillRunning(context),
        MizanScheduler.observeRefreshRunning(context),
    ) { home, backfill, refreshing ->
        HomeUiState(home = home, backfillRunning = backfill, refreshing = refreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() = MizanScheduler.refreshNow(context)
}

data class FundsUiState(
    val rows: List<FundRow> = emptyList(),
    val query: String = "",
    val settings: MizanSettings = MizanSettings(),
)

@HiltViewModel
class FundsViewModel @Inject constructor(
    private val repository: MizanRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<FundsUiState> = combine(
        repository.observeFundRows(),
        query,
        repository.settings,
    ) { rows, search, settings ->
        val needle = search.trim().lowercase()
        val filtered = rows.filter { row ->
            needle.isEmpty() ||
                row.fund.name.lowercase().contains(needle) ||
                row.fund.amc.lowercase().contains(needle) ||
                row.fund.schemeCode.toString().contains(needle)
        }
        FundsUiState(rows = filtered, query = search, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FundsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleWatchlist(row: FundRow) {
        viewModelScope.launch { repository.toggleWatchlist(row.fund.schemeCode, row.watchlisted) }
    }
}

@HiltViewModel
class FundDetailViewModel @Inject constructor(
    private val repository: MizanRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val schemeCode: Long = savedState.get<String>("schemeCode")?.toLongOrNull() ?: 0L

    val state: StateFlow<FundDetailState> = repository.observeFundDetail(schemeCode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FundDetailState())

    fun toggleWatchlist(watchlisted: Boolean) {
        viewModelScope.launch { repository.toggleWatchlist(schemeCode, watchlisted) }
    }

}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: MizanRepository,
) : ViewModel() {

    val state: StateFlow<FundsUiState> = combine(
        repository.observeFundRows(),
        repository.settings,
    ) { rows, settings ->
        FundsUiState(rows = rows.filter { it.watchlisted }, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FundsUiState())

    fun remove(row: FundRow) {
        viewModelScope.launch { repository.removeFromWatchlist(row.fund.schemeCode) }
    }

}

@HiltViewModel
class MissedViewModel @Inject constructor(repository: MizanRepository) : ViewModel() {
    val state: StateFlow<MissedState> = repository.observeMissed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissedState())
}

@HiltViewModel
class MetalsViewModel @Inject constructor(repository: MizanRepository) : ViewModel() {
    val state: StateFlow<List<MetalRow>> = repository.observeMetalRows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class MetalDetailViewModel @Inject constructor(
    repository: MizanRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val metalId: String = savedState.get<String>("metalId") ?: "gold"

    val state: StateFlow<MetalDetailState> = repository.observeMetalDetail(metalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetalDetailState())
}

data class AccountUiState(
    val settings: MizanSettings = MizanSettings(),
    val lastJob: LastJob? = null,
    val notificationsAllowed: Boolean = true,
    val backfillRunning: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: MizanRepository,
    private val notifier: MizanNotifier,
    private val context: Context,
) : ViewModel() {

    val state: StateFlow<AccountUiState> = combine(
        settings.settings,
        repository.observeRecentJobs().map { it.firstOrNull() },
        MizanScheduler.observeBackfillRunning(context),
    ) { prefs, lastJob, backfill ->
        AccountUiState(
            settings = prefs,
            lastJob = lastJob,
            notificationsAllowed = notifier.canPost(),
            backfillRunning = backfill,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun setDisplayName(value: String) = viewModelScope.launch { settings.setDisplayName(value) }

    fun setLumpsum(value: Double) = viewModelScope.launch { settings.setAvailableLumpsum(value) }

    fun setWhatIf(value: Double) = viewModelScope.launch { settings.setWhatIfAmount(value) }

    fun setNotificationsEnabled(value: Boolean) =
        viewModelScope.launch { settings.setNotificationsEnabled(value) }

    fun setNotifyFunds(value: Boolean) = viewModelScope.launch { settings.setNotifyFundDips(value) }

    fun setNotifyMetals(value: Boolean) =
        viewModelScope.launch { settings.setNotifyMetalDips(value) }

    fun setNotifyGoldDrop(value: Boolean) =
        viewModelScope.launch { settings.setNotifyGoldDrop(value) }

    fun runUpdateNow() = MizanScheduler.refreshNow(context)

    fun reloadHistory() = MizanScheduler.reloadHistory(context)
}

/** Decides between onboarding and the main shell, and keeps channels registered. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: MizanRepository,
    notifier: MizanNotifier,
    private val context: Context,
) : ViewModel() {

    init {
        notifier.createChannels()
        viewModelScope.launch { autoRefreshIfStale() }
    }

    private suspend fun autoRefreshIfStale() {
        val prefs = settings.current()
        if (!prefs.disclaimerAcknowledged || !prefs.backfillCompleted) return
        val latestNavDate = repository.latestFundNavDate() ?: return
        val hoursSinceNav = java.time.Duration.between(
            latestNavDate.atStartOfDay(),
            app.mizan.android.core.IndiaClock.now(),
        ).toHours()
        if (hoursSinceNav > STALE_HOURS) {
            MizanScheduler.refreshNow(context)
        }
    }

    val acknowledged: StateFlow<Boolean?> = settings.settings
        .map { it.disclaimerAcknowledged }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val STALE_HOURS = 18L
    }
}
