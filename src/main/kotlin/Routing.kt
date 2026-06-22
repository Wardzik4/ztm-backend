package com.wardzik

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database // <-- PAMIĘTAJ O TYM IMPORCIE
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
@Serializable
data class AdminUnknownTramsResponse(
    val totalMissing: Int,
    val missingVehicles: List<String>
)

fun Application.configureRouting() {

    val dbPassword = System.getenv("DB_PASSWORD") ?: "HKASrT9Ig5TsyJ73"

    Database.connect(
        // Zwróć uwagę na host: aws-1...pooler oraz port: 5432
        url = "jdbc:postgresql://aws-1-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require",

        driver = "org.postgresql.Driver",

        // Wyciągnięty nowy użytkownik z Twojego linku:
        user = "postgres.kcqncvrccpfsqcqowdak",

        password = dbPassword
    )
    println("✅ Podłączono do Supabase!")
    // ---------------------------------

    routing {
        get("/") { call.respondText("Serwer ZTM działa!") }


        get("/api/search/{number}") {
            val number = call.parameters["number"] ?: return@get call.respond(emptyList<UnifiedVehicleResponse>())
            val results = mutableListOf<UnifiedVehicleResponse>()

            // Funkcja pomocnicza budująca paczkę z RAMu i Bazy
            fun buildResponse(isTram: Boolean, prefix: String) {
                // 1. Pytamy RAM o pozycję GPS w ułamek sekundy
                val gpsData = liveGpsCache["$prefix$number"]

                if (gpsData != null) {
                    // 2. Jeśli jest w RAM, dociągamy zajezdnię z Supabase
                    val dbData = transaction {
                        VehicleRoutes.select { VehicleRoutes.vehicleNumber eq "$prefix$number" }.singleOrNull()
                    }

                    // 3. Łączymy w jeden obiekt
                    results.add(
                        UnifiedVehicleResponse(
                            vehicleNumber = gpsData.vehicleNumber,
                            isTram = isTram,
                            lines = gpsData.lines,
                            lat = gpsData.lat,
                            lon = gpsData.lon,
                            time = gpsData.time,
                            loopName = dbData?.get(VehicleRoutes.loopName) ?: "Brak w bazie",
                            loopLat = dbData?.get(VehicleRoutes.loopLat) ?: 0.0,
                            loopLon = dbData?.get(VehicleRoutes.loopLon) ?: 0.0,
                            breakMinutes = dbData?.get(VehicleRoutes.breakMinutes) ?: 0,
                            depotName = dbData?.get(VehicleRoutes.depotName) ?: "Brak",
                            depotLat = dbData?.get(VehicleRoutes.depotLat) ?: 0.0,
                            depotLon = dbData?.get(VehicleRoutes.depotLon) ?: 0.0,
                            depotTime = dbData?.get(VehicleRoutes.depotTime) ?: "--:--"
                        )
                    )
                }
            }

            buildResponse(isTram = false, prefix = "B-")
            buildResponse(isTram = true, prefix = "T-")

            // Zwracamy gotową Listę do telefonu!
            call.respond(results)
        }
    }
    startBackgroundJobs()
}