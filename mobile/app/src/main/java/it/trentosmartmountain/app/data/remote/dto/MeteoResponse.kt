package it.trentosmartmountain.app.data.remote.dto

/**
 * DTO per le API meteo del backend (proxy verso MeteoTrentino/TINIA).
 *
 * - [WeatherLocationsResponse]: risultati di ricerca o location vicine a coordinate GPS
 * - [WeatherForecastResponse]: previsioni 3h e 24h per una location
 */

/** Risposta di `GET /weather/locations/nearby` e `/weather/locations/search`. */
data class WeatherLocationsResponse(
    val count: Int,
    val results: List<WeatherLocationResult>,
)

data class WeatherLocationResult(
    val externalId: String,
    val type: String,         // "town" | "poi"
    val name: String,
    val elevation: Int?,
    val location: WeatherGeoPoint?,
    val regionId: String?,
)

data class WeatherGeoPoint(
    val type: String,
    /** Coordinate GeoJSON: `[longitudine, latitudine]`. */
    val coordinates: List<Double>,
)

/** Risposta di `GET /weather/forecast/{externalId}` (slot 3h + 24h, eventuale cache server). */
data class WeatherForecastResponse(
    val location: WeatherLocationResult,
    val referenceTown: WeatherLocationReference?,
    val meta: WeatherForecastMeta?,
    val forecast3h: List<WeatherForecastSlot>,
    val forecast24h: List<WeatherForecastSlot>,
    val fromCache: Boolean?,
)

data class WeatherLocationReference(
    val externalId: String,
    val name: String,
)

data class WeatherForecastMeta(
    val fetchedAt: String?,
    val validFrom: String?,
    val validTo: String?,
    val fromCache: Boolean?,
)

/**
 * Singolo slot di previsione (3h o 24h).
 *
 * Campo `skyCondition`: codice stringa dal formato MeteoTrentino/TINIA.
 * Mapping consigliato per la UI → [skyConditionEmoji].
 *
 * Campi 3h: `temperature`
 * Campi 24h: `temperatureMin`, `temperatureMax`
 */
data class WeatherForecastSlot(
    val timeLayoutKey: String?,
    val intervalMinutes: Int?,
    val validFrom: String?,
    val validTo: String?,
    val temperature: Double?,
    val temperatureMin: Double?,
    val temperatureMax: Double?,
    val rainFall: Double?,
    val rainProbability: Double?,
    val freshSnow: Double?,
    val snowLevel: Double?,
    val windSpeed: Double?,
    val windGust: Double?,
    val windDirection: Double?,
    val freezingLevel: Double?,
    val skyCondition: String?,      // codice iconografico TINIA
    val sunshineDuration: Double?,
)
