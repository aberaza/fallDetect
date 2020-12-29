package com.aberaza.wearable.alertacaida.app.helpers

import com.aberaza.wearable.alertacaida.app.Model
import kotlin.math.PI


class IIRFilter(val fm:Int = Model.SAMPLING_RATE, val fc: Float= Model.IIR_LPF, var y0:Float = 0.0f ){

    private val t: Float
        get() = 1/fm.toFloat()
    private val rc: Float
        get() = 1/(2* PI.toFloat()*fc)
    private val alpha: Float = t/(t+rc)

    private fun y(x:Float):Float = (1-alpha)*y0 + alpha*x

    fun filter(signal: FloatArray): FloatArray = (signal.map{ x -> y(x)}).toFloatArray()
    fun reset() { y0 = 0.0f}

    override fun toString(): String = StringBuilder().apply {
        this.append(" [fm=$fm, fc=$fc || T=$t, RC=$rc, a=$alpha]")
    }.toString()
}