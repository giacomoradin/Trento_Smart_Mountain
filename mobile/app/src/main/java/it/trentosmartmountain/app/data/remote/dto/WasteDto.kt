package it.trentosmartmountain.app.data.remote.dto

/**
 * DTO del modulo Rifiuti & Logistica del rifugio (ADR-002, MVP read-only).
 * Specchiano `backend/src/services/wasteService.js`.
 */

data class WasteSimulationRequest(
  val periodDays: Int,
  val beds: Int,
  val bedOccupancy: Double,
  val dayVisitors: Int,
  val wastePerGuestKg: Double,
  val wastePerVisitorKg: Double,
  val screeningPerGuestKg: Double,
  val compactorEnabled: Boolean = false,
)

data class WasteSimulationResponse(
  val input: WasteInputEcho?,
  val totals: WasteTotalsDto?,
  val breakdown: List<WasteBreakdownDto> = emptyList(),
  val compliance: WasteComplianceDto?,
  val vectors: List<WasteVectorDto> = emptyList(),
  val cheapestVector: String?,
)

data class WasteInputEcho(
  val periodDays: Int,
  val overnightGuests: Double,
)

data class WasteTotalsDto(
  val preMassKg: Double,
  val postMassKg: Double,
  val preVolumeM3: Double,
  val postVolumeM3: Double,
  val massReductionPct: Double,
  val volumeReductionPct: Double,
)

data class WasteBreakdownDto(
  val name: String,
  val preMassKg: Double,
  val preVolumeM3: Double,
  val postMassKg: Double,
  val postVolumeM3: Double,
  val storageCapacityM3: Double,
)

data class WasteComplianceDto(
  val alerts: List<WasteAlertDto> = emptyList(),
  val criticalDay: Int?,
  val criticalCause: String?,
)

data class WasteAlertDto(
  val type: String,
  val category: String?,
  val day: Int?,
  val message: String,
)

data class WasteVectorDto(
  val name: String,
  val effectivePayloadKg: Double?,
  val trips: Int,
  val totalCostEur: Double,
  val costPerKgEur: Double,
  val avgSaturationPct: Double,
)
