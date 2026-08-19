package app.mizan.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One 4dp scale for the whole app. Anything that needs a gap picks from here rather than
 * inventing a value, which is how the old screens ended up with 2/6/10/14dp all at once.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object Radius {
    val card = 16.dp
    val field = 12.dp
    val pill = RoundedCornerShape(50)
}

object Shapes {
    val card: Shape = RoundedCornerShape(Radius.card)
    val field: Shape = RoundedCornerShape(Radius.field)
}

/**
 * Semantic text roles. Screens ask for the job a string is doing rather than picking a Material
 * style directly, so a figure always outranks its label.
 */
object TextRole {

    /** Headline number: a NAV, a price, a total. */
    val figure: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)

    /** Inline number inside a row or strip. */
    val figureSmall: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

    /** The name of a thing: a fund, a metal, a section. */
    val title: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium

    /** Body copy and row labels. */
    val body: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium

    /** Secondary line under a title. */
    val secondary: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall

    /** Dates, units, helper text. */
    val caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography.labelSmall
}
