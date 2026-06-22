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


        // --- HYBRYDOWY ENDPOINT ---
        // Telefon wysyła nam wszystko co wie, a my to wzbogacamy o Zajezdnię!
        get("/api/enrich") {
            // Odbieramy dane przysłane z telefonu z zapytania URL
            val number = call.request.queryParameters["number"] ?: return@get call.respond(emptyList<UnifiedVehicleResponse>())
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: 0.0
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: 0.0
            val isTram = call.request.queryParameters["isTram"]?.toBoolean() ?: false
            val lines = call.request.queryParameters["lines"] ?: "Brak"
            val time = call.request.queryParameters["time"] ?: "Brak"

            // Dodajemy przedrostek żeby sprawdzić, czy był w Supabase (jako historia)
            val prefix = if (isTram) "T-" else "B-"

            // Szukamy go w Supabase (choć w sumie zajezdnię i tak wyliczymy w locie!)
            val dbData = transaction {
                VehicleRoutes.select { VehicleRoutes.vehicleNumber eq "$prefix$number" }.singleOrNull()
            }

            // A oto nasza potężna logika domenowa (Kalkulator z Excela)
            val calculatedDepot = calculateDepot(number, isTram)
            val calculatedLoop = calculateExpectedEnd(lines)

            // Łączymy GPS z Telefonu + Słowniki z Serwera!
            val response = UnifiedVehicleResponse(
                vehicleNumber = number,
                isTram = isTram,
                lines = lines,
                lat = lat,
                lon = lon,
                time = time,
                loopName = calculatedLoop,
                loopLat = dbData?.get(VehicleRoutes.loopLat) ?: 0.0,
                loopLon = dbData?.get(VehicleRoutes.loopLon) ?: 0.0,
                breakMinutes = (5..20).random(), // Zmyślona przerwa w locie
                depotName = calculatedDepot, // <--- MAGIA Z EXCELA!
                depotLat = dbData?.get(VehicleRoutes.depotLat) ?: 0.0,
                depotLon = dbData?.get(VehicleRoutes.depotLon) ?: 0.0,
                depotTime = "Zjazd"
            )

            call.respond(listOf(response))
        }
    }
}