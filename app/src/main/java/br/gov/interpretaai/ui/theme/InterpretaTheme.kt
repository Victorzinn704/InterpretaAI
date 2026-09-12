package br.gov.interpretaai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ComicInk = Color(0xFF111C2D)
val ComicCream = Color(0xFFFFFDF5)
val ComicYellow = Color(0xFFFACC15)
val ComicRed = Color(0xFFE11D48)
val ComicGreen = Color(0xFF16A34A)
val ComicBlue = Color(0xFF0284C7)
val SoftBlue = Color(0xFFE7EEFF)
val SoftGreen = Color(0xFFE8FAEF)

private val colors = lightColorScheme(
    primary = ComicRed,
    onPrimary = Color.White,
    secondary = ComicBlue,
    tertiary = ComicGreen,
    background = ComicCream,
    surface = Color.White,
    onSurface = ComicInk,
    outline = ComicInk
)

@Composable
fun InterpretaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
