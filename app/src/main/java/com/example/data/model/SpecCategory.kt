package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class SpecCategory(
    val title: String,
    val color: Color
) {
    ALL("All", Color(0xFF38BDF8)),
    DISPLAY("Display", Color(0xFF3B82F6)),
    PERFORMANCE("Performance", Color(0xFFEF4444)),
    CAMERA("Camera", Color(0xFF10B981)),
    BATTERY("Battery", Color(0xFFF59E0B)),
    SOFTWARE("Software", Color(0xFF8B5CF6)),
    CONNECTIVITY("Connectivity", Color(0xFF06B6D4)),
    SECURITY("Security", Color(0xFF6366F1)),
    DESIGN("Design", Color(0xFFEC4899)),
    OTHER("Other", Color(0xFF64748B));

    companion object {
        fun fromString(name: String?): SpecCategory {
            if (name == null) return OTHER
            val normalized = name.trim().uppercase()
            return entries.firstOrNull { it.name == normalized || it.title.uppercase() == normalized }
                ?: OTHER
        }

        fun guessCategory(fieldName: String): SpecCategory {
            val lower = fieldName.lowercase()
            return when {
                lower.contains("display") || lower.contains("screen") || lower.contains("resolution") ||
                        lower.contains("refresh") || lower.contains("nits") || lower.contains("brightness") ||
                        lower.contains("amoled") || lower.contains("oled") || lower.contains("ppi") -> DISPLAY

                lower.contains("chipset") || lower.contains("cpu") || lower.contains("gpu") ||
                        lower.contains("ram") || lower.contains("storage") || lower.contains("memory") ||
                        lower.contains("processor") || lower.contains("benchmark") || lower.contains("sd card") -> PERFORMANCE

                lower.contains("camera") || lower.contains("sensor") || lower.contains("lens") ||
                        lower.contains("telephoto") || lower.contains("ultrawide") || lower.contains("wide") ||
                        lower.contains("video") || lower.contains("megapixel") || lower.contains("selfie") ||
                        lower.contains("optical") -> CAMERA

                lower.contains("battery") || lower.contains("charging") || lower.contains("watt") ||
                        lower.contains("mah") || lower.contains("wireless charge") || lower.contains("power") -> BATTERY

                lower.contains("os") || lower.contains("android") || lower.contains("ios") ||
                        lower.contains("software") || lower.contains("firmware") || lower.contains("ui") -> SOFTWARE

                lower.contains("5g") || lower.contains("wi-fi") || lower.contains("wifi") ||
                        lower.contains("bluetooth") || lower.contains("nfc") || lower.contains("gps") ||
                        lower.contains("network") || lower.contains("sim") || lower.contains("cellular") -> CONNECTIVITY

                lower.contains("fingerprint") || lower.contains("biometric") || lower.contains("face id") ||
                        lower.contains("security") || lower.contains("unlock") || lower.contains("knox") -> SECURITY

                lower.contains("dimension") || lower.contains("weight") || lower.contains("build") ||
                        lower.contains("ip rating") || lower.contains("color") || lower.contains("material") ||
                        lower.contains("water") || lower.contains("dust") || lower.contains("glass") -> DESIGN

                else -> OTHER
            }
        }
    }
}
