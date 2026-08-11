package mihon.data.extension.service

import android.app.Application
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mihon.data.extension.model.CuratedExtensionStoreCatalog
import mihon.domain.extension.model.ExtensionStore

class CuratedExtensionStoreService(
    private val application: Application,
    private val json: Json,
) {
    fun load(): List<ExtensionStore> {
        return try {
            application.assets.open(ASSET_NAME).bufferedReader().use { reader ->
                json.decodeFromString<CuratedExtensionStoreCatalog>(reader.readText())
                    .stores
                    .map { it.toDomainModel() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val ASSET_NAME = "extension_stores.json"
    }
}
