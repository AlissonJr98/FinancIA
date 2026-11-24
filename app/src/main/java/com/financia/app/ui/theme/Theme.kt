package com.financia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

enum class AppThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK_AMOLED("Escuro AMOLED")
}

private val AmoledDarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun FinancIATheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK_AMOLED -> true
    }
    val colorScheme = if (useDark) AmoledDarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
