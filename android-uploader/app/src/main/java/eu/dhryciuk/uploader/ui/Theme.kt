package eu.dhryciuk.uploader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD7B46A),
    onPrimary = Color(0xFF1B160D),
    secondary = Color(0xFFAFC6B6),
    background = Color(0xFF101112),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF191B1C),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF252829),
    onSurfaceVariant = Color(0xFFC7C8C5),
    error = Color(0xFFFFB4AB)
)

@Composable
fun DHUploaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
