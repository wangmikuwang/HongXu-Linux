package io.wenyou.textquest.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import java.io.IOException

/** 写盘错误已由 LocalLibrary 发布给根界面；终止本次操作，不执行后续成功提示。 */
internal fun ViewModel.launchLibraryWrite(block: suspend () -> Unit) = viewModelScope.launch {
    try { block() } catch (_: IOException) { /* 根界面统一显示写盘错误。 */ }
}

/** Builds a screen's [ViewModel] from the container and arguments the screen already holds. */
object Vms {
    inline fun <reified VM : ViewModel> factory(noinline build: () -> VM) = viewModelFactory { initializer { build() } }
}
