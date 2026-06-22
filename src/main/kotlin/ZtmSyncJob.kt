package com.wardzik

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

fun startZtmSyncJob() {
    // Odpalamy nowy, niezależny wątek w tle (Dispatchers.IO jest idealny do bazy danych i sieci)
    CoroutineScope(Dispatchers.IO).launch {

        // Nieskończona pętla - serwer na Renderze nigdy jej nie przerwie
        while (isActive) {
            try {
                println("🔄 [CRON JOB] Rozpoczynam synchronizację danych z ZTM...")

                fetchAndSaveZtmData()

                println("✅ [CRON JOB] Baza zaktualizowana pomyślnie!")
            } catch (e: Exception) {
                println("❌ [CRON JOB] Błąd synchronizacji: ${e.message}")
            }

            println("⏳ [CRON JOB] Idę spać na 24 godziny...")
            // Usypiamy wątek na 24 godziny (24h * 60m * 60s * 1000ms)
            // Do testów możesz tu wpisać np. delay(60 * 1000L) żeby odpalało się co minutę!
            delay(24 * 60 * 60 * 1000L)
        }
    }
}

suspend fun fetchAndSaveZtmData() {
    // 1. TUTAJ W PRZYSZŁOŚCI UDERZYMY DO ZTM API PO ROZKŁADY
    // Na ten moment symulujemy, że parser pobrał i przetworzył listę pojazdów

    val parsedDataFromZtm = listOf(
        // Aktualizujemy dane dla 1000 (np. zmienił trasę na nowy dzień)
        mapOf(
            "number" to "1000", "loop" to "Gocław (Nowa pętla!)", "brk" to 25,
            "dep" to "Zajezdnia Ostrobramska", "time" to "23:40"
        ),
        // Dorzucamy całkowicie nowy pojazd, którego wcześniej nie było!
        mapOf(
            "number" to "3333", "loop" to "Metro Wilanowska", "brk" to 10,
            "dep" to "Zajezdnia Mokotów", "time" to "00:15"
        )
    )

    // 2. OPERACJE NA BAZIE DANYCH (SUPABASE)
    // Transaction oznacza: "Zrób to wszystko, a jak coś wybuchnie, cofnij zmiany"
    transaction {

        println("🗑️ Usuwam stare rozkłady z wczoraj...")
        VehicleRoutes.deleteAll() // Czyścimy starą tabelę!

        println("💾 Zapisuję nowe rozkłady do bazy...")
        parsedDataFromZtm.forEach { data ->
            VehicleRoutes.insert {
                it[vehicleNumber] = data["number"] as String
                it[loopName] = data["loop"] as String
                it[loopLat] = 52.22 // dla uproszczenia stałe GPS
                it[loopLon] = 21.01
                it[breakMinutes] = data["brk"] as Int
                it[depotName] = data["dep"] as String
                it[depotLat] = 52.20
                it[depotLon] = 21.05
                it[depotTime] = data["time"] as String
            }
        }
    }
}