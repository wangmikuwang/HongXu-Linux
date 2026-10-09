package io.wenyou.textquest.data.repo

import android.content.Context
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.SaveSlot
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.model.SessionState
import io.wenyou.textquest.data.model.AchievementRecord
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.model.StoryProgress
import io.wenyou.textquest.data.engine.Achievements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 轻量本地资料库：所有实体以 JSON 文件存于应用私有目录，
 * 无数据库迁移负担，可直接整体导出/导入。启动时加载进内存，
 * 每次变更写盘并更新 StateFlow。
 */
class LocalLibrary internal constructor(private val dir: File) {

    constructor(context: Context) : this(File(context.filesDir, "lib"))

    init { dir.mkdirs() }
    // ponytail: 资料库写入共用一把锁；数据量大到阻塞编辑时再改用数据库事务。
    private val lock = Any()
    private val _writeError = MutableStateFlow<String?>(null)
    val writeError: StateFlow<String?> = _writeError.asStateFlow()
    fun clearWriteError() { _writeError.value = null }

    private suspend fun <T> write(block: () -> T): T = withContext(Dispatchers.IO) {
        synchronized(lock) { block() }
    }
    private val providersFile = File(dir, "providers.json")
    private val charactersFile = File(dir, "characters.json")
    private val storiesFile = File(dir, "stories.json")
    private val savesFile = File(dir, "saves.json")
    private val baselineFile = File(dir, "baseline.txt")
    private val importSnapshotFile = File(dir, "before-import.json")
    private val achievementsFile = File(dir, "achievements.json")
    private val progressFile = File(dir, "progress.json")
    // A finite achievement write belongs to the library, so leaving a play screen cannot cancel it.
    private val achievementScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Runs before the lists below are read so folded-in legacy rules land in the files they load.
    private val _baseline = MutableStateFlow(Baseline.migrate(dir, baselineFile))
    private val _providers = MutableStateFlow(readList(providersFile, ApiProfile.serializer()))
    private val _characters = MutableStateFlow(readList(charactersFile, CharacterData.serializer()))
    private val _stories = MutableStateFlow(readList(storiesFile, Story.serializer()))
    private val _saves = MutableStateFlow(readList(savesFile, SaveSlot.serializer()))
    private val _achievements = MutableStateFlow(Achievements.merge(emptyList(), readList(achievementsFile, AchievementRecord.serializer()), System.currentTimeMillis()))

    val providers: StateFlow<List<ApiProfile>> = _providers.asStateFlow()
    val characters: StateFlow<List<CharacterData>> = _characters.asStateFlow()
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()
    val saves: StateFlow<List<SaveSlot>> = _saves.asStateFlow()
    /** The one baseline every AI request carries; see [Baseline]. */
    val baseline: StateFlow<String> = _baseline.asStateFlow()
    fun currentBaseline(): String = _baseline.value.ifBlank { Baseline.DEFAULT }
    val achievements: StateFlow<List<AchievementRecord>> = _achievements.asStateFlow()
    private val _progress = MutableStateFlow(readList(progressFile, StoryProgress.serializer()))
    /** Branch nodes reached per story, shown on the branch map. */
    val progress: StateFlow<List<StoryProgress>> = _progress.asStateFlow()

    fun trackAchievements(story: Story, state: SessionState, onUnlocked: (List<String>) -> Unit) {
        achievementScope.launch {
            try { onUnlocked(recordAchievements(story, state)) }
            catch (_: IOException) { /* The existing write-error flow reports the failure. */ }
        }
    }

    internal suspend fun recordAchievements(story: Story, state: SessionState): List<String> = write {
        val previous = _achievements.value
        val next = Achievements.observe(previous, story, state, System.currentTimeMillis())
        if (next != previous) {
            persistList(achievementsFile, next, AchievementRecord.serializer())
            _achievements.value = next
        }
        // The same play-state hook records which branch nodes were reached.
        val reached = state.currentNodeId.takeIf { story.mode == StoryMode.SCRIPT && state.storyId == story.id && it in story.nodes }
        val old = _progress.value.firstOrNull { it.storyId == story.id }
        if (reached != null && (old == null || reached !in old.visitedNodes)) {
            val updated = replaceById(_progress.value, story.id, StoryProgress(story.id, old?.visitedNodes.orEmpty() + reached))
            persistList(progressFile, updated, StoryProgress.serializer())
            _progress.value = updated
        }
        next.filter { it.unlockedAt > 0L && previous.none { old -> old.id == it.id && old.unlockedAt > 0L } }.map { it.id }
    }

