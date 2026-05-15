package it.trentosmartmountain.app.data.remote.dto

/**
 * Response da `GET /api/v1/meteo?codice=` — sync wrapper (id documento Station aggiornato).
 * I dati climatici reali stanno nel documento Station (vedi [StationResponse]).
 */
data class MeteoSyncResponse(
    val id: String,
    val stationCode: String,
    val count: Int,
)

/**
 * Response da `GET /stations/local/:code` o `GET /stations/local/search?name=`.
 * Documento Station con metadati stazione + ultima(e) temperatura(e).
 */
data class StationResponse(
    val _id: String,
    val stationCode: String,
    val stationInfo: StationInfo?,
    val sourceUrl: String?,
    val fetchedAt: String?,
    val air_temperature: List<TemperatureReading>?,
)

data class StationInfo(
    val code: String?,
    val name: String?,
    val shortname: String?,
    val elevation: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val east: Double?,
    val north: Double?,
    val startdate: String?,
    val enddate: String?,
)

data class TemperatureReading(
    val UM: String?,
    val date: String?,
    val value: Double?,
)
