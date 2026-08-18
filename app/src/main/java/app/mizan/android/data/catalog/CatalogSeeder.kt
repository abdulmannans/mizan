package app.mizan.android.data.catalog

import android.content.Context
import app.mizan.android.data.db.FundEntity
import app.mizan.android.data.db.MetalEntity
import app.mizan.android.data.db.MizanDatabase
import app.mizan.android.data.db.SHARIAH_UNREVIEWED
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CatalogFile(
    val catalogAsOf: String,
    val funds: List<CatalogFund>,
    val metals: List<CatalogMetal>,
)

@Serializable
private data class CatalogFund(
    val schemeCode: Long,
    val name: String,
    val shortName: String,
    val amc: String,
    val isin: String? = null,
    val mandate: String,
    val shariahStatus: String = SHARIAH_UNREVIEWED,
    val shariahNotes: String? = null,
)

@Serializable
private data class CatalogMetal(
    val id: String,
    val name: String,
    val unit: String,
    val dropThresholdRupees: Double,
)

/**
 * The fund universe ships inside the APK; new schemes arrive with an app release rather than an
 * in-app editor.
 */
@Singleton
class CatalogSeeder @Inject constructor(
    private val context: Context,
    private val database: MizanDatabase,
    private val json: Json,
) {
    suspend fun seed() {
        val raw = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val catalog = json.decodeFromString<CatalogFile>(raw)

        val existing = database.fundDao().tracked().associateBy { it.schemeCode }
        val funds = catalog.funds.map { fund ->
            val current = existing[fund.schemeCode]
            FundEntity(
                schemeCode = fund.schemeCode,
                name = fund.name,
                shortName = fund.shortName,
                amc = fund.amc,
                isin = fund.isin,
                mandate = fund.mandate,
                catalogAsOf = catalog.catalogAsOf,
                shariahStatus = fund.shariahStatus,
                shariahNotes = fund.shariahNotes,
                active = true,
                trackingEnabled = true,
                lastNav = current?.lastNav,
                navAsOf = current?.navAsOf,
                growth1d = current?.growth1d,
                growth1m = current?.growth1m,
                growth1y = current?.growth1y,
            )
        }
        database.fundDao().upsert(funds)

        val existingMetals = database.metalDao().all().associateBy { it.id }
        val metals = catalog.metals.map { metal ->
            val current = existingMetals[metal.id]
            MetalEntity(
                id = metal.id,
                name = metal.name,
                unit = metal.unit,
                dropThresholdRupees = metal.dropThresholdRupees,
                lastPrice = current?.lastPrice,
                priceAsOf = current?.priceAsOf,
                growth1d = current?.growth1d,
                growth1m = current?.growth1m,
                growth1y = current?.growth1y,
            )
        }
        database.metalDao().upsertMetals(metals)
    }

    private companion object {
        const val ASSET_PATH = "catalog/funds.json"
    }
}
