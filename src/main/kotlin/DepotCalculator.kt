package com.wardzik

fun calculateDepot(vehicleNumber: String, isTram: Boolean): String {
    if (isTram) {
        // Logika dla tramwajów (bardzo uproszczona - do rozbudowy przez Ciebie!)
        return when {
            vehicleNumber.startsWith("3") -> "Zajezdnia R-3 Mokotów"
            vehicleNumber.startsWith("1") -> "Zajezdnia R-1 Wola"
            vehicleNumber.startsWith("2") -> "Zajezdnia R-2 Praga"
            else -> "Zajezdnia Tramwajowa (Inna)"
        }
    } else {
        // Logika dla autobusów
        return when {
            vehicleNumber.startsWith("1") || vehicleNumber.startsWith("2") -> "Zajezdnia R-10 Ostrobramska"
            vehicleNumber.startsWith("3") || vehicleNumber.startsWith("4") -> "Zajezdnia R-13 Kleszczowa"
            vehicleNumber.startsWith("5") || vehicleNumber.startsWith("7") -> "Zajezdnia R-7 Woronicza"
            vehicleNumber.startsWith("9") -> "Mobilis / Arriva (Prywatny)"
            else -> "Zajezdnia MZA (Niezidentyfikowana)"
        }
    }
}

// Funkcja "udająca" zgadywanie pętli na podstawie kierunku
fun calculateExpectedEnd(lines: String): String {
    return "Trasa linii $lines (Wymaga GTFS)"
}