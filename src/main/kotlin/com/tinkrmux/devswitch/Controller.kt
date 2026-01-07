@file:Suppress("UNCHECKED_CAST")

package com.tinkrmux.devswitch

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import com.android.ddmlib.IDevice
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

val LocalController = compositionLocalOf<Controller> {
    error("No controller provided")
}

class Controller : CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("exception occurred: $throwable")
        autoRefreshEnabled = false
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + exceptionHandler

    val actions = mutableMapOf<String, SettingsAction<*>>()
    val settingsState = mutableMapOf<String, MutableState<*>>()

    var selectedDevice by mutableStateOf<IDevice?>(null)

    // auto refresh
    var autoRefreshEnabled by mutableStateOf(true)
    private var autoRefreshJob: Job? = null

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = launch {
            while (true) {
                refreshAll()
                delay(1000)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }

    private fun refreshAll() {
        Snapshot.withMutableSnapshot {
            actions.onEach { (key, action) -> refreshState(key, action) }
        }
    }

    private fun refreshState(key: String, action: SettingsAction<*>) =
        launch {
            when (action) {
                is ToggleAction -> {
                    (settingsState[key] as? MutableState<Boolean>)?.run {
                        selectedDevice?.let {
                            value = action.getValue(it)
                        }
                    }
                }

                is RangeAction -> {
                    (settingsState[key] as? MutableState<String>)?.run {
                        selectedDevice?.let {
                            value = action.getValue(it)
                        }
                    }
                }
            }
        }
}
