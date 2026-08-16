package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MangaTest {

    @Test
    fun `New manga use ascending chapter order by default`() {
        Manga.create().sortDescending() shouldBe false
    }
}
