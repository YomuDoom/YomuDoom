package mihon.data.extension.model

import kotlinx.serialization.Serializable
import mihon.domain.extension.model.ExtensionStore

@Serializable
data class CuratedExtensionStoreCatalog(
    val stores: List<CuratedExtensionStore> = emptyList(),
)

@Serializable
data class CuratedExtensionStore(
    val indexUrl: String,
    val name: String,
    val badgeLabel: String,
    val signingKey: String,
    val website: String,
    val discord: String? = null,
    val isLegacy: Boolean = false,
    val extensionListUrl: String? = null,
) {
    fun toDomainModel(): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = ExtensionStore.Contact(
                website = website,
                discord = discord,
            ),
            isLegacy = isLegacy,
            extensionListUrl = extensionListUrl,
        )
    }
}
