package com.financia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF00897B)
val EmeraldDark = Color(0xFF00695C)
val EmeraldLight = Color(0xFF4DB6AC)
val DeepNavy = Color(0xFF0F172A)
val SlateSurface = Color(0xFF1E293B)
val IncomeGreen = Color(0xFF16A34A)
val ExpenseRed = Color(0xFFDC2626)
val AiPurple = Color(0xFF7C3AED)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = AiPurple,
    onSecondary = Color.White,
    background = DeepNavy,
    surface = SlateSurface
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = AiPurple,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White
)

@Composable
fun FinancIATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
