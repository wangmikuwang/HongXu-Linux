package io.wenyou.textquest.data.repo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hidden developer mode: unlocked once (long-press the version), after which its tools switch on and off freely. */
class DevMode(context: Context) {
    data class State(val unlocked: Boolean = false, val directorChat: Boolean = false) {
        val directorChatOn get() = unlocked && directorChat
    }

    private val prefs = context.getSharedPreferences("dev_mode", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(State(prefs.getBoolean(KEY_UNLOCKED, false), prefs.getBoolean(KEY_DIRECTOR_CHAT, false)))
    val state: StateFlow<State> = _state.asStateFlow()

    /** Returns true only the first time, so the caller can announce it once. */
    fun unlock(): Boolean {
        if (_state.value.unlocked) return false
        prefs.edit().putBoolean(KEY_UNLOCKED, true).apply()
        _state.value = _state.value.copy(unlocked = true)
        return true
    }

    fun setDirectorChat(on: Boolean) {
        prefs.edit().putBoolean(KEY_DIRECTOR_CHAT, on).apply()
        _state.value = _state.value.copy(directorChat = on)
    }

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_DIRECTOR_CHAT = "director_chat"
    }
}
