package com.aberaza.wearable.alertacaida.app

import kotlin.math.sqrt

fun vectorModule(vector: FloatArray): Float {
    var squareSum = 0.0f
    for(component in vector) {
        squareSum += (component * component)
    }
    return sqrt(squareSum)
}

/**
 * RingBuffer uses a fixed length array to implement a queue, where,
 * - [tail] Items are added to the tail
 * - [head] Items are removed from the head
 * - [capacity] Keeps track of how many items are currently in the queue
 */
class RingBuffer<T>(val maxSize:Int = 10) {
    private val _tag = this::class.java.simpleName
    private var preLength: Int? = null
    private var postLength: Int? = null

    constructor(preCrash : Int = 0, postCrash:Int = 0): this(preCrash + postCrash) {
        preLength = preCrash
        postLength = postCrash
    }

    constructor(timesteps: Int = 0, stride: Int = 0, prediction: Int = 0) : this(timesteps) {
        preLength = timesteps
        postLength = prediction + stride - timesteps
    }


    private val array = mutableListOf<T?>().apply {
        for (index in 0 until maxSize) {
            add(null)
        }
    }

    private var head = 0
    private var tail = 0
    private var capacity = 0

    val preSize
        get() = preLength?:maxSize
    val postSize
        get() = postLength?:0

    val preArray: List<T?>
        get() = array.slice(0..preSize)
    val postArray: List<T?>
        get() = array.slice(maxSize-postSize until maxSize)

    fun clear() {
        head = 0
        tail = 0
        capacity = 0
    }

    fun enqueue(item: T): RingBuffer<T> {
        array[tail] = item
        tail = (tail + 1) % maxSize

        if(capacity == maxSize){
            head = (head + 1) % maxSize
        }else{
            capacity++
        }

        return this
    }

    val current: T?
        get() = array[head]

    val previous: T?
        get() {
            val index = if(head > 0) (head - 1) else (maxSize - 1)
            //Log.w(_tag, "Try to access index $index")
            if (capacity > 1) return array[index]
            return null
        }


    val isFull: Boolean
        get() = capacity == maxSize

    override fun toString(): String = StringBuilder().apply {
        this.append(" [capacity=$capacity, H=$head, T=$tail] A=${averageSum()}")
    }.toString()

    private fun averageSum(): Float {
        var sum = 0.0f
        var itemCount = capacity
        var readIndex = head
        while (itemCount > 0) {
            sum += array[readIndex] as Float
            readIndex = (readIndex + 1) % maxSize
            itemCount --
        }
        return sum/capacity
    }


    fun contents(): MutableList<T?> {
        return mutableListOf<T?>().apply {
            var itemCount = capacity
            var readIndex = head
            while (itemCount > 0) {
                add(array[readIndex])
                readIndex = (readIndex + 1)%maxSize
                itemCount--
            }
        }
    }
}