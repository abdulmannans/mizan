package app.mizan.android.data.remote

import app.mizan.android.core.IndiaClock
import app.mizan.android.domain.PricePoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class YahooEnvelope(val chart: YahooChart? = null)

@Serializable
private data class YahooChart(val result: List<YahooResult>? = null)

@Serializable
private data class YahooResult(
    val timestamp: List<Long>? = null,
    val indicators: YahooIndicators? = null,
)

@Serializable
private data class YahooIndicators(val quote: List<YahooQuote>? = null)

@Serializable
private data class YahooQuote(val close: List<Double?>? = null)

/** Daily closes from the Yahoo chart endpoint. Used for Nifty 50, COMEX metals and USDINR. */
@Singleton
class YahooChartClient @Inject constructor(
    private val fetcher: HttpFetcher,
    private val json: Json,
) {
    suspend fun closes(symbol: String, range: String = "5y"): List<PricePoint> {
        val encoded = symbol.replace("^", "%5E").replace("=", "%3D")
        val url = "$BASE/$encoded?range=$range&interval=1d&includePrePost=false"
        val parsed = json.decodeFromString<YahooEnvelope>(fetcher.get(url))
        val result = parsed.chart?.result?.firstOrNull() ?: return emptyList()
        val stamps = result.timestamp ?: return emptyList()
        val closes = result.indicators?.quote?.firstOrNull()?.close ?: return emptyList()

        return stamps.indices.mapNotNull { index ->
            val close = closes.getOrNull(index) ?: return@mapNotNull null
            if (close <= 0.0) return@mapNotNull null
            val date = Instant.ofEpochSecond(stamps[index]).atZone(IndiaClock.ZONE).toLocalDate()
            PricePoint(date, close)
        }
            .groupBy { it.date }
            .map { (date, points) -> PricePoint(date, points.last().value) }
            .sortedBy { it.date }
    }

    companion object {
        private const val BASE = "https://query1.finance.yahoo.com/v8/finance/chart"

        const val NIFTY_50 = "^NSEI"
        const val COMEX_GOLD = "GC=F"
        const val COMEX_SILVER = "SI=F"
        const val USD_INR = "USDINR=X"
    }
}