    // ---------------- CRUD ----------------

    suspend fun upsertProvider(p: ApiProfile) = write {
        _providers.value = replaceById(_providers.value, p.id, p).also { persistList(providersFile, it, ApiProfile.serializer()) }
    }

    suspend fun deleteProvider(id: String) = write {
        _providers.value = _providers.value.filterNot { it.id == id }.also {
            persistList(providersFile, it, ApiProfile.serializer())
        }
    }

    suspend fun upsertCharacter(c: CharacterData) = write {
        _characters.value = replaceById(_characters.value, c.id, c).also {
            persistList(charactersFile, it, CharacterData.serializer())
        }
    }

    suspend fun deleteCharacter(id: String) = write {
        _characters.value = _characters.value.filterNot { it.id == id }.also {
            persistList(charactersFile, it, CharacterData.serializer())
        }
    }

    suspend fun upsertStory(s: Story) = write {
        _stories.value = replaceById(_stories.value, s.id, s).also {
            persistList(storiesFile, it, Story.serializer())
        }
    }

    suspend fun deleteStory(id: String) = write {
        _stories.value = _stories.value.filterNot { it.id == id }.also {
            persistList(storiesFile, it, Story.serializer())
        }
        _saves.value = _saves.value.filterNot { it.state.storyId == id }.also {
            persistList(savesFile, it, SaveSlot.serializer())
        }
    }

    suspend fun upsertSave(slot: SaveSlot) = write {
        _saves.value = replaceById(_saves.value, slot.id, slot).also {
            persistList(savesFile, it, SaveSlot.serializer())
        }
    }

    suspend fun deleteSave(id: String) = write {
        _saves.value = _saves.value.filterNot { it.id == id }.also {
            persistList(savesFile, it, SaveSlot.serializer())
        }
    }

    /** Blank restores the default, so AI generation is never left without a baseline. */
    suspend fun setBaseline(text: String) = write {
        val value = text.trim().take(Baseline.MAX_LENGTH).ifBlank { Baseline.DEFAULT }
        atomicWrite(baselineFile) { value }
        _baseline.value = value
    }

    // ---------------- 批量/导入导出 ----------------

    fun bundle(): AppBundle = synchronized(lock) { AppBundle(
        exportedAt = System.currentTimeMillis(),
        providers = _providers.value,
        characters = _characters.value,
        stories = _stories.value,
        saves = _saves.value,
        baseline = _baseline.value,
        achievements = _achievements.value,
        progress = _progress.value,
        origin = io.wenyou.textquest.BuildConfig.SHARE_ORIGIN
    ) }

    /** The library as it was before the last backup import, so that import can be undone. */
    fun hasImportSnapshot(): Boolean = importSnapshotFile.isFile

    suspend fun undoLastImport(): Int {
        val snapshot = withContext(Dispatchers.IO) { AppJson.decodeFromString(AppBundle.serializer(), importSnapshotFile.readText()) }
        return importBundle(snapshot)
    }

