package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.domain.source.service.SourcePreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.service.SourceManager

class PreferenceBackupCreatorTest {

    @Test
    fun `adult content choice is not included in app preferences backup`() {
        val preferenceStore = mockk<PreferenceStore> {
            every { getAll() } returns mapOf(
                SourcePreferences.SHOW_NSFW_SOURCE_PREF_KEY to true,
                "ordinary_preference" to true,
                Preference.appStateKey("internal_preference") to true,
            )
        }
        val creator = PreferenceBackupCreator(
            sourceManager = mockk<SourceManager>(),
            preferenceStore = preferenceStore,
        )

        val backupKeys = creator.createApp(includePrivatePreferences = true).map { it.key }

        assertEquals(listOf("ordinary_preference"), backupKeys)
    }
}
