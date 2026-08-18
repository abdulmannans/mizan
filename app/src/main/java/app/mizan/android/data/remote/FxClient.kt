package app.mizan.android.data.remote

import app.mizan.android.domain.PricePoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class FrankfurterLatest(val rates: Map<String, Double> = emptyMap())

/** USDINR. Frankfurter for the spot rate, Yahoo as the fallback and for history. */
@Singleton
class FxClient @Inject constructor(
    private val fetcher: HttpFetcher,
    private val yahoo: YahooChartClient,
    private val json: Json,
) {
    suspend fun usdInrLatest(): Double? {
        val fromFrankfurter = runCatching {
            val body = fetcher.get("https://api.frankfurter.app/latest?from=USD&to=INR")
            json.decodeFromString<FrankfurterLatest>(body).rates["INR"]
        }.getOrNull()
        if (fromFrankfurter != null && fromFrankfurter > 0) return fromFrankfurter

        return runCatching { yahoo.closes(YahooChartClient.USD_INR, range = "1mo").lastOrNull()?.value }
            .getOrNull()
    }

    suspend fun usdInrHistory(range: String = "5y"): List<PricePoint> =
        runCatching { yahoo.closes(YahooChartClient.USD_INR, range) }.getOrDefault(emptyList())
}
