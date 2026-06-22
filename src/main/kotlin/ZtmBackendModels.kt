package com.wardzik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZtmLiveLocationResponse(
    @SerialName("result") val result: List<VehicleDto>
)

@Serializable
data class VehicleDto(
    @SerialName("Lines") val lines: String = "Brak",
    @SerialName("Lon") val lon: Double = 0.0,
    @SerialName("Lat") val lat: Double = 0.0,
    @SerialName("VehicleNumber") val vehicleNumber: String = "Brak",
    @SerialName("Time") val time: String = "Brak",
    @SerialName("Brigade") val brigade: String = "Brak"
)
// NOWOŚĆ: Klasa, która łączy GPS i Zajezdnię w jedno!
@Serializable
data class UnifiedVehicleResponse(
    val vehicleNumber: String,
    val isTram: Boolean,
    val lines: String,
    val lat: Double,
    val lon: Double,
    val time: String,
    val loopName: String,
    val loopLat: Double,
    val loopLon: Double,
    val breakMinutes: Int,
    val depotName: String,
    val depotLat: Double,
    val depotLon: Double,
    val depotTime: String
)