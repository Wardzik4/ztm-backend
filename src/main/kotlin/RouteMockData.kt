package com.wardzik

import kotlinx.serialization.Serializable

@Serializable
data class RouteMockData(
    val loopName: String,
    val loopLat: Double,
    val loopLon: Double,
    val breakMinutes: Int,
    val depotName: String,
    val depotLat: Double,
    val depotLon: Double,
    val depotTime: String
)