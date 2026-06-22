package com.wardzik

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.* // <-- DODANY IMPORT

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json() // <-- WŁĄCZAMY SILNIK JSON!
    }
}