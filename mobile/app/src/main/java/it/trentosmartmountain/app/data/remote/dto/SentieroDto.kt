package it.trentosmartmountain.app.data.remote.dto

/**
 * DTO per gli endpoint sentieri SAT (`/api/v1/sentieri/...`), modalità "Scegli percorso sulla mappa".
 *
 * Gli endpoint backend rispondono con un wrapper `{ message, count, data, ... }`: i campi reali
 * sono dentro `data`, quindi i [ApiListResponse]/[ApiItemResponse] generici incapsulano la forma.
 */

/** Wrapper backend per risposte con lista (`{ message, count, data: [...] }`). */
data class ApiListResponse<T>(
    val data: List<T> = emptyList(),
    val message: String? = null,
    val count: Int? = null,
)

/** Wrapper backend per risposte con singolo oggetto (`{ message, data: {...} }`). */
data class ApiItemResponse<T>(
    val data: T?,
    val message: String? = null,
)

/** Coordinata geografica (lat/lon) come restituita dal backend sentieri. */
data class SentieroCoordinate(
    val lat: Double,
    val lon: Double,
)

/** Punto di inizio/fine sentiero (nome, quota in metri, coordinata). */
data class SentieroPunto(
    val nome: String? = null,
    val quota: Int? = null,
    val coordinate: SentieroCoordinate? = null,
)

/**
 * Destinazione (punto finale) raggiunta da uno o più sentieri.
 * Risposta da `GET /api/v1/sentieri/destinazioni`.
 */
data class SentieroDestinazioneDto(
    val nome: String,
    val quota: Int? = null,
    val numeroSentieri: Int = 0,
    val coordinate: SentieroCoordinate? = null,
)

/**
 * Sentiero in forma "lista" (senza `percorsoCoordinate`, escluso dal backend per performance).
 * Risposta da `GET /api/v1/sentieri/destinazioni/:nome/sentieri`.
 */
data class SentieroListItemDto(
    val codice: String,
    val denominazione: String? = null,
    val puntoInizio: SentieroPunto? = null,
    val puntoFine: SentieroPunto? = null,
    val difficolta: String? = null,
    val quotaMinima: Int? = null,
    val quotaMassima: Int? = null,
    val lunghezzaPlanimetrica: Int? = null,
    val lunghezzaInclinata: Int? = null,
    val tempoAndata: String? = null,
    val tempoRitorno: String? = null,
)

/**
 * Sentiero completo, incluso `percorsoCoordinate` (stringa "lon,lat lon,lat ...").
 * Risposta da `GET /api/v1/sentieri/:codice`.
 */
data class SentieroDettaglioDto(
    val codice: String,
    val denominazione: String? = null,
    val puntoInizio: SentieroPunto? = null,
    val puntoFine: SentieroPunto? = null,
    val difficolta: String? = null,
    val quotaMinima: Int? = null,
    val quotaMassima: Int? = null,
    val lunghezzaPlanimetrica: Int? = null,
    val lunghezzaInclinata: Int? = null,
    val tempoAndata: String? = null,
    val tempoRitorno: String? = null,
    val percorsoCoordinate: String? = null,
)
