package com.aberaza.wearable.alertacaida.app

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.*


object Model {
    const val IN_TIMESTEPS = 200
    const val OUT_TIMESTEPS = 50
    const val STRIDE = 200
    val Y_SHIFT: Int
        get() = OUT_TIMESTEPS/2
    const val SAMPLING_RATE = 25
    val SAMPLING_PERIOD: Int
        get() = 1000000 * 1/ SAMPLING_RATE
    const val IIR_LPF = 12.0f
    const val BOURKE_LIMIT = 1.5f
    const val RMS_LIMIT = 4.0f
}

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
    HYBRID
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

object ModelKeys {
    const val MODEL_PATH = "mobilenet_quant_v1_224.tflite"
    const val LABEL_PATH = "labels.txt"
    const val INTPUT_TIMESTEPS = 100
    const val OUTPUT_TIMESTEPS = 50
    const val STEP_DIMENSIONS = 1
    const val BATCH_SIZE = 1
}
