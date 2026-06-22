package com.wardzik

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database // <-- PAMIĘTAJ O TYM IMPORCIE
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

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

        get("/api/route/{number}") {
            val number = call.parameters["number"] ?: return@get call.respondText("Brak numeru!")

            val dbResult = transaction {
                VehicleRoutes.select { VehicleRoutes.vehicleNumber eq number }.singleOrNull()
            }

            if (dbResult != null) {
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
                val defaultData = RouteMockData(
                    loopName = "Brak danych w systemie", loopLat = 0.0, loopLon = 0.0,
                    breakMinutes = 0, depotName = "Brak", depotLat = 0.0, depotLon = 0.0, depotTime = "--:--"
                )
                call.respond(defaultData)
            }
        }
    }
}