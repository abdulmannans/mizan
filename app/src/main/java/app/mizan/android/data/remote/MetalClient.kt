package app.mizan.android.data.remote

import app.mizan.android.core.IndiaClock
import app.mizan.android.data.db.METAL_GOLD
import app.mizan.android.data.db.METAL_SILVER
import app.mizan.android.domain.PricePoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * India gold and silver, Mumbai-style. Quotes exclude making charges and GST.
 *
 * Latest price order: GoodReturns Mumbai page, then COMEX x USDINR. History is always COMEX x FX
 * scaled to the India level, because no free source publishes long Indian retail series.
 */
@Singleton
class MetalClient @Inject constructor(
    private val fetcher: HttpFetcher,
    private val yahoo: YahooChartClient,
    private val fx: FxClient,
) {
    /** Gold in rupees per 10g of 24K. */
    suspend fun goldLatest(): PricePoint? = indiaQuote(
        url = GOLD_URL,
        unitRow = "10",
        perGramRowMultiplier = GRAMS_PER_10G,
        min = MIN_GOLD_PER_10G,
        max = MAX_GOLD_PER_10G,
        comexSymbol = YahooChartClient.COMEX_GOLD,
        grams = GRAMS_PER_10G,
    )

    /** Silver in rupees per kg. */
    suspend fun silverLatest(): PricePoint? = indiaQuote(
        url = SILVER_URL,
        unitRow = "1000",
        perGramRowMultiplier = GRAMS_PER_KG,
        min = MIN_SILVER_PER_KG,
        max = MAX_SILVER_PER_KG,
        comexSymbol = YahooChartClient.COMEX_SILVER,
        grams = GRAMS_PER_KG,
    )

    suspend fun history(metalId: String, anchor: Double?): List<PricePoint> {
        val symbol = if (metalId == METAL_SILVER) {
            YahooChartClient.COMEX_SILVER
        } else {
            YahooChartClient.COMEX_GOLD
        }
        val grams = if (metalId == METAL_SILVER) GRAMS_PER_KG else GRAMS_PER_10G

        val usdPerOunce = runCatching { yahoo.closes(symbol) }.getOrDefault(emptyList())
        if (usdPerOunce.isEmpty()) return emptyList()

        val fxSeries = fx.usdInrHistory()
        val fxByDate = fxSeries.associate { it.date to it.value }
        val fallbackRate = fxSeries.lastOrNull()?.value ?: return emptyList()

        var carriedRate = fxSeries.firstOrNull()?.value ?: fallbackRate
        val converted = usdPerOunce.map { point ->
            carriedRate = fxByDate[point.date] ?: carriedRate
            PricePoint(point.date, point.value / GRAMS_PER_OUNCE * grams * carriedRate)
        }

        // Indian retail sits above the plain COMEX conversion (duty, premium), so scale the whole
        // series so its last point matches the India quote we actually show.
        val last = converted.lastOrNull()?.value ?: return converted
        if (anchor == null || anchor <= 0.0 || last <= 0.0) return converted
        val factor = anchor / last
        return converted.map { PricePoint(it.date, it.value * factor) }
    }

    val supportedIds: List<String> = listOf(METAL_GOLD, METAL_SILVER)

    /**
     * Prefer the Indian retail quote, but only when it survives a cross-check against the physical
     * COMEX conversion. India sits above COMEX on duty and premium; several times above it means
     * the page layout changed and we parsed an advertisement.
     */
    private suspend fun indiaQuote(
        url: String,
        unitRow: String,
        perGramRowMultiplier: Double,
        min: Double,
        max: Double,
        comexSymbol: String,
        grams: Double,
    ): PricePoint? {
        val reference = comex(comexSymbol, grams)
        val scraped = goodReturns(url, unitRow, perGramRowMultiplier, min, max)
        if (scraped != null) {
            val referenceValue = reference?.value
            val ratio = if (referenceValue != null && referenceValue > 0) {
                scraped.value / referenceValue
            } else {
                null
            }
            if (ratio == null || ratio in MIN_INDIA_PREMIUM..MAX_INDIA_PREMIUM) return scraped
        }
        return reference
    }

    private suspend fun comex(symbol: String, grams: Double): PricePoint? {
        val usd = runCatching { yahoo.closes(symbol, range = "1mo").lastOrNull() }.getOrNull()
            ?: return null
        val rate = fx.usdInrLatest() ?: return null
        val price = usd.value / GRAMS_PER_OUNCE * grams * rate
        return PricePoint(IndiaClock.today(), price)
    }

    /**
     * GoodReturns publishes a per-gram table (Gram / 24K / 22K / 18K for gold). The first rate cell
     * in a row is the purest grade, and rupee signs arrive as HTML entities.
     */
    private suspend fun goodReturns(
        url: String,
        unitRow: String,
        perGramRowMultiplier: Double,
        min: Double,
        max: Double,
    ): PricePoint? = runCatching {
        val html = decodeRupeeEntities(fetcher.get(url, accept = "text/html"))

        rowValue(html, unitRow)?.takeIf { it in min..max }
            ?.let { return@runCatching PricePoint(IndiaClock.today(), it) }

        // Fall back to the single-gram row, which is the least likely row to be restyled.
        rowValue(html, "1")
            ?.times(perGramRowMultiplier)
            ?.takeIf { it in min..max }
            ?.let { return@runCatching PricePoint(IndiaClock.today(), it) }

        null
    }.getOrNull()

    private fun rowValue(html: String, unitRow: String): Double? {
        val pattern = Regex(
            """<td>\s*$unitRow\s*</td>\s*<td>[^₹]{0,60}₹\s*([0-9][0-9,]{2,})""",
            RegexOption.IGNORE_CASE,
        )
        return pattern.find(html)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun decodeRupeeEntities(html: String): String = html
        .replace("&#x20b9;", "₹", ignoreCase = true)
        .replace("&#8377;", "₹")
        .replace("&nbsp;", " ")

    private companion object {
        const val GOLD_URL = "https://www.goodreturns.in/gold-rates/mumbai.html"
        const val SILVER_URL = "https://www.goodreturns.in/silver-rates/mumbai.html"

        const val GRAMS_PER_OUNCE = 31.1034768
        const val GRAMS_PER_10G = 10.0
        const val GRAMS_PER_KG = 1000.0

        // Sanity windows keep a stray page number from becoming a price.
        const val MIN_GOLD_PER_10G = 30_000.0
        const val MAX_GOLD_PER_10G = 500_000.0
        const val MIN_SILVER_PER_KG = 40_000.0
        const val MAX_SILVER_PER_KG = 1_000_000.0

        const val MIN_INDIA_PREMIUM = 0.7
        const val MAX_INDIA_PREMIUM = 2.0
    }
}
