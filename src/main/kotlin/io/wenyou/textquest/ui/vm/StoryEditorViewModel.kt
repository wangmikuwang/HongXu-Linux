package io.wenyou.textquest.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.AiStorySettings
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.ChoiceData
import io.wenyou.textquest.data.model.Cond
import io.wenyou.textquest.data.model.Effect
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.model.StoryNode
import io.wenyou.textquest.data.repo.LocalLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StoryEditorState(
    val story: Story? = null,
    val characters: List<CharacterData> = emptyList(),
    val selectedNodeId: String = "start",
    val isNew: Boolean = true,
    val message: String = ""
)

/** 剧情（节点图）可视化编辑器。storyId 为空表示新建。 */
class StoryEditorViewModel(
    private val storyId: String?,
    container: WenYouApp.AppContainer
) : ViewModel() {

    private val library: LocalLibrary = container.library

    private val _ui = MutableStateFlow(StoryEditorState(isNew = storyId == null))
    val ui: StateFlow<StoryEditorState> = _ui.asStateFlow()

    init {
        _ui.update {
            it.copy(characters = library.characters.value)
        }
        if (storyId == null) {
            _ui.update {
                it.copy(story = blankStory(), selectedNodeId = "start")
            }
        } else {
            val story = library.stories.value.firstOrNull { s -> s.id == storyId }
            if (story == null) {
                _ui.update { it.copy(message = "未找到该剧情") }
            } else {
                _ui.update {
                    it.copy(story = story, selectedNodeId = story.startNodeId, isNew = false)
                }
            }
        }
    }

    private var revisedCharacters = emptyList<CharacterData>()
    fun applyRevision(bundle: AppBundle) {
        val revised = bundle.stories.single()
        require(revised.id == story().id)
        val changed = bundle.characters.filter { c -> _ui.value.characters.firstOrNull { it.id == c.id } != c }
        revisedCharacters = (revisedCharacters + changed).associateBy { it.id }.values.toList()
        _ui.update { current -> current.copy(story = revised,
            characters = current.characters.map { old -> bundle.characters.firstOrNull { it.id == old.id } ?: old },
            selectedNodeId = if (current.selectedNodeId in revised.nodes) current.selectedNodeId else revised.startNodeId) }
    }

    // ---------------- 通用 ----------------

    private fun story(): Story = _ui.value.story ?: blankStory()

    private fun updateStory(transform: (Story) -> Story) {
        _ui.update { it.copy(story = transform(it.story ?: blankStory())) }
    }

    private fun current(): StoryNode? = _ui.value.story?.nodes?.get(_ui.value.selectedNodeId)

    private fun updateNode(nodeId: String, transform: (StoryNode) -> StoryNode) {
        updateStory { s -> s.copy(nodes = s.nodes + (nodeId to transform(s.nodes[nodeId] ?: blankNode(nodeId)))) }
    }

    fun select(nodeId: String) = _ui.update { it.copy(selectedNodeId = nodeId) }

    // ---------------- 基本信息 ----------------

    fun setTitle(v: String) = updateStory { it.copy(title = v) }
    fun setSubtitle(v: String) = updateStory { it.copy(subtitle = v) }
    fun setEmoji(v: String) = updateStory { it.copy(coverEmoji = v) }
    fun setColor(v: Int) = updateStory { it.copy(colorIndex = v) }
    fun setGenre(v: String) = updateStory { it.copy(genre = v) }
    fun setMode(v: StoryMode) = updateStory { it.copy(mode = v) }
    fun setCharacterIds(ids: List<String>) = updateStory { it.copy(characterIds = ids) }
    fun setStartNode(id: String) = updateStory { it.copy(startNodeId = id) }
    /** 内容分类标记：影响剧情库分类与内容开关过滤。 */
    fun setLgbt(v: Boolean) = updateStory { it.copy(lgbt = v) }
    fun setAdult(v: Boolean) = updateStory { it.copy(adult = v) }

    fun setWorld(v: String) = updateAi { it.copy(worldSummary = v) }
    fun setTone(v: String) = updateAi { it.copy(tone = v) }
    fun setDirectorExtra(v: String) = updateAi { it.copy(directorExtra = v) }
    fun setInitVar(name: String, value: Double) =
        updateStory { s -> s.copy(initialVariables = s.initialVariables + (name to value)) }
    fun renameInitVar(old: String, new: String) {
        if (old == new || old.isBlank() || new.isBlank()) return
        updateStory { s ->
            val vars = s.initialVariables.toMutableMap()
            val v = vars.remove(old) ?: return@updateStory s
            s.copy(initialVariables = vars + (new.trim() to v))
        }
    }
    fun removeInitVar(name: String) =
        updateStory { s -> s.copy(initialVariables = s.initialVariables - name) }
    fun toggleInitFlag(name: String) =
        updateStory { s ->
            s.copy(initialFlags = if (name in s.initialFlags) s.initialFlags - name else s.initialFlags + name)
        }

    private fun updateAi(t: (AiStorySettings) -> AiStorySettings) =
        updateStory { s -> s.copy(ai = t(s.ai)) }

    // ---------------- 节点 ----------------

    fun addNode(): String {
        val id = nextNodeId()
        updateStory { s ->
            val node = blankNode(id)
            s.copy(nodes = s.nodes + (id to node))
        }
        _ui.update { it.copy(selectedNodeId = id) }
        return id
    }


    fun removeNode(id: String) {
        if ((_ui.value.story?.nodes?.size ?: 0) <= 1) {
            _ui.update { it.copy(message = "至少保留一个节点") }
            return
        }
        updateStory { s ->
            val nodes = s.nodes - id
            val clean = nodes.mapValues { (_, n) ->
                n.copy(
                    choices = n.choices.map { c ->
                        if (c.next == id) c.copy(next = "@self") else c
                    },
                    endTarget = if (n.endTarget == id) "" else n.endTarget
                )
            }
            val newStart = if (s.startNodeId == id) clean.keys.first() else s.startNodeId
            s.copy(nodes = clean, startNodeId = newStart)
        }
        if (_ui.value.selectedNodeId == id) {
            _ui.update { it.copy(selectedNodeId = _ui.value.story!!.startNodeId) }
        }
    }

    fun setKind(v: NodeKind) = updateSelected { it.copy(kind = v) }
    fun setNodeTitle(v: String) = updateSelected { it.copy(title = v) }
    fun setNodeText(v: String) = updateSelected { it.copy(text = v) }
    fun setSpeaker(v: String) = updateSelected { it.copy(speakerId = v) }
    fun setPrompt(v: String) = updateSelected { it.copy(prompt = v) }
    fun setEndTarget(v: String) = updateSelected { it.copy(endTarget = v) }

    private fun updateSelected(transform: (StoryNode) -> StoryNode) {
        updateNode(_ui.value.selectedNodeId, transform)
    }

    // ---------------- 选项 / 条件 / 效果 ----------------

    fun addChoice() {
        updateSelected { n -> n.copy(choices = n.choices + ChoiceData(text = "新的选项")) }
    }

    fun removeChoice(index: Int) = updateSelected { n ->
        n.copy(choices = n.choices.filterIndexed { i, _ -> i != index })
    }

    fun updateChoice(index: Int, transform: (ChoiceData) -> ChoiceData) = updateSelected { n ->
        n.copy(choices = n.choices.mapIndexed { i, c -> if (i == index) transform(c) else c })
    }

    fun setChoiceConditions(index: Int, conds: List<Cond>) =
        updateChoice(index) { it.copy(conditions = conds) }

    fun setChoiceEffects(index: Int, effects: List<Effect>) =
        updateChoice(index) { it.copy(effects = effects) }

    fun setNodeEnterEffects(effects: List<Effect>) =
        updateSelected { it.copy(onEnter = effects) }

    // ---------------- 保存 ----------------

    fun save() {
        val s = story()
        val title = s.title.trim()
        if (title.isEmpty()) {
            _ui.update { it.copy(message = "请先填写剧情标题") }
            return
        }
        val nodes = s.nodes
        if (nodes.isEmpty()) {
            _ui.update { it.copy(message = "还没有任何剧情节点") }
            return
        }
        val start = if (s.startNodeId in nodes) s.startNodeId else nodes.keys.first()
        val clean = s.copy(id = s.id.ifBlank { UUID.randomUUID().toString() }, title = title, startNodeId = start)
        launchLibraryWrite {
            revisedCharacters.forEach { library.upsertCharacter(it) }
            library.upsertStory(clean)
            _ui.update {
                it.copy(story = clean, isNew = false, message = "已保存「${clean.title}」")
            }
        }
    }

    // ---------------- 内部 ----------------

    private fun nextNodeId(): String {
        val existing = _ui.value.story?.nodes?.keys ?: emptySet()
        var i = 2
        while (("node$i") in existing) i++
        return "node$i"
    }

    private fun blankStory(): Story = Story(
        id = "",
        title = "",
        subtitle = "",
        mode = StoryMode.SCRIPT,
        startNodeId = "start",
        nodes = mapOf("start" to blankNode("start"))
    )

    private fun blankNode(id: String): StoryNode = StoryNode(
        id = id,
        kind = NodeKind.NARRATION,
        title = "新节点",
        text = "（在此写下场景描述……支持 \${变量名} 插值。）",
        choices = emptyList()
    )
}
