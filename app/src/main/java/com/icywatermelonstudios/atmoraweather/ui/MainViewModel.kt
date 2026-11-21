package com.icywatermelonstudios.atmoraweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.icywatermelonstudios.atmoraweather.WeatherApp
import com.icywatermelonstudios.atmoraweather.data.model.Location
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.data.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// state of the weather download for currently selected location
data class WeatherState(
    val report: WeatherReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// everything the screens need
data class MainUiState(
    // false until saved settings are read from disk. prevents a flash of the empty "no location" screen
    val isReady: Boolean = false,
    val locations: List<Location> = emptyList(),
    val homeLocationId: Long? = null,
    val selectedLocation: Location? = null,
    val units: UnitSettings = UnitSettings(),
    val weather: WeatherState = WeatherState(),
)

// state of the add location search dialog
data class SearchState(
    val query: String = "",
    val results: List<Location> = emptyList(),
    val isSearching: Boolean = false,
    val noResults: Boolean = false,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = getApplication<WeatherApp>().container
    private val store = container.settingsStore
    private val repository = container.repository

    private data class SettingsSnapshot(
        val locations: List<Location>,
        val homeId: Long?,
        val selectedId: Long?,
        val units: UnitSettings,
    ) {
        val selectedLocation: Location?
            get() = locations.firstOrNull { it.id == selectedId }
                ?: locations.firstOrNull { it.id == homeId }
                ?: locations.firstOrNull()
    }

    private val settings: StateFlow<SettingsSnapshot?> = combine(
        store.locations,
        store.homeLocationId,
        store.selectedLocationId,
        store.units,
    ) { locations, homeId, selectedId, units ->
        SettingsSnapshot(locations, homeId, selectedId, units)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val weather = MutableStateFlow(WeatherState())

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<MainUiState> = combine(settings, weather) { s, w ->
        if (s == null) {
            MainUiState()
        } else {
            MainUiState(
                isReady = true,
                locations = s.locations,
                homeLocationId = s.homeId,
                selectedLocation = s.selectedLocation,
                units = s.units,
                weather = w,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    private val _search = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _search

    private var searchJob: Job? = null

    private data class LoadRequest(val location: Location?, val units: UnitSettings, val tick: Int)

    init {
        // reload the weather whenever something updates
        viewModelScope.launch {
            combine(settings.filterNotNull(), refreshTick) { s, tick ->
                LoadRequest(s.selectedLocation, s.units, tick)
            }
                .distinctUntilChanged()
                .collectLatest { request ->
                    val location = request.location
                    if (location == null) {
                        weather.value = WeatherState()
                    } else {
                        loadWeather(location, request.units)
                    }
                }
        }
    }

    private suspend fun loadWeather(location: Location, units: UnitSettings) {
        // keep the old report only if it belongs to the same location, otherwise show a clean loading state
        weather.update { old ->
            old.copy(
                report = old.report?.takeIf { it.location.id == location.id },
                isLoading = true,
                error = null,
            )
        }
        repository.getWeather(location, units)
            .onSuccess { report -> weather.value = WeatherState(report = report) }
            .onFailure { e ->
                weather.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
    }

    fun refresh() {
        refreshTick.update { it + 1 }
    }

    fun selectLocation(id: Long) {
        viewModelScope.launch { store.selectLocation(id) }
    }

    fun addLocation(location: Location) {
        viewModelScope.launch { store.addLocation(location) }
        clearSearch()
    }

    fun removeLocation(id: Long) {
        viewModelScope.launch { store.removeLocation(id) }
    }

    fun setHomeLocation(id: Long) {
        viewModelScope.launch { store.setHomeLocation(id) }
    }

    fun setUnits(units: UnitSettings) {
        viewModelScope.launch { store.setUnits(units) }
    }

    // called on every keystroke, waits 400 ms of silence before hitting the network
    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        _search.update { it.copy(query = query, error = null, noResults = false) }

        if (query.trim().length < 2) {
            _search.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _search.update { it.copy(isSearching = true) }
            repository.searchCities(query)
                .onSuccess { found ->
                    _search.update {
                        it.copy(results = found, isSearching = false, noResults = found.isEmpty())
                    }
                }
                .onFailure { e ->
                    _search.update {
                        it.copy(results = emptyList(), isSearching = false, error = e.toUserMessage())
                    }
                }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchState()
    }
}