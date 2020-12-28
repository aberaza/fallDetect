package com.aberaza.wearable.alertacaida.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val prefsName         = "CAIDA_DETECT_PREFS"
private const val serviceState      = "ACCEL_SERVICE_STATE"

private const val accelBufferPrecrashLength = "ACCEL_SERVICE_BUFFER_PRECRASH_LENGTH"
private const val accelBufferPostcrashLength = "ACCEL_SERVICE_BUFFER_POSTCRASH_LENGTH"


private const val accelAverageThreshold = "ACCEL_SERVICE_AVG_TRSH"

private const val sessionUID        = "SESSION_USER_ID"

private var c: Context? = null

val TAG = "SettingsConfig"
/* HELPERS */
var CONTEXT : Context
    get() = c!!
    set(context:Context) {
        c = context
    }

val PREFS: SharedPreferences
    get() = CONTEXT.getSharedPreferences(prefsName, 0)

private fun setValue(name:String, value: Any):Any = PREFS.edit().let{
    when(value){
        is String -> it.putString(name, value)
        is Int -> it.putInt(name, value)
        else -> Log.w(TAG, "Could not infer type of value")
    }
    it.apply()
    return value
}
private fun setString(name: String, value: String) = PREFS.edit().let{
    it.putString(name, value)
    it.apply()
}

/* public API */
fun getAccelServiceState(): ServiceState =
    ServiceState.valueOf(PREFS.getString(serviceState, ServiceState.STOPPED.name)?:"null")

fun setAccelServiceState(status: ServiceState) = setValue(serviceState, status.name)

fun getAccelServicePrecrashBufferLength(): Int = PREFS.getInt(accelBufferPrecrashLength, 200)
fun getAccelServicePostcrashBufferLength(): Int = PREFS.getInt(accelBufferPostcrashLength, 50)
fun getAccelServiceBufferLength(): Int = getAccelServicePrecrashBufferLength() + getAccelServicePostcrashBufferLength()

fun getUID() : String = PREFS.getString(sessionUID, "defaultUID")?: "noUID"
fun setUID(uid: String) = setValue(sessionUID, uid)

