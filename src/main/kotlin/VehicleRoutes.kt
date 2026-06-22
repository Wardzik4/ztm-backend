package com.wardzik

import org.jetbrains.exposed.sql.Table

// To jest reprezentacja naszej tabeli z Supabase w języku Kotlin
object VehicleRoutes : Table("vehicle_routes") {
    val vehicleNumber = varchar("vehicle_number", 10)
    val loopName = varchar("loop_name", 100)
    val loopLat = double("loop_lat")
    val loopLon = double("loop_lon")
    val breakMinutes = integer("break_minutes")
    val depotName = varchar("depot_name", 100)
    val depotLat = double("depot_lat")
    val depotLon = double("depot_lon")
    val depotTime = varchar("depot_time", 10)
}