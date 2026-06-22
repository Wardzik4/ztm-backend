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
import java.util.concurrent.ConcurrentHashMap // <-- WAŻNY IMPORT!

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// 🔥 PAMIĘĆ RAM SERWERA (Błyskawiczny Cache)
val liveGpsCache = ConcurrentHashMap<String, VehicleDto>()

fun startBackgroundJobs() {
    val ztmApiKey = System.getenv("ZTM_API_KEY") ?: ""

    // 1. SZYBKI PRACOWNIK: Pobiera GPS co 15 sekund i trzyma go w pamięci RAM!
    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            try {
                val buses: ZtmLiveLocationResponse = httpClient.get("https://api.um.warszawa.pl/api/action/busestrams_get/") {
                    parameter("resource_id", "f2e5503e-927d-4ad3-9500-4ab9e55deb59")
                    parameter("apikey", ztmApiKey)
                    parameter("type", 1)
                }.body()

                val trams: ZtmLiveLocationResponse = httpClient.get("https://api.um.warszawa.pl/api/action/busestrams_get/") {
                    parameter("resource_id", "f2e5503e-927d-4ad3-9500-4ab9e55deb59")
                    parameter("apikey", ztmApiKey)
                    parameter("type", 2)
                }.body()

                // Bezpiecznie zapisujemy najnowszy GPS do Pamięci RAM
                buses.result.forEach { if(it.vehicleNumber.isNotBlank()) liveGpsCache["B-${it.vehicleNumber.take(10)}"] = it }
                trams.result.forEach { if(it.vehicleNumber.isNotBlank()) liveGpsCache["T-${it.vehicleNumber.take(10)}"] = it }

            } catch (e: Exception) { println("❌ Błąd szybkiego GPS: ${e.message}") }
            delay(15 * 1000L) // Czekamy 15 sekund!
        }
    }

    // 2. WOLNY PRACOWNIK: Co 10 minut bierze to co jest w RAMie i zapisuje do Supabase
    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            try {
                delay(5000L) // Dajemy szybkiemu 5 sekund na pierwsze pobranie przy starcie

                // Wyciągamy z RAM-u unikalne pojazdy
                val allBuses = liveGpsCache.filterKeys { it.startsWith("B-") }.values.distinctBy { it.vehicleNumber }
                val allTrams = liveGpsCache.filterKeys { it.startsWith("T-") }.values.distinctBy { it.vehicleNumber }

                transaction {
                    VehicleRoutes.deleteAll()

                    VehicleRoutes.batchInsert(allBuses) { bus ->
                        this[VehicleRoutes.vehicleNumber] = "B-${bus.vehicleNumber.take(10)}"
                        this[VehicleRoutes.loopName] = calculateExpectedEnd(bus.lines)
                        this[VehicleRoutes.loopLat] = bus.lat
                        this[VehicleRoutes.loopLon] = bus.lon
                        this[VehicleRoutes.breakMinutes] = (5..15).random()
                        this[VehicleRoutes.depotName] = calculateDepot(bus.vehicleNumber, isTram = false)
                        this[VehicleRoutes.depotLat] = 0.0
                        this[VehicleRoutes.depotLon] = 0.0
                        this[VehicleRoutes.depotTime] = "Zjazd"
                    }

                    VehicleRoutes.batchInsert(allTrams) { tram ->
                        this[VehicleRoutes.vehicleNumber] = "T-${tram.vehicleNumber.take(10)}"
                        this[VehicleRoutes.loopName] = calculateExpectedEnd(tram.lines)
                        this[VehicleRoutes.loopLat] = tram.lat
                        this[VehicleRoutes.loopLon] = tram.lon
                        this[VehicleRoutes.breakMinutes] = (5..20).random()
                        this[VehicleRoutes.depotName] = calculateDepot(tram.vehicleNumber, isTram = true)
                        this[VehicleRoutes.depotLat] = 0.0
                        this[VehicleRoutes.depotLon] = 0.0
                        this[VehicleRoutes.depotTime] = "Zjazd"
                    }
                }
                println("✅ Baza Supabase zaktualizowana!")
            } catch(e: Exception) { println("❌ Błąd bazy Supabase: ${e.message}") }

            delay(10 * 60 * 1000L) // 10 minut snu
        }
    }
}