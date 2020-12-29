package com.aberaza.wearable.alertacaida.app.helpers

abstract class FallDetector(val name: String) {
    abstract fun isFall(preFall:FloatArray, postFall:FloatArray) : Boolean
}