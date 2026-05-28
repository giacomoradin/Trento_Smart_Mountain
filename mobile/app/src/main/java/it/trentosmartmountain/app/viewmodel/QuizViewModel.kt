package it.trentosmartmountain.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trentosmartmountain.app.data.remote.TsmApiClient
import it.trentosmartmountain.app.data.remote.dto.BreakdownItem
import it.trentosmartmountain.app.data.remote.dto.QuizAnswerRequest
import it.trentosmartmountain.app.data.remote.dto.QuizDetailResponse
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionRequest
import it.trentosmartmountain.app.data.remote.dto.QuizSubmissionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuizState {
    object Loading : QuizState()
    data class Question(
        val quiz: QuizDetailResponse,
        val currentIndex: Int,
        val selectedChoice: Int?,
        val isAnswered: Boolean,
        val breakdown: BreakdownItem?,
    ) : QuizState()
    data class Submitting(val quiz: QuizDetailResponse) : QuizState()
    data class Result(val quiz: QuizDetailResponse, val submission: QuizSubmissionResponse) : QuizState()
    data class Error(val message: String) : QuizState()
    object AllCompleted : QuizState()
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TsmApiClient.service()

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state: StateFlow<QuizState> = _state.asStateFlow()

    // Answers collected during Q&A
    private val collectedAnswers = mutableListOf<QuizAnswerRequest>()
    // Breakdown revealed per-question after submit (filled after final submit)
    private var resultBreakdown: List<BreakdownItem> = emptyList()

    fun loadQuiz(quizId: String) {
        viewModelScope.launch {
            _state.value = QuizState.Loading
            collectedAnswers.clear()
            runCatching { api.getQuizDetail(quizId) }
                .onSuccess { resp ->
                    val quiz = resp.body()
                    if (quiz != null && resp.isSuccessful) {
                        _state.value = QuizState.Question(quiz, 0, null, false, null)
                    } else {
                        _state.value = QuizState.Error("Impossibile caricare il quiz.")
                    }
                }
                .onFailure { _state.value = QuizState.Error(it.message ?: "Errore di rete") }
        }
    }

    /** Risolve uno slug categoria → primo quiz non superato, poi delega a loadQuiz. */
    fun loadQuizFromCategory(slug: String) {
        viewModelScope.launch {
            _state.value = QuizState.Loading
            collectedAnswers.clear()
            runCatching { api.getNextQuizForCategory(slug) }
                .onSuccess { resp ->
                    val next = resp.body()
                    when {
                        !resp.isSuccessful || next == null ->
                            _state.value = QuizState.Error("Impossibile caricare la categoria.")
                        next.allCompleted || next.id.isNullOrBlank() ->
                            _state.value = QuizState.AllCompleted
                        else -> loadQuiz(next.id)
                    }
                }
                .onFailure { _state.value = QuizState.Error(it.message ?: "Errore di rete") }
        }
    }

    fun selectChoice(choiceIndex: Int) {
        val current = _state.value as? QuizState.Question ?: return
        if (current.isAnswered) return
        _state.value = current.copy(selectedChoice = choiceIndex)
    }

    fun confirmAnswer() {
        val current = _state.value as? QuizState.Question ?: return
        val choice = current.selectedChoice ?: return

        val question = current.quiz.questions[current.currentIndex]
        collectedAnswers.add(QuizAnswerRequest(question.id, choice))

        // Show answer feedback (breakdown will be filled only after full submit)
        _state.value = current.copy(isAnswered = true, breakdown = null)
    }

    fun nextQuestion() {
        val current = _state.value as? QuizState.Question ?: return
        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.quiz.questions.size) {
            _state.value = QuizState.Question(current.quiz, nextIndex, null, false, null)
        } else {
            submitQuiz(current.quiz)
        }
    }

    private fun submitQuiz(quiz: QuizDetailResponse) {
        viewModelScope.launch {
            _state.value = QuizState.Submitting(quiz)
            runCatching {
                api.submitQuiz(quiz.id, QuizSubmissionRequest(collectedAnswers.toList()))
            }.onSuccess { resp ->
                val result = resp.body()
                if (result != null && resp.isSuccessful) {
                    resultBreakdown = result.breakdown
                    _state.value = QuizState.Result(quiz, result)
                } else {
                    _state.value = QuizState.Error("Errore invio quiz.")
                }
            }.onFailure {
                _state.value = QuizState.Error(it.message ?: "Errore di rete")
            }
        }
    }
}
