package com.aberaza.wearable.alertacaida.app

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.PrimitiveKind
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class ModelInterpreter constructor(assets: AssetManager) {
    private val _tag = this::class.java.simpleName
    private val MODEL_PATH = "tfliteModel.tflite"
    private var interpreter: Interpreter?=null

    private var inputs = Array<Array<FloatArray>>(1,{Array<FloatArray>(200, {FloatArray(1)})})
    private var outputs = Array<Array<FloatArray>>(1,{Array<FloatArray>(100, {FloatArray(1)})})

    init{
        try {
            val options = Interpreter.Options()
//            options.setNumThreads(4)
//            options.setUseNNAPI(false)
            interpreter = Interpreter(loadModelFile(assets, MODEL_PATH), options)
            interpreter!!.allocateTensors()
            Log.d(_tag, "INTERPRETER INFO:\n InputTensors: ${interpreter!!.inputTensorCount}\n InputTensorShape ${interpreter!!.getInputTensor(0).shape()}")

            //this.inputs = <Array<FloatArray>>(1,{Array<FloatArray>(100, {FloatArray(1)})})
            //this.outputs = <Array<FloatArray>>(1,{Array<FloatArray>(100, {FloatArray(1)})})

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(accelData:FloatArray):Array<Float>{
        // Use input, output to avoid recurrent allocations
        /*
        val inputArray = arrayOf<Float>()
        val outputArray= arrayOf<Float>()
        //inputs[0] = arrayOf<
        interpreter!!.run(inputArray, outputArray)
        return outputArray
        */

        Log.d(_tag, "Inputs are ${Arrays.toString(accelData)}")
        //val pruebaIn = Array<Array<FloatArray>>(1) { batch -> Array<FloatArray>(100){ step -> FloatArray(1){accelData[step]}}}
        for (index in accelData.indices) {
            Log.d(_tag, "Populate inputs[${index}] with ${accelData[index]}")
            inputs[0][index][0] = accelData[index]
        }
        Log.d(_tag, "Inputs parsed ${Arrays.toString(inputs[0])}")
        val pruebaOut = Array<Array<FloatArray>>(1,{Array<FloatArray>(100, {FloatArray(1)})})
        //var testInput = ByteBuffer.allocateDirect(1*100*1*BYTE_SIZE(Float))
        interpreter!!.run(inputs, pruebaOut)
        val flattenedArray = Array<Float>(100){ pruebaOut[0][it][0]}
        Log.d(_tag, "Output is ${Arrays.toString(flattenedArray)}")
        return flattenedArray
    }

    fun close() {
        interpreter?.close()
    }
}