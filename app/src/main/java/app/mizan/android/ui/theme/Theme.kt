package app.mizan.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.mizan.android.domain.Level

private val Emerald10 = Color(0xFF04231A)
private val Emerald20 = Color(0xFF06382A)
private val Emerald40 = Color(0xFF0E7C5A)
private val Emerald50 = Color(0xFF10996E)
private val Emerald60 = Color(0xFF34D399)
private val Emerald90 = Color(0xFFD1FAE5)

private val Slate05 = Color(0xFF0B0F0D)
private val Slate10 = Color(0xFF121815)
private val Slate20 = Color(0xFF1B2320)
private val Slate80 = Color(0xFFCBD5D1)
private val Slate95 = Color(0xFFF4F7F5)

private val Amber = Color(0xFFB45309)
private val AmberDark = Color(0xFFFBBF24)
private val Danger = Color(0xFFB3261E)
private val DangerDark = Color(0xFFF2B8B5)

private val LightScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = Emerald90,
    onPrimaryContainer = Emerald10,
    secondary = Emerald50,
    onSecondary = Color.White,
    background = Slate95,
    onBackground = Slate05,
    surface = Color.White,
    onSurface = Slate05,
    surfaceVariant = Color(0xFFE3EAE6),
    onSurfaceVariant = Color(0xFF41504A),
    error = Danger,
    onError = Color.White,
    outline = Color(0xFF71807A),
)

private val DarkScheme = darkColorScheme(
    primary = Emerald60,
    onPrimary = Emerald10,
    primaryContainer = Emerald20,
    onPrimaryContainer = Emerald90,
    secondary = Emerald50,
    onSecondary = Emerald10,
    background = Slate05,
    onBackground = Slate95,
    surface = Slate10,
    onSurface = Slate95,
    surfaceVariant = Slate20,
    onSurfaceVariant = Slate80,
    error = DangerDark,
    onError = Color(0xFF601410),
    outline = Color(0xFF5C6B65),
)

private val MizanTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        bodySmall = base.bodySmall.copy(lineHeight = 18.sp),
    )
}

/** Score readouts are tabular so the columns in Missed line up. */
val NumberStyle: TextStyle = TextStyle(fontWeight = FontWeight.SemiBold)

@Composable
fun MizanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MizanTypography,
        content = content,
    )
}

object LevelColors {
    @Composable
    @ReadOnlyComposable
    fun container(level: Level?): Color {
        val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
        return when (level) {
            Level.EXCEPTIONAL, Level.VERY_ATTRACTIVE ->
                if (dark) Emerald20 else Emerald90
            Level.ATTRACTIVE -> if (dark) Color(0xFF3F2D06) else Color(0xFFFEF3C7)
            Level.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
            Level.WEAK, Level.UNATTRACTIVE ->
                if (dark) Color(0xFF3A1512) else Color(0xFFFDE2E0)
            null -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

    @Composable
    @ReadOnlyComposable
    fun content(level: Level?): Color {
        val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
        return when (level) {
            Level.EXCEPTIONAL, Level.VERY_ATTRACTIVE -> if (dark) Emerald60 else Emerald10
            Level.ATTRACTIVE -> if (dark) AmberDark else Amber
            Level.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
            Level.WEAK, Level.UNATTRACTIVE -> if (dark) DangerDark else Danger
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}

private fun Color.luminanceIsDark(): Boolean = (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5
