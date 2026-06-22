package com.wardzik

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRouting() {
    routing {
        get("/") { call.respondText("Serwer ZTM działa!") }

        // --- NASZ NOWY ADRES Z PARAMETREM ---
        // {number} oznacza, że spodziewamy się tu numeru pojazdu
        get("/api/route/{number}") {

            // 1. Odbieramy numer z linku (np. z /api/route/1000)
            val number = call.parameters["number"] ?: return@get call.respondText("Brak numeru!")

            // 2. Otwieramy połączenie z bazą Supabase
            val dbResult = transaction {
                // Tłumaczenie z SQL: SELECT * FROM vehicle_routes WHERE vehicle_number = '1000' LIMIT 1
                VehicleRoutes.select { VehicleRoutes.vehicleNumber eq number }.singleOrNull()
            }

            // 3. Sprawdzamy czy znaleźliśmy go w bazie
            if (dbResult != null) {
                // Znaleziono! Przekładamy dane z bazy do naszego modelu JSON
                val responseData = RouteMockData(
                    loopName = dbResult[VehicleRoutes.loopName],
                    loopLat = dbResult[VehicleRoutes.loopLat],
                    loopLon = dbResult[VehicleRoutes.loopLon],
                    breakMinutes = dbResult[VehicleRoutes.breakMinutes],
                    depotName = dbResult[VehicleRoutes.depotName],
                    depotLat = dbResult[VehicleRoutes.depotLat],
                    depotLon = dbResult[VehicleRoutes.depotLon],
                    depotTime = dbResult[VehicleRoutes.depotTime]
                )
                call.respond(responseData)
            } else {
                // Pojazdu nie ma w naszej bazie - zwracamy "pusty" wynik
                // (Robimy tak by mapa w Androidzie się nie wysypała z braku danych)
                val defaultData = RouteMockData(
                    loopName = "Brak danych w systemie",
                    loopLat = 0.0,
                    loopLon = 0.0,
                    breakMinutes = 0,
                    depotName = "Brak",
                    depotLat = 0.0,
                    depotLon = 0.0,
                    depotTime = "--:--"
                )
                call.respond(defaultData)
            }
        }
    }
}