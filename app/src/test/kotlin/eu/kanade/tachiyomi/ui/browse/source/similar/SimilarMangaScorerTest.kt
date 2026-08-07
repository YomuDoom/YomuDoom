package eu.kanade.tachiyomi.ui.browse.source.similar

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

class SimilarMangaScorerTest {

    @Test
    fun `shared metadata produces a higher score`() {
        val reference = Manga.create().copy(
            title = "The Ancient Mage",
            author = "A. Writer",
            genre = listOf("Fantasy", "Adventure"),
            description = "A young mage explores a dangerous world.",
        )
        val similar = Manga.create().copy(
            title = "The Ancient Mage Returns",
            author = "A. Writer",
            genre = listOf("Fantasy", "Adventure"),
            description = "A mage explores another dangerous world.",
        )
        val unrelated = Manga.create().copy(
            title = "City of Machines",
            author = "Different Author",
            genre = listOf("Romance"),
            description = "A quiet story about city life.",
        )

        assertTrue(
            SimilarMangaScorer.score(reference, similar) >
                SimilarMangaScorer.score(reference, unrelated),
        )
    }

    @Test
    fun `normalization ignores accents and separators`() {
        assertTrue(
            SimilarMangaScorer.normalize("Ação: No. 1") ==
                SimilarMangaScorer.normalize("acao no 1"),
        )
    }
}
