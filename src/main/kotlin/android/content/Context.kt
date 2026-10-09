package android.content

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

/**
 * Desktop stand-in for the two Android types the shared data layer uses (file locations and key-value prefs),
 * so LocalLibrary, SettingsStore, DevMode and ShareInbox compile unchanged. Only this and android.os.SystemClock are stood in.
 */
open class Context(val filesDir: File) {
    val cacheDir: File get() = File(filesDir, "cache").apply { mkdirs() }
    private val prefs = mutableMapOf<String, SharedPreferences>()

    @Synchronized
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        prefs.getOrPut(name) { FilePreferences(File(filesDir, "prefs/$name.properties")) }

    companion object {
        const val MODE_PRIVATE = 0
    }
}

interface SharedPreferences {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getString(key: String, default: String?): String?
    fun getStringSet(key: String, default: Set<String>?): Set<String>?
    fun edit(): Editor

    interface Editor {
        fun putBoolean(key: String, value: Boolean): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putString(key: String, value: String?): Editor
        fun putStringSet(key: String, value: Set<String>?): Editor
        fun remove(key: String): Editor
        fun apply()
        fun commit(): Boolean
    }
}

/** Properties file written atomically on every apply; string sets are stored as JSON arrays. */
private class FilePreferences(private val file: File) : SharedPreferences {
    private val values = Properties().apply { if (file.isFile) file.inputStream().use(::load) }

    override fun getBoolean(key: String, default: Boolean) = values.getProperty(key)?.toBooleanStrictOrNull() ?: default
    override fun getInt(key: String, default: Int) = values.getProperty(key)?.toIntOrNull() ?: default
    override fun getLong(key: String, default: Long) = values.getProperty(key)?.toLongOrNull() ?: default
    override fun getString(key: String, default: String?): String? = values.getProperty(key) ?: default
    override fun getStringSet(key: String, default: Set<String>?): Set<String>? =
        values.getProperty(key)?.let { runCatching { Json.decodeFromString<List<String>>(it).toSet() }.getOrNull() } ?: default

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, String?>()
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value.toString() }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value.toString() }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value.toString() }
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, value: Set<String>?) = apply { changes[key] = value?.let { Json.encodeToString(it.toList()) } }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun apply() { commit() }
        override fun commit(): Boolean = synchronized(values) {
            changes.forEach { (k, v) -> if (v == null) values.remove(k) else values.setProperty(k, v) }
            runCatching {
                file.parentFile.mkdirs()
                val tmp = File(file.path + ".tmp")
                tmp.outputStream().use { values.store(it, null) }
                java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            }.isSuccess
        }
    }
}
