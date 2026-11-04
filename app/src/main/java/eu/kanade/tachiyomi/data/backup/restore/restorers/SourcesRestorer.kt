package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesRestorer(
    private val stubSourceRepository: StubSourceRepository = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
) {

    suspend operator fun invoke(backupSources: List<BackupSource>) {
        if (backupSources.isEmpty()) return

        val downloadDirs = storageManager.getDownloadsDirectory()?.listFiles().orEmpty()
            .filter { it.isDirectory && !it.name.isNullOrBlank() }
            .mapNotNull { it.name }

        backupSources.forEach { backupSource ->
            var lang = backupSource.lang
            if (lang.isBlank()) {
                val matchingDir = downloadDirs.firstOrNull { dirName ->
                    dirName.startsWith("${backupSource.name} (", ignoreCase = true)
                }
                if (matchingDir != null) {
                    val langMatch = Regex("""\(([a-zA-Z0-9\-_]+)\)$""").find(matchingDir)
                    if (langMatch != null) {
                        lang = langMatch.groupValues[1].lowercase()
                    }
                }
            }
            stubSourceRepository.upsertStubSource(backupSource.sourceId, lang, backupSource.name)
        }
    }
}
