package com.wardzik

import io.ktor.server.engine.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database // <-- Upewnij się, że masz ten import!

fun main(args: Array<String>) {

    // --- ŁĄCZYMY SIĘ Z BAZĄ DANYCH SUPABASE ---
    val dbPassword = System.getenv("DB_PASSWORD") ?: "HKASrT9Ig5TsyJ73"

    Database.connect(
        url = "jdbc:postgresql://db.kcqncvrccpfsqcqowdak.supabase.co:5432/postgres?sslmode=require",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = dbPassword // Używamy zmiennej!
    )
    println("✅ Podłączono do Supabase!")

    // Odpalamy Ktor-a
    io.ktor.server.netty.EngineMain.main(args)
}