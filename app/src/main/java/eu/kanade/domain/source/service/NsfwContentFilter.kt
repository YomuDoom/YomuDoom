package eu.kanade.domain.source.service

import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NsfwContentFilter(
    private val preferences: SourcePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
) {

    fun isSourceAllowed(sourceId: Long): Boolean {
        return preferences.showNsfwSource.get() || sourceId !in extensionManager.nsfwSourceIds.value
    }

    fun isMangaAllowed(manga: Manga): Boolean = isSourceAllowed(manga.source)

    fun changes(): Flow<Unit> {
        return combine(
            preferences.showNsfwSource.changes(),
            extensionManager.nsfwSourceIds,
        ) { _, _ -> true }
            .map { }
    }
}
