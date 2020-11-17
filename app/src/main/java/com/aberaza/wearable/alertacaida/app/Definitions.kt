package com.aberaza.wearable.alertacaida.app

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.*

enum class Action {
    START,
    STOP
}

enum class ServiceState {
    STOPPING,
    STOPPED,
    STARTING,
    STARTED
}

enum class AccelServiceBroadcast(val type: String) {
    DATA_UPDATE("DATA_UPDATE"),
    STATUS_UPDATE("STATUS_UPDATE"),
    CRASH_DETECTED("CRASH_DETECTED")
}

enum class SessionState {
    IDLE,
    RECORDING,
    DETECTED,
    COMPUTING
}
enum class FallResult {
    IS_FALL,
    IS_NOT
}
enum class DetectModel {
    BOURKE,
    BERAZA
}
@Serializable
data class AccelSensorConfig(
    val version: Int,
    val bufferSize: Int,
    val averageThreshold: Float,
    val suddenThreshold: Float
)

@Serializable
data class Episode(
    val uid: String,
    val sensorResolution: Float,
    val sensorMaxRange: Float,
    val samplingFreq: Int,
    val startTime: Long,
    var duration: Long = 0,
    var sessionData: FloatArray,
    var isFall: Boolean? = null
)

private const val TOAST_ENABLED = true
fun toast(context: Context, message: String ="", length: Int = Toast.LENGTH_SHORT){
    Log.i("TOAST", message)
    if(TOAST_ENABLED) {
        Toast.makeText(context, message, length).show()
    }
}

val timeStamp
    get() = System.currentTimeMillis()

