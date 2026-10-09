package io.wenyou.textquest

import android.content.Context
import java.io.File

/** Crash log: always crash.log in the data folder, and also in the folder chosen in settings, if any. */
object CrashLog {
    fun write(context: Context, text: String, dir: String?) {
        runCatching { File(context.filesDir, "crash.log").writeText(text) }
        if (!dir.isNullOrBlank()) runCatching { File(dir, "crash.log").writeText(text) }
    }
}
