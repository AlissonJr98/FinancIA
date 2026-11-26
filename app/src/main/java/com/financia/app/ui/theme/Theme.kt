package com.financia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF00897B)
val EmeraldDark = Color(0xFF00695C)
val EmeraldLight = Color(0xFF26A69A)
val EmeraldNeon = Color(0xFF10B981)

val AmoledBlack = Color(0xFF070B12)
val AmoledCardSurface = Color(0xFF111827)
val AmoledCardVariant = Color(0xFF1E293B)

val IncomeGreen = Color(0xFF16A34A)
val ExpenseRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val AiPurple = Color(0xFF8B5CF6)

enum class AppThemeMode(val label: String, val emoji: String, val subtitle: String) {
    EMERALD_DARK("Esmeralda Neo-Bank", "🌲", "Dark AMOLED com Verde Neon"),
    ULTRAVIOLET("Ultravioleta", "💜", "Roxo Profundo & Neon"),
    GOLD_BLACK("Gold Black", "🪙", "Preto Puro com Dourado"),
    LIGHT("Clean Light", "☀️", "Modo Claro Moderno");

    companion object {
        fun fromName(name: String): AppThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: if (name.equals("SYSTEM", ignoreCase = true) || name.equals("DARK_AMOLED", ignoreCase = true)) EMERALD_DARK
                else EMERALD_DARK
        }
    }
}

// 1. Esmeralda Neo-Bank (Padrão Dark AMOLED)
private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldNeon,
    onPrimary = Color(0xFF00201A),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = AiPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    background = AmoledBlack,
    surface = AmoledCardSurface,
    surfaceVariant = AmoledCardVariant,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

// 2. Ultravioleta Premium
private val UltravioletColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color(0xFF1E0538),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFFC084FC),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = Color(0xFFE9D5FF),
    background = Color(0xFF090514),
    surface = Color(0xFF160E29),
    surfaceVariant = Color(0xFF241640),
    onBackground = Color(0xFFFAF5FF),
    onSurface = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFFA855F7)
)

// 3. Gold Black Edition (Preto OLED com Dourado de Luxo)
private val GoldBlackColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),
    onPrimary = Color(0xFF291800),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF000000),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF242424),
    onBackground = Color(0xFFFFFBEB),
    onSurface = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFFD4AF37)
)

// 4. Clean Light
private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun FinancIATheme(
    themeMode: AppThemeMode = AppThemeMode.EMERALD_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.EMERALD_DARK -> EmeraldColorScheme
        AppThemeMode.ULTRAVIOLET -> UltravioletColorScheme
        AppThemeMode.GOLD_BLACK -> GoldBlackColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
