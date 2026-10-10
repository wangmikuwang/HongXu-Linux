package io.wenyou.textquest.ui.vm

import io.wenyou.textquest.data.model.autoSaveName
import io.wenyou.textquest.data.model.isAutoSaveName

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.ai.AiChoice
import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.ai.DirectorMessage
import io.wenyou.textquest.data.ai.AiScene
import io.wenyou.textquest.data.ai.withContinuity
import io.wenyou.textquest.data.ai.StateChange
import io.wenyou.textquest.data.engine.GameEngine
import io.wenyou.textquest.data.model.ApiProfile
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.CharacterMetrics
import io.wenyou.textquest.data.model.CharacterState
import io.wenyou.textquest.data.model.ChoiceData
import io.wenyou.textquest.data.model.EntryKind
import io.wenyou.textquest.data.model.LogEntry
import io.wenyou.textquest.data.model.ScenePace
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.SaveSlot
import io.wenyou.textquest.data.model.SessionState
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.model.StoryNode
import io.wenyou.textquest.data.repo.LocalLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class PlayStage { INIT, ROLE_SELECT, AUTHORED, DM_INPUT, AI_WORKING, STOPPED }

/** A recap being prepared for the next chapter; the player can edit it before starting. */
data class ChapterDraft(val summary: String = "", val busy: Boolean = true, val error: String = "")

data class PlayUi(
    val story: Story? = null,
    val session: SessionState? = null,
    val characters: List<CharacterData> = emptyList(),
    val stage: PlayStage = PlayStage.INIT,
    val nodeId: String = "",
    val nodeTitle: String = "",
    val visibleChoices: List<ChoiceData> = emptyList(),
    val pendingAiChoices: List<AiChoice> = emptyList(),
    val aiReasoningDelta: String = "",
    val aiTargetExit: Boolean = false,
    val stoppedTitle: String = "",
    val stoppedMessage: String = "",
    val aiMode: Boolean = false,
    val providerMissing: Boolean = false,
    val lastMessage: String = "",
    val activeSaveId: String? = null,
    val saveName: String = "",
    val providers: List<ApiProfile> = emptyList(),
    val selectedProviderId: String? = null,
    val achievementMessages: List<String> = emptyList(),
    val chapter: ChapterDraft? = null,
    val directorChat: List<DirectorMessage> = emptyList(),
    val directorChatBusy: Boolean = false,
    val directorChatError: String = ""
)

/**
 * 对局驱动状态机：把「分支引擎 + AI 场景 + AI 导演自由模式」统一成
 * ［开始 → 作者选项 / AI 选项 / 自由输入 → 结局/停止］的流转。
 */
