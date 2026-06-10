package it.trentosmartmountain.app.data.remote.dto

/** Body opzionale per POST/PUT `/api/v1/sessions/:id/checklist`. */
data class ChecklistGenerateRequest(
    /** Se omesso, il backend usa `sentieroCode` persistito sulla sessione. */
    val sentieroCode: String? = null,
    val locationId: String? = null,
    val partenza: String? = null,
)

/** Risposta GET `/api/v1/sessions/:id/checklist`. */
data class ChecklistGetResponse(
    val checklist: ChecklistDto,
    val freeze: ChecklistFreezeInfo,
)

/** Risposta POST/PUT checklist. */
data class ChecklistMutationResponse(
    val message: String? = null,
    val checklist: ChecklistDto,
    val meteoDisponibile: Boolean? = null,
)

data class ChecklistDto(
    val generatedAt: String? = null,
    val updatedAt: String? = null,
    val isFrozen: Boolean = false,
    val frozenAt: String? = null,
    val meteoSnapshot: ChecklistMeteoSnapshot? = null,
    val categorie: List<ChecklistCategoriaDto> = emptyList(),
    val acquaLitri: Double? = null,
    val calorieFabbisogno: Int? = null,
)

data class ChecklistCategoriaDto(
    val nome: String,
    val livello: String,
    val items: List<ChecklistItemDto> = emptyList(),
)

data class ChecklistItemDto(
    val nome: String,
    val motivo: String? = null,
)

data class ChecklistMeteoSnapshot(
    val locationId: String? = null,
    val locationName: String? = null,
    val forecastFetchedAt: String? = null,
    val temperaturaMinPrevista: Double? = null,
    val temperaturaMedPrevista: Double? = null,
    val pioggiaProbMax: Double? = null,
    val ventoMaxPrevisto: Double? = null,
)

data class ChecklistFreezeInfo(
    val isFrozen: Boolean = false,
    val frozenAt: String? = null,
)
