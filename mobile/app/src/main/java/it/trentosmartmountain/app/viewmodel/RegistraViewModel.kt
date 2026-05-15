package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.location.LocationSnapshot
import it.trentosmartmountain.app.data.location.UserLocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistraViewModel(application: Application) : AndroidViewModel(application) {

  data class UiState(
    val hasLocationPermission: Boolean = false,
    val userLocation: LocationSnapshot? = null,
    val gpsSignalLevel: Int = 0,
    val gpsAccuracyLabel: String? = null,
    val centerOnUserTick: Int = 0,
    val locationPermissionDenied: Boolean = false,
  )

  private val locationTracker = UserLocationTracker(application)
  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      locationTracker.location.collect { snapshot ->
        _uiState.update { state ->
          state.copy(
            userLocation = snapshot,
            gpsSignalLevel =
              snapshot?.let { UserLocationTracker.gpsSignalLevel(it.accuracyMeters) } ?: 0,
            gpsAccuracyLabel =
              snapshot?.let { "±${it.accuracyMeters.toInt()} m" },
          )
        }
      }
    }
  }

  fun onLocationPermissionResult(granted: Boolean) {
    _uiState.update {
      it.copy(
        hasLocationPermission = granted,
        locationPermissionDenied = !granted,
      )
    }
    if (granted) {
      locationTracker.start()
    } else {
      locationTracker.stop()
    }
  }

  fun centerOnUser() {
    _uiState.update { it.copy(centerOnUserTick = it.centerOnUserTick + 1) }
  }

  override fun onCleared() {
    locationTracker.stop()
    super.onCleared()
  }
}
