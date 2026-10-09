package io.wenyou.textquest.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.wenyou.textquest.WenYouApp
import io.wenyou.textquest.data.ai.AiCreator
import io.wenyou.textquest.data.ai.AiDirector
import io.wenyou.textquest.data.ai.CreationKind
import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.StoryMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreationUi(
    val idea: String = "",
    val revision: String = "",
    val kind: CreationKind = CreationKind.STORY,
    val mode: StoryMode = StoryMode.AI_DIRECTOR,
    val busy: Boolean = false,
    val saving: Boolean = false,
    val draft: AppBundle? = null,
    val error: String = "",
    val saved: Boolean = false
)

class CreationViewModel(private val container: WenYouApp.AppContainer) : ViewModel() {
    private val creator = AiCreator(container.chatClient)
    private val state = MutableStateFlow(CreationUi())
    val ui = state.asStateFlow()
    private var generation: Job? = null

    fun setIdea(value: String) {
        if (!ui.value.busy) state.update { it.copy(idea = value.take(2000), draft = null, error = "", saved = false) }
    }
    fun setKind(value: CreationKind) {
        if (!ui.value.busy) state.update { it.copy(kind = value, draft = null, error = "", saved = false) }
    }
    fun setMode(value: StoryMode) {
        if (!ui.value.busy) state.update { it.copy(mode = value, draft = null, error = "", saved = false) }
    }
    fun setRevision(value: String) { if (!ui.value.busy) state.update { it.copy(revision = value.take(2000), error = "") } }
    fun revise() {
        val current = ui.value
        val draft = current.draft ?: return
        if (current.busy || current.revision.isBlank()) return
        val profiles = container.library.providers.value
        val profile = profiles.firstOrNull { it.id == container.settings.state.value.defaultProviderId } ?: profiles.firstOrNull()
        if (profile == null) { state.update { it.copy(error = "请先配置 AI 服务") }; return }
        state.update { it.copy(busy = true, error = "") }
        generation = viewModelScope.launch {
            try {
                val revised = creator.revise(profile, current.revision, draft, container.settings.state.value.adultContent)
                state.update { it.copy(draft = revised, revision = "") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.update { it.copy(error = AiDirector.errorMessage(e)) } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    fun generate() {
        val current = ui.value
        if (current.busy) return
        val profiles = container.library.providers.value
        val prefs = container.settings.state.value
        val profile = profiles.firstOrNull { it.id == prefs.defaultProviderId } ?: profiles.firstOrNull()
        if (profile == null || current.idea.isBlank()) {
            state.update { it.copy(error = if (profile == null) "请先配置 AI 服务" else "请填写一句创意描述") }
            return
        }
        state.update { it.copy(busy = true, error = "", draft = null, saved = false) }
        generation = viewModelScope.launch {
            try {
                val draft = creator.generate(profile, current.idea, current.kind, prefs.adultContent, current.mode)
                state.update { it.copy(draft = draft) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(error = AiDirector.errorMessage(e)) }
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }
    fun cancel() {
        generation?.cancel()
    }
    fun save() {
        val current = ui.value
        val draft = current.draft ?: return
        if (current.busy || current.saved) return
        state.update { it.copy(busy = true, saving = true, error = "") }
        viewModelScope.launch {
            try {
                // Reuse IDs on retry so a partial disk failure cannot duplicate generated characters.
                container.library.importShared(draft)
                state.update { it.copy(saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(error = "保存失败：${e.message}") }
            } finally {
                state.update { it.copy(busy = false, saving = false) }
            }
        }
    }
    fun consumeSaved() { state.value = CreationUi() }
}
