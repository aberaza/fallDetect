package com.aberaza.wearable.alertacaida.app


import android.content.Intent
import android.content.Context
import android.os.Bundle

import android.os.ResultReceiver
import android.util.Log
import androidx.core.app.JobIntentService
import kotlin.math.PI

// actions that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
const val ACTION_CRASH_DETECT = "com.aberaza.wearable.alertacaida.app.action.CRASH"

// parameters
const val RECEIVER_PARAM = "com.aberaza.wearable.alertacaida.app.extra.RECEIVER"
const val PREFALL_PARAM = "com.aberaza.wearable.alertacaida.app.extra.PREFALL"
const val POSTFALL_PARAM = "com.aberaza.wearable.alertacaida.app.extra.POSTFALL"
const val SID_PARAM = "com.aberaza.wearable.alertacaida.app.extra.SID"

const val SID_KEY = "SID_key"
const val IS_FALL_KEY = "isFall_key"
const val MESSAGE_KEY = "message_key"

const val SUCCESS_CODE = 1
const val FAILURE_CODE = -1

/**
 * An  subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * helper methods.
 */
class CrashDetectService() : JobIntentService() {
    val _tag = this::class.java.simpleName
    protected var resultReceiver: ResultReceiver? = null
    var model: FallDetector = Bourke()

    constructor(detectionModel: DetectModel? = DetectModel.BOURKE): this() {
        when(detectionModel){
            DetectModel.BOURKE -> model = Bourke()
            else -> model = Bourke()
        }
    }

    override fun onHandleWork(intent: Intent) {
        resultReceiver = intent.getParcelableExtra(RECEIVER_PARAM)
        if(resultReceiver == null){
            Log.wtf(_tag, "No receiver declared. Nowhere to send back the result")
            return
        }
        onHandleIntent(intent)
    }
    fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_CRASH_DETECT -> handleCrashDetect(intent)
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleCrashDetect(intent: Intent) {
        val preFall = intent.getFloatArrayExtra(PREFALL_PARAM)
        val postFall = intent.getFloatArrayExtra(POSTFALL_PARAM)
        val sid = intent.getStringExtra(SID_PARAM)?:""
        val start = timeStamp
        val isFall: Boolean = model?.isFall(preFall!!, postFall!!) ?: false
        Log.i(_tag, "DETECTION TIME :: ${timeStamp  -  start }ms")
        //val resultCode = if(isFall) FallResult.IS_FALL else FallResult.IS_NOT
        //resultReceiver?.send(resultCode.ordinal, null)
        doCallback(SUCCESS_CODE, sid, isFall)
    }

    private fun doCallback(code: Int, sid:String, fallDetected:Boolean) = doCallbackImpl(code, sid, fallDetected, null)
    private fun doCallback(code:Int, sid:String, message: String) = doCallbackImpl(code, sid,null, message)

    private fun doCallbackImpl(code: Int, sid: String, fallDetected: Boolean? = null, message: String? = null){
        val response = Bundle()
        response.putString(SID_KEY, sid)
        if (fallDetected != null) response.putBoolean(IS_FALL_KEY, fallDetected)
        if (message != null) response.putString(MESSAGE_KEY, message)

        resultReceiver!!.send(code, response)
    }
    override fun toString(): String = StringBuilder().apply {
        this.append(" [model=${model?.name?: "none"}]")
    }.toString()

    companion object Queued {
        var jobId = 0
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionCrash(context: Context, preFall: FloatArray, postFall: FloatArray, sid: String, receiver: ResultReceiver) {
            val intent = Intent(context, CrashDetectService::class.java).apply {
                action = ACTION_CRASH_DETECT
                putExtra(PREFALL_PARAM, preFall)
                putExtra(POSTFALL_PARAM, postFall)
                putExtra(SID_PARAM, sid)
                putExtra(RECEIVER_PARAM, receiver)
            }
            //context.startService(intent)
            enqueueWork(context, intent)
        }

        private fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, CrashDetectService::class.java, 2,intent)
        }
    }
}

abstract class FallDetector(val name: String) {
    abstract fun isFall(preFall:FloatArray, postFall:FloatArray) : Boolean
}

class Bourke(val UFT: Float = 3.0f,  val LFT: Float = 0.6f,  val filter: IIRFilter? = null) : FallDetector("bourke") {

    override fun isFall(preFall: FloatArray, postFall: FloatArray): Boolean {
        // Get only 1 sec before, 1 sec after
        val shortPre: FloatArray = preFall.sliceArray((preFall.size - 50) until preFall.size)
        val shortPost: FloatArray = postFall.sliceArray(0..25)
        val episode = filter?.filter(shortPre.plus(shortPost))?:shortPre.plus(shortPost)
        // If filter given, do it
        filter?.reset()

        return episode.max()?:1.0f >= UFT || episode.min()?:1.0f <= LFT
    }

    override fun toString(): String = StringBuilder().apply {
        this.append(" $name :: [UFT=$UFT, LFT=$LFT, Filter=${filter!=null}")
    }.toString()
}

class IIRFilter(val fm:Int, val fc: Float, var y0:Float = 0.0f ){

    val T: Float
        get() = 1/fm.toFloat()
    val RC: Float
        get() = 1/(2* PI.toFloat()*fc)
    val alpha: Float = T/(T+RC)

    fun y(x:Float):Float = (1-alpha)*y0 + alpha*x

    fun filter(signal: FloatArray): FloatArray = (signal.map{ x -> y(x)}).toFloatArray()
    fun reset() { y0 = 0.0f}

    override fun toString(): String = StringBuilder().apply {
        this.append(" [fm=$fm, fc=$fc || T=$T, RC=$RC, a=$alpha]")
    }.toString()
}