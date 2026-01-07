package com.tinkrmux.devswitch

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AdbDeviceManager(private val adbPath: String) {

    private val adb: AndroidDebugBridge by lazy {
        AndroidDebugBridge.initIfNeeded(false)
        AndroidDebugBridge.createBridge(adbPath, false)
    }

    val devices: Flow<List<IDevice>> =
        callbackFlow {
            val listener = object : AndroidDebugBridge.IDeviceChangeListener {
                override fun deviceConnected(device: IDevice?) {
                    updateDevices()
                }

                override fun deviceDisconnected(device: IDevice?) {
                    updateDevices()
                }

                override fun deviceChanged(device: IDevice?, changeMask: Int) {
                    updateDevices()
                }

                fun updateDevices() {
                    trySend(adb.devices?.toList() ?: emptyList())
                }
            }

            // Register the listener
            AndroidDebugBridge.addDeviceChangeListener(listener)

            // Emit initial state
            trySend(adb.devices?.toList() ?: emptyList())

            // Remove the listener when flow is canceled
            awaitClose {
                AndroidDebugBridge.removeDeviceChangeListener(listener)
            }
        }.flowOn(Dispatchers.Default)
}

suspend fun IDevice.executeShellCommand(command: String): String =
    suspendCancellableCoroutine { continuation ->
        try {
            val outputBuffer = StringBuilder()
            val receiver = object : IShellOutputReceiver {
                override fun addOutput(data: ByteArray, offset: Int, length: Int) {
                    outputBuffer.append(String(data, offset, length))
                }

                override fun flush() {
                    if (!continuation.isCancelled) {
                        continuation.resume(outputBuffer.toString())
                    }
                }

                override fun isCancelled(): Boolean {
                    return continuation.isCancelled
                }
            }

            executeShellCommand(
                command,
                receiver,
                10,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }