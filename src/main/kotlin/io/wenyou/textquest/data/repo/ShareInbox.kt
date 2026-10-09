package io.wenyou.textquest.data.repo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.zip.CRC32

/**
 * One entry for shared content arriving from the clipboard, opened files, shared text, links or in-app import.
 * The app previews whatever is pending; codes already offered, or shared from this device, are not offered again
 * from the clipboard.
 */
class ShareInbox(context: Context) {
    private val prefs = context.getSharedPreferences("share_inbox", Context.MODE_PRIVATE)
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Clipboard change stamp already inspected, so each copied text is read once. */
    var lastClipStamp: Long
        get() = prefs.getLong(KEY_STAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_STAMP, value).apply()

    /**
     * Queues the first share code found in [text] for preview; returns false when there is none.
     * Clipboard offers stay silent for codes already handled and for content this app cannot import.
     */
    fun offer(text: String, fromClipboard: Boolean = false): Boolean {
        val code = ShareCode.extract(text) ?: return false
        if (fromClipboard && (key(code) in handled() || ShareCode.foreign(code))) return true
        markHandled(code)
        _pending.value = code
        return true
    }

    fun dismiss() {
        _pending.value = null
    }

    /** Remembers [code] so copying it later does not prompt an import on this device. */
    fun markHandled(code: String) {
        val keys = (handled() - key(code)) + key(code)
        prefs.edit().putString(KEY_HANDLED, keys.takeLast(MAX_HANDLED).joinToString(",")).apply()
    }

    private fun handled(): List<String> = prefs.getString(KEY_HANDLED, "").orEmpty().split(',').filter(String::isNotEmpty)

    private fun key(code: String) = CRC32().apply { update(code.toByteArray(Charsets.UTF_8)) }.value.toString(16)

    private companion object {
        const val KEY_STAMP = "clip_stamp"
        const val KEY_HANDLED = "handled"
        const val MAX_HANDLED = 30
    }
}
