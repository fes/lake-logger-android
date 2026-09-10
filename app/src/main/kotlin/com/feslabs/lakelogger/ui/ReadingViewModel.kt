package com.feslabs.lakelogger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.feslabs.lakelogger.data.LakeApiException
import com.feslabs.lakelogger.data.LakeReading
import com.feslabs.lakelogger.data.LakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val reading: LakeReading? = null,
    val history: List<LakeReading> = emptyList(),
    val lastFetchedAtEpochMillis: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LakeRepository(application)
    private val historyDays = 7

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Show cached data immediately on launch, before the first network
        // round-trip completes, so the UI never opens on an empty state if
        // the widget (or a previous app session) already populated the cache.
        viewModelScope.launch {
            repository.cachedReading()?.let { cached ->
                _uiState.value = _uiState.value.copy(
                    reading = cached.reading,
                    lastFetchedAtEpochMillis = cached.fetchedAtEpochMillis,
                )
            }
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch { refreshCurrent() }
        viewModelScope.launch { refreshHistory() }
    }

    private suspend fun refreshCurrent() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            val latest = repository.refreshCurrent()
            _uiState.value = _uiState.value.copy(
                reading = latest ?: _uiState.value.reading,
                lastFetchedAtEpochMillis = System.currentTimeMillis(),
                isLoading = false,
            )
        } catch (e: LakeApiException) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
        }
    }

    private suspend fun refreshHistory() {
        try {
            val history = repository.refreshHistory(historyDays)
            _uiState.value = _uiState.value.copy(history = history)
        } catch (e: LakeApiException) {
            // History is a secondary/nice-to-have view; don't clobber a
            // successful "current reading" error state with a history fetch
            // failure. Only surface it if we have nothing else to show.
            if (_uiState.value.reading == null) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
}
