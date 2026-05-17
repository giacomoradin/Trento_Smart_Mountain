package it.trentosmartmountain.app.data.remote.dto

/** Body per `POST /auth/forgot-password` (invio link reset via email). */
data class ForgotPasswordRequest(val email: String)
