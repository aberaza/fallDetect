@file:Suppress("DEPRECATION")

package com.aberaza.wearable.alertacaida.app

import android.app.ActivityManager
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ToggleButton
import com.aberaza.wearable.alertacaida.R

class MainWearActivity : WearableActivity() {

    private val _tag = "WearableActivity"

    private var crashService : AccelSensorRead? = null

    private val connection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as AccelSensorRead.LocalBinder
            crashService = binder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_wear)

        // Enables Always-on
        setAmbientEnabled()

        val serviceStatusButton : ToggleButton = findViewById(R.id.serviceStatusButton)
        serviceStatusButton.setOnCheckedChangeListener { _, isChecked: Boolean ->
            when(isChecked){
                true -> runCrashService(Action.START)
                false -> runCrashService(Action.STOP)
            }
        }

        val uidText : EditText = findViewById(R.id.uidText)
        uidText.setOnEditorActionListener { v, actionId, event ->
            when(actionId){
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL -> {
                    crashService?.uid = v.text.toString()
                    true
                }
                else -> false
            }
        }
    }

    private var accelServiceReceiver: BroadcastReceiver? = null

    override fun onResume() {
        super.onResume()
        if(accelServiceReceiver==null){
            accelServiceReceiver = AccelSensorReceiver().also{
                val filter = IntentFilter(AccelServiceBroadcast.STATUS_UPDATE.type)
                registerReceiver(it, filter)
            }
        }
        refreshUI()
    }

    override fun onPause() {
        super.onPause()
        if(accelServiceReceiver != null) unregisterReceiver(accelServiceReceiver)
    }

    private fun bindCrashService(){
        Log.v(_tag, "Binding to crashService")
        Intent(this, AccelSensorRead::class.java).also {intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun runCrashService(action:Action){
        Intent(this, AccelSensorRead::class.java).also { intent ->
            intent.action = action.name
            intent.putExtra("uid", "something")
            startService(intent)
        }
    }

    private fun crashServiceRunning() : Boolean {
        return (isServiceRunning(AccelSensorRead::class.java)
                && crashService!= null
                && crashService!!.status == ServiceState.STARTED)
    }

    private fun isServiceRunning(service : Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for(s in manager.getRunningServices(Int.MAX_VALUE)) {
            if(service.name == s.service.className) {
                return true
            }
        }
        return false
    }

    fun refreshUI(sid:String? = null) {
        if(sid != null) {
            val uidText: EditText = findViewById(R.id.uidText)
            uidText.setText(sid)
        }
        val serviceStatusButton : ToggleButton = findViewById(R.id.serviceStatusButton)
        serviceStatusButton.isChecked = crashServiceRunning()
    }
}


private class AccelSensorReceiver : BroadcastReceiver() {
    private var ref: MainWearActivity?=null

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            AccelServiceBroadcast.STATUS_UPDATE.type -> {
                if (intent.extras != null && ref!= null)  (ref as MainWearActivity).refreshUI()
            }
        }
    }

    fun setReference(_ref : MainWearActivity) {
        ref = _ref
    }
}