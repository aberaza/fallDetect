package com.aberaza.wearable.alertacaida.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aberaza.wearable.alertacaida.BuildConfig

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.lang.ref.WeakReference

import kotlin.math.abs
import kotlin.math.floor



class AccelSensorRead : Service(), SensorEventListener {

    private val _tag = "AccelSensorRead"

    //private var status = ServiceState.STOPPED
    private var sessionStatus = SessionState.IDLE

    var status : ServiceState = ServiceState.STOPPED
        private set(s:ServiceState) {field = s}

    var uid  : String = "__default__"
        get() {
            if(field == "__default__")
                field = getUID()
            return field
        }
        set(sid) { field = setUID(sid) as String }



    /* Sensors and Manager */
    private lateinit var sensorManager: SensorManager
    private lateinit var accelSensor: Sensor

    /* Network Conectivity */

    private val _awsApiUrl = BuildConfig.API_URL
    private val netManager = NetManager(this, _awsApiUrl)

    /* Buffer and Settings */
    private var motionThreshold: Float = SensorManager.STANDARD_GRAVITY * 1.5f
    private var samplesAfterHit: Int = 50 //= getAccelServicePostcrashBufferLength()

    private lateinit var accelBuffer: FloatRingBuffer // = FloatRingBuffer(getAccelServicePrecrashBufferLength(), getAccelServicePostcrashBufferLength())

    private lateinit var crashResponseReceiver : ResultReceiver


    private var pendingEpisodes : HashMap<String, Episode> = HashMap<String, Episode> ()

