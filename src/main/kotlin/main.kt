package com.wardzik

import io.ktor.server.engine.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database // <-- Upewnij się, że masz ten import!

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}