class PlayViewModel internal constructor(
    private val storyId: String,
    private val saveId: String,
    private val library: LocalLibrary,
    private val director: AiDirector,
    private val defaultProviderId: () -> String?
) : ViewModel() {

    constructor(storyId: String, saveId: String, container: WenYouApp.AppContainer) : this(
        storyId, saveId, container.library, container.director, { container.settings.defaultProviderId }
    )

    private val _ui = MutableStateFlow(PlayUi())
    val ui: StateFlow<PlayUi> = _ui.asStateFlow()

    private var session: SessionState? = null
        set(value) {
            field = value
            _ui.update { it.copy(session = value) }
            val story = _ui.value.story
            if (story != null && value != null) library.trackAchievements(story, value) { unlocked ->
                if (unlocked.isNotEmpty()) _ui.update { it.copy(achievementMessages = it.achievementMessages + unlocked) }
            }
        }

    /** 当前 AI 生成任务。发起新请求前会取消旧任务，避免重试等场景产生并发覆盖。 */
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            val story = library.stories.value.firstOrNull { it.id == storyId }
            if (story == null) {
                _ui.update {
                    it.copy(stage = PlayStage.STOPPED, stoppedTitle = "剧情不见了",
                        stoppedMessage = "未找到该剧情（可能已被删除）。")
                }
                return@launch
            }
            val chars = library.characters.value.filter { it.id in story.characterIds }
            // “new” 是路由中开新局的哨兵值，不作为存档 id 加载
            val loadId = saveId.takeIf { it.isNotBlank() && it != "new" }
            val loadedSlot = loadId?.let { library.saves.value.firstOrNull { x -> x.id == it } }
            val saveName = loadedSlot?.name ?: ""
            val base: SessionState = loadedSlot?.state ?: GameEngine.newSession(story, chars)
            _ui.update {
                it.copy(story = story, characters = chars, aiMode = story.mode == StoryMode.AI_DIRECTOR,
                    activeSaveId = loadedSlot?.id, saveName = saveName,
                    providers = library.providers.value, selectedProviderId = null)
            }
            if (loadedSlot == null) prepareRoleSelection(base)
            else beginPlay(story, base, isFresh = false)
        }
    }

    // ---------------- 开局 / 读档 ----------------

    private fun prepareRoleSelection(base: SessionState) {
        session = null
        _ui.update { it.copy(stage = PlayStage.ROLE_SELECT, session = base, nodeId = "", nodeTitle = "") }
    }

    fun selectPlayerCharacter(characterId: String) {
        val ui = _ui.value
        if (ui.stage != PlayStage.ROLE_SELECT) return
        val story = ui.story ?: return
        val base = ui.session ?: return
        val character = ui.characters.firstOrNull { it.id == characterId }
        if (characterId.isNotBlank() && character == null) return
        beginPlay(story, base.copy(playerCharacterId = characterId, playerCharacterName = character?.name.orEmpty()), isFresh = true)
    }

    private fun playerEntry(text: String): LogEntry {
        val state = session
        return LogEntry(EntryKind.CHOICE, speaker = state?.playerCharacterName?.ifBlank { "你" } ?: "你",
            speakerId = state?.playerCharacterId.orEmpty(), text = text)
    }

    private fun beginPlay(story: Story, base: SessionState, isFresh: Boolean) {
        var s = base
        if (isFresh) {
            val arrival = GameEngine.arriveAt(story, base, story.startNodeId)
            s = arrival.state
            session = s
            logSystem(arrival.notes)
        } else {
            session = s
        }
        if (story.mode == StoryMode.AI_DIRECTOR) {
            if (isFresh) logNodeNarration(story.nodes[s.currentNodeId], s)
            _ui.update {
                it.copy(stage = PlayStage.DM_INPUT, nodeId = s.currentNodeId,
                    nodeTitle = story.nodes[s.currentNodeId]?.title.orEmpty(),
                    visibleChoices = emptyList(), pendingAiChoices = s.pendingAiChoices.map(::toAiChoice),
                    providerMissing = provider() == null)
            }
        } else {
            renderNode(logNarration = isFresh)
        }
    }

    private fun renderNode(logNarration: Boolean = true, autoVisited: Set<String> = emptySet()) {
        val ui = _ui.value
        val story = ui.story ?: return
        val s = session ?: return
        val nodeId = s.currentNodeId
        if (nodeId in autoVisited) {
            _ui.update { it.copy(stage = PlayStage.STOPPED, stoppedTitle = "剧情循环",
                stoppedMessage = "无选项节点的自动跳转形成了循环，请在剧情编辑器中修改出口。") }
            return
        }
        val node = story.nodes[nodeId]
        if (node == null) {
            _ui.update {
                it.copy(stage = PlayStage.STOPPED, stoppedTitle = "走神了",
                    stoppedMessage = "这个剧情节点不存在，故事戛然而止。")
            }
            return
        }
        if (node.kind != NodeKind.AI && (s.aiAwaitingChoice || s.pendingAiChoices.isNotEmpty())) {
            session = s.copy(pendingAiChoices = emptyList(), aiAwaitingChoice = false)
        }
        when (node.kind) {
            NodeKind.NARRATION -> {
                if (logNarration) logNodeNarration(node, s)
                val choices = GameEngine.visibleChoices(s, story, nodeId)
                _ui.update {
                    it.copy(stage = PlayStage.AUTHORED, nodeId = nodeId, nodeTitle = node.title,
                        visibleChoices = choices, pendingAiChoices = emptyList(),
                        providerMissing = provider() == null)
                }
                if (choices.isEmpty() && node.endTarget.isNotBlank() && node.endTarget != nodeId) {
                    advanceTo(node.endTarget, autoVisited + nodeId)
                } else if (choices.isEmpty()) {
                    _ui.update {
                        it.copy(stage = PlayStage.STOPPED, stoppedTitle = node.title.ifBlank { "未完待续" },
                            stoppedMessage = "作者还没给这个节点写后续选项。\n\n你可以在「剧情编辑」里补上分支，或重新开始。")
                    }
                }
            }
            NodeKind.ENDING -> {
                val rendered = GameEngine.renderTemplate(node.text, s.variables)
                if (logNarration) appendEntries(listOf(LogEntry(EntryKind.NARRATION, text = rendered)))
                _ui.update {
                    it.copy(stage = PlayStage.STOPPED, nodeId = nodeId, nodeTitle = node.title,
                        visibleChoices = emptyList(), pendingAiChoices = emptyList(), aiTargetExit = false,
                        stoppedTitle = node.title.ifBlank { "结局" },
                        stoppedMessage = "你抵达了这个故事的结局。")
                }
            }
            NodeKind.AI -> {
                if (!logNarration && s.aiAwaitingChoice) {
                    _ui.update {
                        it.copy(stage = PlayStage.AUTHORED, nodeId = nodeId, nodeTitle = node.title,
                            visibleChoices = emptyList(), pendingAiChoices = s.pendingAiChoices.map(::toAiChoice),
                            aiTargetExit = node.endTarget.isNotBlank(),
                            providerMissing = provider() == null)
                    }
                    return
                }
                _ui.update {
                    it.copy(stage = PlayStage.AI_WORKING, nodeId = nodeId, nodeTitle = node.title,
                        visibleChoices = emptyList(), pendingAiChoices = emptyList(),
                        aiTargetExit = node.endTarget.isNotBlank(),
                        providerMissing = provider() == null)
                }
                runAiScene()
            }
        }
    }

    private fun logNodeNarration(node: StoryNode?, s: SessionState) {
        if (node == null || node.text.isBlank()) return
        val rendered = GameEngine.renderTemplate(node.text, s.variables)
        if (node.speakerId.isNotBlank()) {
            val speaker = _ui.value.characters.firstOrNull { it.id == node.speakerId }?.name ?: "角色"
            appendEntries(listOf(LogEntry(EntryKind.CHARACTER, speaker = speaker, speakerId = node.speakerId, text = rendered)))
        } else {
            appendEntries(listOf(LogEntry(EntryKind.NARRATION, text = rendered)))
        }
    }

    private fun advanceTo(target: String, autoVisited: Set<String> = emptySet()) {
        val story = _ui.value.story ?: return
        val s = (session ?: return).copy(pendingAiChoices = emptyList(), aiAwaitingChoice = false)
        val arrival = GameEngine.arriveAt(story, s, target)
        session = arrival.state
        logSystem(arrival.notes)
        renderNode(autoVisited = autoVisited)
    }

    // ---------------- 玩家动作 ----------------

    fun chooseAuthored(index: Int) {
        if (_ui.value.stage != PlayStage.AUTHORED) return
        val ui = _ui.value
        val story = ui.story ?: return
        val choices = ui.visibleChoices
        if (index !in choices.indices) return
        chooseBy(story, choices[index], ui.nodeId)
    }

    private fun chooseBy(story: Story, choice: ChoiceData, fromNodeId: String) {
        if (session == null) return
        appendEntries(listOf(playerEntry(choice.text)))
        val s = session ?: return
        val outcome = GameEngine.applyEffects(s, choice.effects)
        session = outcome.state
        logSystem(outcome.notes)
        val node = story.nodes[fromNodeId]
        val target = choice.next.ifBlank { "@self" }
        val self = target == "@self" || target == fromNodeId
        if (!self) {
            val arrival = GameEngine.arriveAt(story, session ?: return, target)
            session = arrival.state
            logSystem(arrival.notes)
            renderNode()
        } else if (node?.kind == NodeKind.AI) {
            runAiScene()
        } else {
            // 作者把选项指回本节点：留在原地，重新展示本节点选项（不重复正文）
            _ui.update {
                it.copy(stage = PlayStage.AUTHORED, visibleChoices = GameEngine.visibleChoices(outcome.state, story, fromNodeId),
                    pendingAiChoices = emptyList())
            }
        }
    }

    fun chooseAi(index: Int) {
        if (_ui.value.stage != PlayStage.AUTHORED) return
        val ui = _ui.value
        val story = ui.story ?: return
        val choice = ui.pendingAiChoices.getOrNull(index) ?: return
        appendEntries(listOf(playerEntry(choice.text)))
        session = session?.copy(pendingAiChoices = emptyList(), aiAwaitingChoice = false)
        val next = choice.next.ifBlank { "@self" }
        if (next != "@self" && story.nodes.containsKey(next)) {
            _ui.update { it.copy(pendingAiChoices = emptyList()) }
            advanceTo(next)
        } else {
            runAiScene()
        }
    }

    /** AI 场景结束该段，前往作者设定的主线出口。 */
    fun aiExitToMainline() {
        val story = _ui.value.story ?: return
        val node = story.nodes[_ui.value.nodeId] ?: return
        val target = node.endTarget
        if (target.isBlank() || target == node.id || !story.nodes.containsKey(target)) return
        _ui.update { it.copy(pendingAiChoices = emptyList()) }
        advanceTo(target)
    }

    // ---------------- AI 场景 / 导演 ----------------

    private fun runAiScene() {
        val ui = _ui.value
        val story = ui.story ?: return
        val s = session ?: return
        val node = story.nodes[ui.nodeId] ?: return
        val profile = provider()
        if (profile == null) {
            appendEntries(listOf(LogEntry(EntryKind.ERROR, speaker = "系统",
                text = "这个场景需要 AI 生成，但还没有可用的 AI 服务。")))
            _ui.update {
                it.copy(stage = PlayStage.STOPPED, stoppedTitle = "缺少 AI 服务", providerMissing = true,
                    stoppedMessage = "添加任意一家 AI 服务后点「重试」继续（推荐 DeepSeek，也支持 OpenAI 兼容服务、Claude、Gemini 和本地 Ollama）。")
            }
            return
        }
        // 已有生成任务在运行时，不再发起新的并发请求（覆盖快速连点等场景）
        if (aiJob?.isActive == true) return
        turnBeforeAi = TurnBeforeAi(s, emptyList(), directorTurn = false)
        session = s.copy(pendingAiChoices = emptyList(), aiAwaitingChoice = false)
        _ui.update { it.copy(stage = PlayStage.AI_WORKING, aiReasoningDelta = "", pendingAiChoices = emptyList(), providerMissing = false) }
        launchAiJob { job ->
            try {
                val reasoning = ReasoningStream()
                val scene = director.generateScene(profile, story, node, ui.characters, s,
                    adult = story.adult,
                    onReasoning = reasoning::append,
                    onDelta = { reasoning.flush() })
                if (job.isActive && aiJob === job) {
                    aiJob = null
                    finishAiScene(scene)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (job.isActive) aiFailed(t)
            }
        }
    }

    /** Streams reasoning at most ~20 times per second instead of copying the whole text into UI state per token. */
    private inner class ReasoningStream {
        private val text = StringBuilder()
        private var dirty = false
        private var publishedAt = 0L

        fun append(chunk: String) {
            text.append(chunk)
            dirty = true
            if (System.nanoTime() - publishedAt >= 50_000_000L) flush()
        }

        fun flush() {
            if (!dirty) return
            dirty = false
            publishedAt = System.nanoTime()
            val snapshot = text.toString()
            _ui.update { it.copy(aiReasoningDelta = snapshot) }
        }
    }

    /** 在 [viewModelScope] 中串行执行 AI 请求：取消旧的、记录当前任务。 */
    private fun launchAiJob(block: suspend (Job) -> Unit) {
        aiJob?.cancel()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext[Job] ?: return@launch
            try {
                block(self)
            } finally {
                if (aiJob === self) aiJob = null
            }
        }
        aiJob = job
        job.start()
    }

    /** 应用 AI 建议的角色状态变化，并打印「✨ 状态变化」日志。 */
    private fun applyStateChanges(changes: List<StateChange>) {
        if (changes.isEmpty()) return
        val s = session ?: return
        val ui = _ui.value
        var charStates = s.characterStates
        val parts = mutableListOf<String>()
        for (c in changes) {
            val char = ui.characters.firstOrNull { it.id == c.char } ?: continue
            val st = charStates[c.char] ?: CharacterState()
            var metrics = st.metrics
            var flags = st.flags
            var desc = st.description
            if (CharacterMetrics.byKey(c.metric) != null && c.delta.isFinite() && c.delta != 0.0) {
                val ov = metrics[c.metric] ?: 0.0
                val nv = CharacterMetrics.clamp(ov + c.delta)
                if (nv != ov) {
                    metrics = metrics + (c.metric to nv)
                    val sign = if (c.delta > 0) "+" else ""
                    parts += "${CharacterMetrics.icon(c.metric)}${char.name} ${CharacterMetrics.label(c.metric)}$sign${GameEngine.formatNumber(nv - ov)}"
                }
            }
            if (c.flag.isNotBlank() && flags.size < 100) flags = flags + c.flag.take(120)
            if (c.desc.isNotBlank()) desc = c.desc.take(1000)
            if (metrics != st.metrics || flags != st.flags || desc != st.description) {
                charStates = charStates + (c.char to st.copy(metrics = metrics, flags = flags, description = desc, lastChangeReason = c.reason.trim().take(240)))
            }
        }
        if (parts.isNotEmpty() || charStates != s.characterStates) {
            session = s.copy(characterStates = charStates, updatedAt = System.currentTimeMillis())
            if (parts.isNotEmpty()) {
                appendEntries(listOf(LogEntry(EntryKind.SYSTEM, speaker = "系统", text = "✨ 状态变化：${parts.joinToString("　")}")))
            }
        }
    }

    private fun finishAiScene(scene: AiScene) {
        val ui = _ui.value
        val story = ui.story ?: return
        val node = story.nodes[ui.nodeId]
        countAiTurn()
        session = session?.let { scene.withContinuity(it, ui.characters.map { c -> c.id }.toSet()) }
        applyStateChanges(scene.stateEffects)
        appendEntries(scene.logEntries(ui.characters, node?.speakerId.orEmpty(), session?.playerCharacterId.orEmpty()))
        val choices = scene.choices
        // 只有指向真实存在的其它节点才算有效出口，避免死循环 / 跳到不存在的剧情
        val exit = node?.endTarget?.takeIf { it.isNotBlank() && it != node.id && story.nodes.containsKey(it) }
        if (choices.isEmpty()) {
            session = session?.copy(pendingAiChoices = emptyList(), aiAwaitingChoice = exit == null)
            _ui.update { it.copy(stage = PlayStage.AUTHORED, aiReasoningDelta = "", pendingAiChoices = emptyList(), visibleChoices = emptyList()) }
            if (exit != null) {
                advanceTo(exit)
            } else {
                _ui.update { it.copy(lastMessage = "AI 没有给出选项——你可以点「继续」让故事延伸。") }
            }
        } else {
            session = session?.copy(pendingAiChoices = choices.map(::toChoiceData), aiAwaitingChoice = true)
            _ui.update { it.copy(stage = PlayStage.AUTHORED, aiReasoningDelta = "", pendingAiChoices = choices, visibleChoices = emptyList()) }
        }
    }

    fun continueAi() = runAiScene()

    fun retryAi() = runAiScene()

    private fun aiFailed(t: Throwable) {
        val message = AiDirector.errorMessage(t)
        appendEntries(listOf(LogEntry(EntryKind.ERROR, speaker = "系统", text = "AI 生成失败：$message")))
        _ui.update {
            it.copy(stage = PlayStage.STOPPED, stoppedTitle = "AI 生成失败",
                stoppedMessage = "$message\n\n你可以点「重试」，或（若设了主线出口）「回到主线」。")
        }
    }

    /** AI 导演自由对话。 */
    fun dmSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val ui = _ui.value
        val story = ui.story ?: return
        if (story.mode != StoryMode.AI_DIRECTOR) return
        val s = session ?: return
        val profile = provider()
        if (profile == null) {
            appendEntries(listOf(LogEntry(EntryKind.ERROR, speaker = "系统",
                text = "AI 导演模式需要先接入一家 AI 服务。")))
            _ui.update { it.copy(providerMissing = true) }
            return
        }
        if (ui.stage != PlayStage.DM_INPUT || aiJob?.isActive == true) return
        turnBeforeAi = TurnBeforeAi(s, ui.pendingAiChoices, directorTurn = true)
        appendEntries(listOf(playerEntry(trimmed)))
        session = session?.copy(pendingAiChoices = emptyList(), aiAwaitingChoice = false)
        _ui.update { it.copy(stage = PlayStage.AI_WORKING, aiReasoningDelta = "", pendingAiChoices = emptyList(), providerMissing = false) }
        launchAiJob { job ->
            try {
                val reasoning = ReasoningStream()
                val scene = director.directorTurn(profile, story, ui.characters, s, trimmed,
                    adult = story.adult,
                    onReasoning = reasoning::append,
                    onDelta = { reasoning.flush() })
                if (job.isActive && aiJob === job) {
                    countAiTurn()
                    appendEntries(scene.logEntries(ui.characters, playerId = session?.playerCharacterId.orEmpty()))
                    session = session?.let { scene.withContinuity(it, ui.characters.map { c -> c.id }.toSet()) }
                    applyStateChanges(scene.stateEffects)
                    session = session?.copy(pendingAiChoices = scene.choices.map(::toChoiceData), aiAwaitingChoice = true)
                    _ui.update { it.copy(aiReasoningDelta = "", stage = PlayStage.DM_INPUT, pendingAiChoices = scene.choices) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (job.isActive) {
                    // Restore the unconsumed turn so retrying a suggestion never duplicates player input.
                    session = s
                    appendEntries(listOf(LogEntry(EntryKind.ERROR, speaker = "系统", text = "AI 导演出错：${AiDirector.errorMessage(t)}")))
                    _ui.update { it.copy(aiReasoningDelta = "", stage = PlayStage.DM_INPUT, pendingAiChoices = ui.pendingAiChoices) }
                }
            }
        }
    }

    private data class TurnBeforeAi(val session: SessionState, val choices: List<AiChoice>, val directorTurn: Boolean)
    private var turnBeforeAi: TurnBeforeAi? = null

    /**
     * Stops the AI request in progress. A director turn is put back exactly as it was, input and suggestions
     * included, so it can be resent or changed; an AI scene stops with a retry.
     */
    fun stopAi() {
        val job = aiJob ?: return
        if (_ui.value.stage != PlayStage.AI_WORKING) return
        aiJob = null
        job.cancel()
        val before = turnBeforeAi
        if (before != null && before.directorTurn) {
            session = before.session
            _ui.update { it.copy(stage = PlayStage.DM_INPUT, aiReasoningDelta = "", pendingAiChoices = before.choices, lastMessage = "已停止生成") }
        } else {
            before?.let { session = it.session }
            _ui.update { it.copy(stage = PlayStage.STOPPED, aiReasoningDelta = "", stoppedTitle = "已停止生成",
                stoppedMessage = "这一幕还没有写完。点「重试」重新生成，或返回稍后再来。") }
        }
    }

    // ---------------- 存档 / 重开 ----------------

    fun saveNow() {
        val s = session ?: return
        val ui = _ui.value
        val story = ui.story ?: return
        launchLibraryWrite {
            val now = System.currentTimeMillis()
            // Auto names track the current step count; names the player chose are kept.
            val name = ui.saveName.takeUnless(::isAutoSaveName) ?: autoSaveName(story.title, s.history.size)
            val existing = ui.activeSaveId
            val id = existing ?: UUID.randomUUID().toString()
            val createdAt = existing?.let { library.saves.value.firstOrNull { x -> x.id == it }?.createdAt } ?: now
            library.upsertSave(SaveSlot(id, name, createdAt, now, s))
            _ui.update { it.copy(activeSaveId = id, saveName = name, lastMessage = "已存档「$name」") }
        }
    }

    fun setPace(pace: ScenePace) {
        session = session?.copy(pace = pace.name)
    }

    /** Asks the AI for a recap of the journey so far; shown for review before a new chapter starts. */
    fun draftChapter() {
        val ui = _ui.value
        val story = ui.story ?: return
        val s = session ?: return
        val profile = provider()
        if (profile == null) {
            _ui.update { it.copy(chapter = ChapterDraft(busy = false, error = "需要先接入一家 AI 服务才能生成总结。"), providerMissing = true) }
            return
        }
        _ui.update { it.copy(chapter = ChapterDraft()) }
        viewModelScope.launch {
            val result = runCatching { director.summarize(profile, story, ui.characters, s) }
            _ui.update { cur ->
                val draft = cur.chapter ?: return@update cur
                cur.copy(chapter = result.fold({ draft.copy(summary = it, busy = false, error = "") },
                    { draft.copy(busy = false, error = AiDirector.errorMessage(it)) }))
            }
        }
    }

    fun dismissChapter() = _ui.update { it.copy(chapter = null) }

    /** Developer mode: an out-of-story question or request to the director; requests become memos in the save. */
    fun sendDirectorChat(text: String) {
        val message = text.trim()
        val ui = _ui.value
        val story = ui.story ?: return
        val s = session ?: return
        if (message.isEmpty() || ui.directorChatBusy) return
        val profile = provider()
        if (profile == null) {
            _ui.update { it.copy(directorChatError = "需要先接入一家 AI 服务。", providerMissing = true) }
            return
        }
        val history = ui.directorChat
        _ui.update { it.copy(directorChat = history + DirectorMessage(true, message), directorChatBusy = true, directorChatError = "") }
        viewModelScope.launch {
            val result = runCatching { director.directorChat(profile, story, ui.characters, s, history, message) }
            result.onSuccess { reply ->
                if (reply.note.isNotBlank()) session = session?.let { it.copy(directorNotes = (it.directorNotes + reply.note).takeLast(MAX_DIRECTOR_NOTES)) }
            }
            _ui.update { cur -> result.fold(
                { cur.copy(directorChat = cur.directorChat + it, directorChatBusy = false) },
                { cur.copy(directorChatBusy = false, directorChatError = AiDirector.errorMessage(it)) }) }
        }
    }

    /** The player sets a character's state by hand from the console; following AI turns read it like any other state. */
    fun setCharacterState(charId: String, state: CharacterState) {
        val s = session ?: return
        if (_ui.value.stage == PlayStage.AI_WORKING || _ui.value.characters.none { it.id == charId }) return
        val clean = state.copy(
            metrics = state.metrics.filterKeys { CharacterMetrics.byKey(it) != null }.mapValues { CharacterMetrics.clamp(it.value) },
            flags = state.flags.map { it.trim().take(120) }.filter { it.isNotEmpty() }.take(100).toSet(),
            description = state.description.trim().take(1000))
        session = s.copy(characterStates = s.characterStates + (charId to clean), updatedAt = System.currentTimeMillis())
    }

    fun removeDirectorNote(index: Int) {
        session = session?.let { s -> s.copy(directorNotes = s.directorNotes.filterIndexed { i, _ -> i != index }) }
    }

    /**
     * Keeps the finished chapter in its save, then continues in a new save that starts from [summary]:
     * character states, flags and variables carry over, the long history does not.
     */
    fun startChapter(summary: String) {
        val recap = summary.trim().take(AiDirector.RECAP_LIMIT)
        val old = session ?: return
        val ui = _ui.value
        val story = ui.story ?: return
        if (recap.isBlank() || !ui.aiMode) return
        aiJob?.cancel()
        aiJob = null
        val now = System.currentTimeMillis()
        val next = old.copy(history = listOf(LogEntry(EntryKind.NARRATION, text = "【前情提要】\n$recap", ts = now)),
            memory = "", recap = recap, pendingAiChoices = emptyList(), aiAwaitingChoice = false, updatedAt = now)
        val oldId = ui.activeSaveId ?: UUID.randomUUID().toString()
        val oldName = ui.saveName.takeUnless(::isAutoSaveName) ?: autoSaveName(story.title, old.history.size)
        val oldCreated = library.saves.value.firstOrNull { it.id == oldId }?.createdAt ?: now
        val newId = UUID.randomUUID().toString()
        val newName = "${story.title} · 新篇章"
        session = next
        _ui.update { it.copy(chapter = null, activeSaveId = newId, saveName = newName, pendingAiChoices = emptyList(),
            stage = PlayStage.DM_INPUT, stoppedTitle = "", stoppedMessage = "") }
        // One coroutine so the finished chapter is written before the new one.
        launchLibraryWrite {
            library.upsertSave(SaveSlot(oldId, oldName, oldCreated, now, old))
            library.upsertSave(SaveSlot(newId, newName, now, now, next))
            _ui.update { it.copy(lastMessage = "已开启新篇章；上一篇章已存档为「$oldName」") }
        }
    }

    fun restart() {
        val story = _ui.value.story ?: return
        // 若仍有未结束的生成任务，先取消，避免旧结果写入新开局
        aiJob?.cancel()
        aiJob = null
        val fresh = GameEngine.newSession(story, _ui.value.characters)
        _ui.update {
            it.copy(activeSaveId = null, saveName = "", lastMessage = "",
                pendingAiChoices = emptyList(), aiReasoningDelta = "", stoppedTitle = "", stoppedMessage = "")
        }
        prepareRoleSelection(fresh)
    }

    // ---------------- 内部 ----------------

    private fun provider(): ApiProfile? {
        val list = library.providers.value
        if (list.isEmpty()) return null
        val overridden = _ui.value.selectedProviderId
        val def = when {
            overridden != null -> list.firstOrNull { it.id == overridden }
            else -> list.firstOrNull { it.id == defaultProviderId() }
        }
        return def ?: list.first()
    }

    /** 对局内临时切换使用的 AI 服务（null = 跟随默认/第一个可用）。 */
    fun selectProvider(id: String?) {
        _ui.update { it.copy(selectedProviderId = id) }
    }

    fun consumeAchievementMessage() {
        _ui.update { it.copy(achievementMessages = it.achievementMessages.drop(1)) }
    }

    private fun logSystem(notes: List<String>) {
        if (notes.isEmpty()) return
        appendEntries(notes.map { LogEntry(EntryKind.SYSTEM, speaker = "系统", text = it) })
    }

    private fun appendEntries(entries: List<LogEntry>) {
        val s = session ?: return
        val now = System.currentTimeMillis()
        val stamped = entries.map { if (it.ts == 0L) it.copy(ts = now) else it }
        val choices = maxOf(s.choicesTaken, s.history.count { it.kind == EntryKind.CHOICE })
        session = s.copy(history = (s.history + stamped).takeLast(600), updatedAt = now,
            choicesTaken = (choices.coerceAtLeast(0).toLong() + entries.count { it.kind == EntryKind.CHOICE }).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    private fun countAiTurn() {
        session = session?.let { it.copy(aiTurns = if (it.aiTurns >= Int.MAX_VALUE) Int.MAX_VALUE else it.aiTurns.coerceAtLeast(0) + 1) }
    }

    private fun toChoiceData(choice: AiChoice) = ChoiceData(choice.text, choice.next)
    private fun toAiChoice(choice: ChoiceData) = AiChoice(choice.text, choice.next)

    private companion object {
        const val MAX_DIRECTOR_NOTES = 10
    }
}
