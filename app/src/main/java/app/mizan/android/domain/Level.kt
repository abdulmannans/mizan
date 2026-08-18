package app.mizan.android.domain

/**
 * Dip meter levels. This is a measure of how beaten-up a NAV is versus its own history,
 * never a ranking of one fund against another.
 */
enum class Level(val key: String, val label: String, val rank: Int) {
    UNATTRACTIVE("unattractive", "Unattractive", 0),
    WEAK("weak", "Weak", 1),
    NEUTRAL("neutral", "Neutral", 2),
    ATTRACTIVE("attractive", "Attractive", 3),
    VERY_ATTRACTIVE("very_attractive", "Very attractive", 4),
    EXCEPTIONAL("exceptional", "Exceptional", 5);

    val isAttractive: Boolean get() = rank >= ATTRACTIVE.rank

    companion object {
        /** Alerts and missed-deploy rows exist only at or above this score. */
        const val ATTRACTIVE_SCORE = 65

        fun of(score: Int): Level = when {
            score >= 90 -> EXCEPTIONAL
            score >= 80 -> VERY_ATTRACTIVE
            score >= ATTRACTIVE_SCORE -> ATTRACTIVE
            score >= 50 -> NEUTRAL
            score >= 30 -> WEAK
            else -> UNATTRACTIVE
        }

        fun fromKey(key: String): Level = entries.firstOrNull { it.key == key } ?: UNATTRACTIVE
    }
}
