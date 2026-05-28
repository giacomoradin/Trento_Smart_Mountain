package it.trentosmartmountain.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class QuizCategoryDto(
    @SerializedName("_id") val id: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String,
    @SerializedName("iconName") val iconName: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class QuizCategoryProgressResponse(
    @SerializedName("category") val category: QuizCategoryDto,
    @SerializedName("totalQuizzes") val totalQuizzes: Int,
    @SerializedName("passedByMe") val passedByMe: Int,
    @SerializedName("totalCredits") val totalCredits: Int,
    @SerializedName("earnedByMe") val earnedByMe: Int,
    @SerializedName("progressPct") val progressPct: Double,
)

data class QuizListItemResponse(
    @SerializedName("quiz") val quiz: QuizSummary,
    @SerializedName("passedByMe") val passedByMe: Boolean,
    @SerializedName("completedAt") val completedAt: String?,
)

data class QuizSummary(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("creditsReward") val creditsReward: Int,
)

data class QuizQuestionDto(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("choices") val choices: List<String>,
)

data class QuizDetailResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val category: QuizCategoryDto,
    @SerializedName("questions") val questions: List<QuizQuestionDto>,
    @SerializedName("creditsReward") val creditsReward: Int,
    /** true se l'utente ha già superato questo quiz in passato → niente crediti aggiuntivi al submit. */
    @SerializedName("alreadyPassed") val alreadyPassed: Boolean = false,
)

data class QuizAnswerRequest(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("choiceIndex") val choiceIndex: Int,
)

data class QuizSubmissionRequest(
    @SerializedName("answers") val answers: List<QuizAnswerRequest>,
)

data class BreakdownItem(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("choiceIndex") val choiceIndex: Int,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("correctIndex") val correctIndex: Int,
    @SerializedName("explanation") val explanation: String,
)

data class QuizSubmissionResponse(
    @SerializedName("score") val score: Double,
    @SerializedName("correctCount") val correctCount: Int,
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("passed") val passed: Boolean,
    @SerializedName("creditsAwarded") val creditsAwarded: Int,
    @SerializedName("breakdown") val breakdown: List<BreakdownItem>,
    @SerializedName("newTotalCredits") val newTotalCredits: Int?,
)

/** Risposta a GET /quiz/categories/:slug/next — usato dalla FormazioneScreen
 *  per saltare direttamente al prossimo quiz aperto della categoria. */
data class NextQuizResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("totalQuestions") val totalQuestions: Int?,
    @SerializedName("creditsReward") val creditsReward: Int?,
    @SerializedName("allCompleted") val allCompleted: Boolean,
)
