package com.aberaza.wearable.alertacaida.app.helpers

import android.content.res.AssetManager
import android.util.Log
import com.aberaza.wearable.alertacaida.app.Model
import com.aberaza.wearable.alertacaida.app.ModelInterpreter
import com.aberaza.wearable.alertacaida.app.timeStamp
import kotlin.math.pow
import kotlin.math.sqrt

class HybridModel(private val rmseLimit: Float = Model.RMSE_LIMIT, assetManager: AssetManager): FallDetector("HybridModel"){
    private val _tag = this::class.java.simpleName
    private var interpreter: ModelInterpreter = ModelInterpreter(assetManager)


    private fun rmse(y: FloatArray, pred:FloatArray): Float {
        if (y.size != pred.size){
            return 0.0f
        }
        var cumul = 0.0f
        for (i in 0..y.size){
            cumul += (y[i]-pred[i]).pow(2)
        }
        cumul /= y.size
        return sqrt(cumul)
    }
    private fun rmse(y: FloatArray, pred:Array<Float>): Float {
        val predArray = FloatArray(pred.size){pred[it]}
        return rmse(y, predArray)
    }

    override fun isFall(preFall: FloatArray, postFall: FloatArray): Boolean {
        if(interpreter == null){
            Log.e(_tag, "interpreter was not initialized")
            return false
        }
        val start = timeStamp
        val prediction: Array<Float> = interpreter.predict(preFall)
        val isFall: Boolean = rmse(postFall, prediction) >= this.rmseLimit
        Log.i(_tag, "Prediction in ${start - timeStamp}")
        return isFall
    }
}