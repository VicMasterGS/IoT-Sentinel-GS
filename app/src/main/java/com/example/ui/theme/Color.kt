package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Palette (IoT Sentinel Graphite & Deep Navy with Cyan & Emerald accents)
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF0F172A)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOutline = Color(0xFF334155)

val CyanAccent = Color(0xFF22D3EE)
val CyanPrimary = Color(0xFF06B6D4)
val EmeraldAccent = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)

val AmberWarning = Color(0xFFF59E0B)
val RedAlert = Color(0xFFEF4444)
val BlueInfo = Color(0xFF3B82F6)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextTertiaryDark = Color(0xFF64748B)

// Light Palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOutline = Color(0xFFCBD5E1)
val CyanPrimaryLight = Color(0xFF0891B2)
val EmeraldPrimaryLight = Color(0xFF059669)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// 4 Accent Colors for Theme Customization
enum class AccentColor(
    val id: String,
    val displayName: String,
    val previewColor: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val primaryContainerDark: Color,
    val primaryContainerLight: Color,
    val onPrimaryDark: Color,
    val onPrimaryLight: Color,
    val onPrimaryContainerDark: Color,
    val onPrimaryContainerLight: Color
) {
    CYAN(
        id = "CYAN",
        displayName = "Cian tecnológico",
        previewColor = Color(0xFF22D3EE),
        primaryDark = Color(0xFF22D3EE),
        primaryLight = Color(0xFF0891B2),
        primaryContainerDark = Color(0xFF004F5C),
        primaryContainerLight = Color(0xFFBAF0FD),
        onPrimaryDark = Color(0xFF00363F),
        onPrimaryLight = Color.White,
        onPrimaryContainerDark = Color(0xFF9CF0FF),
        onPrimaryContainerLight = Color(0xFF001F26)
    ),
    GREEN(
        id = "GREEN",
        displayName = "Verde industrial",
        previewColor = Color(0xFF10B981),
        primaryDark = Color(0xFF10B981),
        primaryLight = Color(0xFF059669),
        primaryContainerDark = Color(0xFF005234),
        primaryContainerLight = Color(0xFFB1F7D1),
        onPrimaryDark = Color(0xFF003822),
        onPrimaryLight = Color.White,
        onPrimaryContainerDark = Color(0xFF6CF8B8),
        onPrimaryContainerLight = Color(0xFF002113)
    ),
    VIOLET(
        id = "VIOLET",
        displayName = "Violeta",
        previewColor = Color(0xFFA78BFA),
        primaryDark = Color(0xFFA78BFA),
        primaryLight = Color(0xFF7C3AED),
        primaryContainerDark = Color(0xFF4C1D95),
        primaryContainerLight = Color(0xFFDDD6FE),
        onPrimaryDark = Color(0xFF2E1065),
        onPrimaryLight = Color.White,
        onPrimaryContainerDark = Color(0xFFEDE9FE),
        onPrimaryContainerLight = Color(0xFF2E1065)
    ),
    AMBER(
        id = "AMBER",
        displayName = "Ámbar",
        previewColor = Color(0xFFFBBF24),
        primaryDark = Color(0xFFFBBF24),
        primaryLight = Color(0xFFD97706),
        primaryContainerDark = Color(0xFF78350F),
        primaryContainerLight = Color(0xFFFEF3C7),
        onPrimaryDark = Color(0xFF451A03),
        onPrimaryLight = Color.White,
        onPrimaryContainerDark = Color(0xFFFEF3C7),
        onPrimaryContainerLight = Color(0xFF451A03)
    );

    companion object {
        fun fromId(id: String?): AccentColor {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CYAN
        }
    }
}
