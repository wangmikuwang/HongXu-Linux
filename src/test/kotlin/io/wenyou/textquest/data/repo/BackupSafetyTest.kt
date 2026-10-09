package io.wenyou.textquest.data.repo

import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.Story
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BackupSafetyTest {
    @Test fun importKeepsLocalKeysAndCanBeUndone() = runBlocking<Unit> {
        val dir = Files.createTempDirectory("backup").toFile()
        val library = LocalLibrary(dir)
        assertFalse(library.hasImportSnapshot())
        library.upsertProvider(ApiProfile("p", "DeepSeek", apiKey = "local-key"))
        library.upsertStory(Story("mine", "我的新剧情"))
        // An older backup, exported without keys, that does not know the new story.
        library.importBundle(AppBundle(providers = listOf(ApiProfile("p", "DeepSeek", apiKey = "")), stories = listOf(Story("old", "旧剧情"))))
        assertEquals("local-key", library.providers.value.single().apiKey)
        assertEquals(listOf("old"), library.stories.value.map { it.id })
        assertTrue(library.hasImportSnapshot())
        // The snapshot is private and system backups exclude it together with the keys.
        assertTrue(File(dir, "before-import.json").isFile)
        library.undoLastImport()
        assertEquals(listOf("mine"), library.stories.value.map { it.id })
        assertEquals("local-key", library.providers.value.single().apiKey)
        dir.deleteRecursively()
    }

    @Test fun systemBackupsNeverCarryTheKeys() {
        val res = listOf(File("src/main/res/xml"), File("app/src/main/res/xml")).first(File::isDirectory)
        for (name in listOf("backup_rules.xml", "data_extraction_rules.xml")) {
            val text = File(res, name).readText()
            assertTrue(name, text.contains("path=\"lib/providers.json\""))
        }
        // Both cloud backup and device-to-device transfer leave the keys out.
        assertEquals(2, Regex("lib/providers.json").findAll(File(res, "data_extraction_rules.xml").readText()).count())
    }
}