    /**
     * Replaces the library with a backup. The current library is kept as a snapshot first, and providers the backup
     * lists without an API key (backups leave keys out by default) keep the key already on this device.
     */
    suspend fun importBundle(bundle: AppBundle): Int = write {
        atomicWrite(importSnapshotFile) { AppJson.encodeToString(AppBundle.serializer(), bundle()) }
        val keys = _providers.value.associate { it.id to it.apiKey }
        val providers = bundle.providers.map { p -> if (p.apiKey.isBlank()) p.copy(apiKey = keys[p.id].orEmpty()) else p }
        persistList(providersFile, providers, ApiProfile.serializer())
        _providers.value = providers
        persistList(charactersFile, bundle.characters, CharacterData.serializer())
        _characters.value = bundle.characters
        persistList(storiesFile, bundle.stories, Story.serializer())
        _stories.value = bundle.stories
        persistList(savesFile, bundle.saves, SaveSlot.serializer())
        _saves.value = bundle.saves
        // Only the player's own backups carry a baseline; shared content never reaches here.
        if (bundle.baseline.isNotBlank()) {
            val value = bundle.baseline.trim().take(Baseline.MAX_LENGTH)
            atomicWrite(baselineFile) { value }
            _baseline.value = value
        }
        val merged = Achievements.merge(_achievements.value, bundle.achievements, System.currentTimeMillis())
        if (merged != _achievements.value) {
            persistList(achievementsFile, merged, AchievementRecord.serializer())
            _achievements.value = merged
        }
        // Like achievements, restoring an older backup must not forget explored branches.
        val progress = (_progress.value + bundle.progress).groupBy { it.storyId }
            .map { (id, records) -> StoryProgress(id, records.flatMapTo(mutableSetOf()) { it.visitedNodes }) }
        if (progress != _progress.value) {
            persistList(progressFile, progress, StoryProgress.serializer())
            _progress.value = progress
        }
        bundle.providers.size + bundle.characters.size + bundle.stories.size + bundle.saves.size
    }

    data class SharedImportResult(val added: Int, val existing: Int)

    /** 分享码导入：仅按 id 补入缺失的剧情与角色，不覆盖同名、不触碰用户已有数据。 */
    suspend fun importShared(bundle: AppBundle): SharedImportResult = write {
        val charIds = _characters.value.mapTo(mutableSetOf()) { it.id }
        val newChars = bundle.characters.filter { charIds.add(it.id) }
        val storyIds = _stories.value.mapTo(mutableSetOf()) { it.id }
        val newStories = bundle.stories.filter { storyIds.add(it.id) }
        // Shared content never changes the baseline.
        if (newChars.isNotEmpty() || newStories.isNotEmpty()) {
            val chars = _characters.value + newChars
            val stories = _stories.value + newStories
            persistList(charactersFile, chars, CharacterData.serializer())
            _characters.value = chars
            persistList(storiesFile, stories, Story.serializer())
            _stories.value = stories
        }
        val added = newChars.size + newStories.size
        SharedImportResult(added, bundle.characters.size + bundle.stories.size - added)
    }

    // ---------------- 内部工具 ----------------

    private fun <T> replaceById(list: List<T>, id: String, item: T): List<T> {
        val out = list.toMutableList()
        val idx = out.indexOfFirst {
            when (it) {
                is ApiProfile -> it.id == id
                is CharacterData -> it.id == id
                is Story -> it.id == id
                is SaveSlot -> it.id == id
                is StoryProgress -> it.storyId == id
                else -> false
            }
        }
        if (idx >= 0) out[idx] = item else out.add(item)
        return out
    }

    private fun <T> persistList(file: File, list: List<T>, serializer: kotlinx.serialization.KSerializer<T>) =
        atomicWrite(file) { AppJson.encodeToString(ListSerializer(serializer), list) }

    /** Encoding runs inside the guard so serialization failures are reported like disk failures. */
    private fun atomicWrite(file: File, encode: () -> String) {
        var pending: File? = null
        try {
            val text = encode()
            val temp = File.createTempFile(file.name, ".pending", dir)
            pending = temp
            FileOutputStream(temp).use {
                it.write(text.toByteArray(Charsets.UTF_8))
                it.fd.sync()
            }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            val message = "无法保存 ${file.name}：${e.message}"
            _writeError.value = message
            throw IOException(message, e)
        } finally {
            pending?.delete()
        }
    }

    private fun <T> readList(file: File, serializer: kotlinx.serialization.KSerializer<T>): List<T> {
        if (!file.exists()) return emptyList()
        return try {
            AppJson.decodeFromString(ListSerializer(serializer), file.readText())
        } catch (t: Throwable) {
            // 文件损坏时保留现场（.corrupt），从空列表继续，避免应用崩溃
            try { file.copyTo(File(file.parentFile, file.name + ".corrupt-" + System.currentTimeMillis()), true) } catch (_: Throwable) {}
            emptyList()
        }
    }
}

