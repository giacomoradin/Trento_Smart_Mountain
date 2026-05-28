package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.TsmApplication
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.QuizCategoryProgressResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormazioneUiState(
    val isLoading: Boolean = true,
    val categories: List<QuizCategoryProgressResponse> = emptyList(),
    val error: String? = null,
)

class FormazioneViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _uiState = MutableStateFlow(FormazioneUiState())
    val uiState: StateFlow<FormazioneUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = FormazioneUiState(isLoading = true)
            runCatching { api.getQuizCategories() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = FormazioneUiState(isLoading = false, categories = resp.body() ?: emptyList())
                    } else {
                        _uiState.value = FormazioneUiState(isLoading = false, error = "Errore ${resp.code()}")
                    }
                }
                .onFailure { _uiState.value = FormazioneUiState(isLoading = false, error = it.message) }
        }
    }
}