    /* Service Binding */
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): AccelSensorRead = this@AccelSensorRead
    }
    override fun onBind(intent: Intent): IBinder {
        Log.v(_tag, "onBind service")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(_tag, "onStartCommand called")
        if(intent!=null) {
            val extras = intent.extras
            uid = "anonymous"
            if(extras != null) uid = extras.get("uid") as String

            when(intent.action) {
                Action.START.name -> startService()
                Action.STOP.name -> stopService()
                else -> {
                    startService()
                    Log.w(_tag, "No action in received intent!")
                }
            }

        }else{
            Log.i(_tag, "No intent, restarted by system")
        }
        return START_STICKY
    }

    override fun onCreate() {
        //super.onCreate()
        Log.v(_tag, "onCreate()")
        CONTEXT = this
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //results in m/s^2
        samplesAfterHit = getAccelServicePostcrashBufferLength()

        crashResponseReceiver = CrashResultReceiver(WeakReference(this), Handler())
        startService()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(_tag, "onDestroy")
        this.stopService()
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(crashResponseReceiver)
        //crashResponseReceiver = null
    }


    private fun startService() {
        Log.v(_tag, "startService()")
        if(status == ServiceState.STOPPED){
            accelBuffer = FloatRingBuffer(getAccelServicePrecrashBufferLength(), getAccelServicePostcrashBufferLength())
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
            //iirFilter = IIRFilter(accelSensor.minDelay,
            changeEpisodeStatus(SessionState.RECORDING)
            status = ServiceState.STARTED
            toast(this, "AccelSensorRead Started ${status.name}")
        }
        Log.v(_tag, "accelBuffer:: ${accelBuffer.toString()}")
    }

    private fun stopService() {
        Log.v(_tag, "stopService()")
        if(status == ServiceState.STARTED){
            sensorManager.unregisterListener(this, this.accelSensor)
            changeEpisodeStatus(SessionState.IDLE)
            status = ServiceState.STOPPED
            toast(this, "AccelSensorRead Stopped")
        }
    }

    private fun changeEpisodeStatus(status: SessionState) {
        sessionStatus = status
        toast(this, "SET sessionStatus : ${status.name}")
        Intent(AccelServiceBroadcast.STATUS_UPDATE.type).also {intent ->
            intent.putExtra("status", status.name)
            sendBroadcast(intent)
        }

        when(status) {
            SessionState.IDLE -> accelBuffer.clear()
            SessionState.RECORDING -> Log.i(_tag, "Change sessionStatus to RECORDING")
            SessionState.DETECTED -> Log.i(_tag, "Change sessionStatus to DETECTED")
            SessionState.COMPUTING -> {
                val sid = enqueueEpisode(
                    Episode(
                        uid ?: "someone",
                        accelSensor.resolution,
                        accelSensor.maximumRange,
                        floor(1000000.0 / accelSensor.minDelay).toInt(),
                        System.currentTimeMillis(),
                        accelBuffer.maxSize.toLong(),
                        accelBuffer.contents(),
                        false
                    )
                )
                processEpisode(sid)
                changeEpisodeStatus(SessionState.RECORDING)
            }
        }
    }

    private fun enqueueEpisode(episode: Episode): String {
        val sid: String = "$uid-$timeStamp"
        pendingEpisodes[sid] = episode
        return sid
    }
    private fun processEpisode(sid: String) {
        CrashDetectService.startActionCrash(
            this,
            accelBuffer.preArray,
            accelBuffer.postArray,
            sid,
            crashResponseReceiver
        )
    }
    private fun dequeueEpisode(sid: String):Episode? {
        val episode: Episode? = pendingEpisodes[sid]
        if(episode != null) pendingEpisodes.remove(sid)
        return episode
    }
    fun saveEpisode(episode: Episode) {
        val serializedEpisode = Json(JsonConfiguration.Stable).stringify(Episode.serializer(), episode)
        Log.i(_tag, serializedEpisode)
        netManager.postJson(serializedEpisode)
    }

    private val processingEpisodesQueue: MutableList<Episode>
        get() {
            TODO()
        }

    private fun queueEpisode(episode: Episode) {

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //TODO("Not yet implemented")
    }

    private var extraSamplesCounter : Int = 0
    private fun processAccelSample(sample:FloatArray, checkThreshold:Boolean = true){

        accelBuffer.enqueue(vectorModule(sample))

        if(checkThreshold){
            val lastMotion = if(accelBuffer.previous !== null) accelBuffer.previous else SensorManager.GRAVITY_EARTH
            if( abs(accelBuffer.current!! - lastMotion!!) >= motionThreshold && accelBuffer.isFull){
                changeEpisodeStatus(SessionState.DETECTED)
                extraSamplesCounter = 0
            }
        }else{
            if(++extraSamplesCounter == samplesAfterHit) changeEpisodeStatus(SessionState.COMPUTING)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                when(sessionStatus){
                    SessionState.RECORDING -> processAccelSample(event.values, true) //if threshold fullfilled will switch to DETECTED
                    SessionState.DETECTED -> processAccelSample(event.values, false) //After N samples will switch to COMPUTING
                    SessionState.COMPUTING -> processAccelSample(event.values, false) //Keep adding samples to buffer
                    SessionState.IDLE -> Log.w(_tag, "sessionStatus is IDLE")
                }
            }
        }
    }



    private class CrashResultReceiver(val service: WeakReference<AccelSensorRead>, handler: Handler?) : ResultReceiver(handler) {

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            val sid = resultData?.getString(SID_KEY)?:""
            var serviceRef = service.get()

            when(resultCode){
                SUCCESS_CODE -> {
                    val episode: Episode? = serviceRef?.dequeueEpisode(sid)
                    if (episode != null) {
                        episode.isFall = resultData?.getBoolean(IS_FALL_KEY)
                        serviceRef?.saveEpisode(episode)
                    }
                }
                FAILURE_CODE -> {
                    serviceRef?.dequeueEpisode(sid)
                    Log.w("CrashResultReceiver", "ERR: ${resultData?.getString(MESSAGE_KEY)?:"No Error Message"}")                 }
                else -> Log.w("CrashResultReceiver", "Failed To Process. Error result from crash detect service")
            }
            super.onReceiveResult(resultCode, resultData)
        }
    }


}

