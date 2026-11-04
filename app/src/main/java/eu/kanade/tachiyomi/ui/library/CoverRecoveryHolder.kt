package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.manga.model.Manga

object CoverRecoveryHolder {
    var lastRecoveredManga: List<Manga> = emptyList()
    var lastRecoveredTitles: List<String> = emptyList()
}
