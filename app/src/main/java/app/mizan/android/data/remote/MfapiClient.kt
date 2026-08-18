package app.mizan.android.data.remote

import app.mizan.android.domain.PricePoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MfapiResponse(
    val meta: MfapiMeta? = null,
    val data: List<MfapiPoint> = emptyList(),
    val status: String? = null,
)

@Serializable
private data class MfapiMeta(
    val scheme_name: String? = null,
    val fund_house: String? = null,
    val isin_growth: String? = null,
)

@Serializable
private data class MfapiPoint(val date: String, val nav: String)

/** NAV history for a Direct Growth scheme code. MFAPI dates are already Indian session dates. */
@Singleton
class MfapiClient @Inject constructor(
    private val fetcher: HttpFetcher,
    private val json: Json,
) {
    suspend fun history(schemeCode: Long): List<PricePoint> {
        val body = fetcher.get("$BASE/$schemeCode")
        val parsed = json.decodeFromString<MfapiResponse>(body)
        return parsed.data
            .mapNotNull { point ->
                val nav = point.nav.toDoubleOrNull() ?: return@mapNotNull null
                if (nav <= 0.0) return@mapNotNull null
                val date = runCatching { LocalDate.parse(point.date, DATE_FORMAT) }.getOrNull()
                    ?: return@mapNotNull null
                PricePoint(date, nav)
            }
            .sortedBy { it.date }
    }

    private companion object {
        const val BASE = "https://api.mfapi.in/mf"
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }
}
