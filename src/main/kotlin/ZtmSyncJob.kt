package com.wardzik

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

// Klient HTTP dla serwera
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

fun startZtmSyncJob() {
    val ztmApiKey = System.getenv("ZTM_API_KEY") ?: "f901d0f5-52ec-4ee4-9bda-4adba28752db"

    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            try {
                println("🔄 [CRON JOB] Pobieram PRAWIDZIWE dane z ZTM Warszawa...")

                // 1. Pobieramy autobusy
                val busesResponse: ZtmLiveLocationResponse = httpClient.get("https://api.um.warszawa.pl/api/action/busestrams_get/") {
                    parameter("resource_id", "f2e5503e-927d-4ad3-9500-4ab9e55deb59")
                    parameter("apikey", ztmApiKey)
                    parameter("type", 1)
                }.body()

                // 2. Pobieramy tramwaje
                val tramsResponse: ZtmLiveLocationResponse = httpClient.get("https://api.um.warszawa.pl/api/action/busestrams_get/") {
                    parameter("resource_id", "f2e5503e-927d-4ad3-9500-4ab9e55deb59")
                    parameter("apikey", ztmApiKey)
                    parameter("type", 2)
                }.body()

                // Filtrujemy śmieci (np. puste numery) i usuwamy DUPLIKATY z ZTM!
                val allBuses = busesResponse.result
                    .filter { it.vehicleNumber.isNotBlank() && it.vehicleNumber != "Brak" }
                    .distinctBy { it.vehicleNumber }

                val allTrams = tramsResponse.result
                    .filter { it.vehicleNumber.isNotBlank() && it.vehicleNumber != "Brak" }
                    .distinctBy { it.vehicleNumber }

                println("📡 Pobrane z GPS: Autobusów: ${allBuses.size}, Tramwajów: ${allTrams.size}")

                // 3. Masowy zapis do Supabase!
                transaction {
                    println("🗑️ Czyszczę starą bazę Supabase...")
                    VehicleRoutes.deleteAll()

                    println("💾 Wrzucam autobusy do bazy...")
                    VehicleRoutes.batchInsert(allBuses) { bus ->
                        // Używamy .take(10) by uciąć ew. śmieci z ZTM powyżej 10 znaków
                        this[VehicleRoutes.vehicleNumber] = ("B-" + bus.vehicleNumber).take(10)
                        this[VehicleRoutes.loopName] = calculateExpectedEnd(bus.lines)
                        this[VehicleRoutes.loopLat] = bus.lat
                        this[VehicleRoutes.loopLon] = bus.lon
                        this[VehicleRoutes.breakMinutes] = (5..15).random()
                        this[VehicleRoutes.depotName] = calculateDepot(bus.vehicleNumber, isTram = false)
                        this[VehicleRoutes.depotLat] = 0.0
                        this[VehicleRoutes.depotLon] = 0.0
                        // ZMIANA: Skracamy tekst, by zmieścił się w 10 znakach!
                        this[VehicleRoutes.depotTime] = "Zjazd"
                    }

                    println("💾 Wrzucam tramwaje do bazy...")
                    VehicleRoutes.batchInsert(allTrams) { tram ->
                        this[VehicleRoutes.vehicleNumber] = ("T-" + tram.vehicleNumber).take(10)
                        this[VehicleRoutes.loopName] = calculateExpectedEnd(tram.lines)
                        this[VehicleRoutes.loopLat] = tram.lat
                        this[VehicleRoutes.loopLon] = tram.lon
                        this[VehicleRoutes.breakMinutes] = (5..20).random()
                        this[VehicleRoutes.depotName] = calculateDepot(tram.vehicleNumber, isTram = true)
                        this[VehicleRoutes.depotLat] = 0.0
                        this[VehicleRoutes.depotLon] = 0.0
                        // ZMIANA: Tutaj też
                        this[VehicleRoutes.depotTime] = "Zjazd"
                    }
                }

                println("✅ [CRON JOB] Baza zaktualizowana PRAWIDZIWYMI danymi!")

            } catch (e: Exception) {
                println("❌ [CRON JOB] Błąd pobierania ZTM: ${e.message}")
            }

            // Odświeżamy dane np. co 10 minut (10 * 60 * 1000)
            println("⏳ [CRON JOB] Czekam 10 minut na kolejne pobranie GPS...")
            delay(10 * 60 * 1000L)
        }
    }
}