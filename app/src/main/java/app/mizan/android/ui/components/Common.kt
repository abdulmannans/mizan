package app.mizan.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mizan.android.core.Compliance
import app.mizan.android.domain.Level
import app.mizan.android.domain.ScoreComponents
import app.mizan.android.ui.theme.LevelColors
import app.mizan.android.ui.theme.Shapes
import app.mizan.android.ui.theme.Space
import app.mizan.android.ui.theme.TextRole

/** The only card in the app. */
@Composable
fun MizanCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(Space.lg)) {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = TextRole.title)
                    trailing?.invoke()
                }
                Spacer(Modifier.height(Space.md))
            }
            content()
        }
    }
}

/** A title above content that is not boxed in a card. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextRole.title)
        trailing?.invoke()
    }
}

/** Label on the left, value on the right. Replaces the old hand-rolled key/value row. */
@Composable
fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                label,
                style = TextRole.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(value, style = TextRole.body, fontWeight = FontWeight.Medium, color = valueColor)
        },
    )
}

/**
 * A row standing for a fund, metal or event: name, a supporting line, and whatever belongs on
 * the right (usually a level badge).
 */
@Composable
fun EntityRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overline: String? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = overlineContent ?: overline?.let { { Text(it, style = TextRole.caption) } },
        headlineContent = {
            Text(
                title,
                style = TextRole.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    it,
                    style = TextRole.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = leading,
        trailingContent = trailing,
    )
}

data class Stat(val label: String, val value: String, val caption: String? = null)

/** Two to four figures across one line, for the numbers a screen leads with. */
@Composable
fun StatStrip(stats: List<Stat>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        stats.forEach { stat ->
            Column(Modifier.weight(1f)) {
                Text(
                    stat.label,
                    style = TextRole.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(stat.value, style = TextRole.figureSmall)
                if (stat.caption != null) {
                    Text(
                        stat.caption,
                        style = TextRole.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

enum class DetailPane(val label: String) {
    OVERVIEW("Overview"),
    SCORE("Score"),
    HISTORY("History"),
}

@Composable
fun DetailPaneSelector(
    selected: DetailPane,
    onSelect: (DetailPane) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        DetailPane.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = selected == entry,
                onClick = { onSelect(entry) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DetailPane.entries.size,
                ),
            ) {
                Text(entry.label)
            }
        }
    }
}

@Composable
fun ScoreBreakdown(components: ScoreComponents, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        components.labelled().forEach { (label, value, weight) ->
            Column(Modifier.padding(vertical = Space.xs)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = TextRole.secondary)
                    Text("$value / $weight", style = TextRole.secondary)
                }
                Spacer(Modifier.height(Space.xs))
                LinearProgressIndicator(
                    progress = { (value.toFloat() / weight).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun LevelBadge(level: Level?, score: Int?, modifier: Modifier = Modifier) {
    val label = when {
        score == null -> "Not scored"
        else -> "$score · ${level?.label ?: "--"}"
    }
    Surface(
        modifier = modifier,
        shape = app.mizan.android.ui.theme.Radius.pill,
        color = LevelColors.container(level),
    ) {
        Text(
            text = label,
            color = LevelColors.content(level),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Space.xs),
        )
    }
}

@Composable
fun ShariahBadge(status: String, modifier: Modifier = Modifier) {
    val (label, container, content) = when (status) {
        "verified_compliant" -> Triple(
            "Verified compliant",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        "non_compliant" -> Triple(
            "Non-compliant",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        "rejected" -> Triple(
            "Rejected",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        else -> Triple(
            "Unreviewed",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(modifier = modifier, shape = Shapes.field, color = container) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Space.sm, vertical = 3.dp),
        )
    }
}

/**
 * The single compliance surface. One line plus "Read" opens the full text, which keeps the
 * disclaimer present everywhere the docs require without a paragraph on every screen.
 */
@Composable
fun ComplianceChip(onRead: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.field,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(start = Space.md, end = Space.xs, top = Space.xs, bottom = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                Compliance.DISCLAIMER_SHORT,
                style = TextRole.secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRead) { Text("Read") }
        }
    }
}

/** Helper copy that genuinely explains something. Not a home for legal text. */
@Composable
fun FootnoteText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextRole.secondary,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun GrowthText(percent: Double?, modifier: Modifier = Modifier) {
    val color = when {
        percent == null -> MaterialTheme.colorScheme.onSurfaceVariant
        percent > 0 -> MaterialTheme.colorScheme.primary
        percent < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = app.mizan.android.core.Formatters.percentSigned(percent),
        style = TextRole.secondary,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, Shapes.card)
            .padding(Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextRole.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
