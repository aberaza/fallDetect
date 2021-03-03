package com.aberaza.wearable.alertacaida.app

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelInterpreter constructor(assets: AssetManager) {
    private val _tag = this::class.java.simpleName
    //private val MODEL_PATH = "tfliteModel.tflite"
    // private val MODEL_PATH = "tfliteModel.175.dynamic.tflite"
    private val MODEL_PATH = "tfliteModel.350.dynamic.tflite"
    private var interpreter: Interpreter?=null

    private var inputs = Array<Array<FloatArray>>(1,{Array<FloatArray>(Model.IN_TIMESTEPS, {FloatArray(1)})})
    private var outputs = Array<Array<FloatArray>>(1,{Array<FloatArray>(Model.OUT_TIMESTEPS, {FloatArray(1)})})

    init{
        try {
            val options = Interpreter.Options()
//            options.setNumThreads(4)
//            options.setUseNNAPI(false)
            interpreter = Interpreter(loadModelFile(assets, MODEL_PATH), options)
            interpreter!!.allocateTensors()
            Log.d(_tag, "INTERPRETER INFO:\n InputTensors: ${interpreter!!.inputTensorCount}\n InputTensorShape ${interpreter!!.getInputTensor(0).shape()}")
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
        Log.d(_tag, "accelData length = ${accelData.size}")
        for (index in accelData.indices) {
            inputs[0][index][0] = accelData[index]
        }
        interpreter!!.run(inputs, outputs)

        return Array(Model.OUT_TIMESTEPS){ outputs[0][it][0]}
    }

    fun close() {
        interpreter?.close()
    }
}