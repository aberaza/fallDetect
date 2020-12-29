package com.aberaza.wearable.alertacaida.app.helpers

import android.util.Log
import kotlin.math.max

class FloatRingBuffer(val maxSize: Int = 10){
    private val _tag = this::class.java.simpleName
    private var preLength: Int? = null
    private var postLength: Int? = null

    constructor(preCrash : Int = 0, postCrash:Int = 0): this(preCrash + postCrash) {
        preLength = preCrash
        postLength = postCrash
        Log.d(_tag, "Create RingBuffer of size $maxSize")
    }

    constructor(in_timesteps : Int = 0, out_timesteps:Int = 0, stride: Int = 0): this(stride + out_timesteps) {
        preLength = in_timesteps - out_timesteps / 2
        postLength = maxSize - (in_timesteps - out_timesteps/2)
    }

    private val array = FloatArray(maxSize)

    private var head = 0
    private var tail = 0
    private var capacity = 0

    val preSize
        get() = preLength?:maxSize
    val postSize
        get() = postLength?:0

    val preArray: FloatArray
        get() = array.sliceArray(0..preSize)
    val postArray: FloatArray
        get() = array.sliceArray(maxSize-postSize until maxSize)
    val fullArray: FloatArray
        get() = array


    fun clear() {
        head = 0
        tail = 0
        capacity = 0
    }

    fun enqueue(item: Float): FloatRingBuffer {
        array[tail] = item
        tail = (tail + 1) % maxSize

        if(capacity == maxSize){
            head = (head + 1) % maxSize
        }else{
            capacity++
        }

        return this
    }

    val current: Float
        get() = array[head]

    val previous: Float?
        get() {
            val index = if(head > 0) (head - 1) else (maxSize - 1)
            //Log.w(_tag, "Try to access index $index")
            if (capacity > 1) return array[index]
            return null
        }


    val isFull: Boolean
        get() = capacity == maxSize

    override fun toString(): String = StringBuilder().apply {
        this.append(" [capacity=$capacity, H=$head, T=$tail, Pre=$preSize, Post=$postSize] A=${averageSum()}")
    }.toString()

    private fun averageSum(): Float {
        var sum = 0.0f
        var itemCount = capacity
        var readIndex = head
        while (itemCount > 0) {
            sum += array[readIndex]
            readIndex = (readIndex + 1) % maxSize
            itemCount --
        }
        return sum/capacity
    }


    fun contents(): FloatArray {
        return (if(tail >= head) array.sliceArray(head..tail)
            else array.sliceArray(head until maxSize).plus(array.sliceArray(0..tail))
          )
    }
}