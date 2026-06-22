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