@file:Suppress("DEPRECATION")

package com.aberaza.wearable.alertacaida.app

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result

enum class RequestType {
    GET,
    POST
}

class NetRequest(val uri:String, val type: RequestType = RequestType.GET, val body : String="")

class NetManager(private val context: Context, private val url: String) {
    private val _tag = this::class.java.simpleName

    private val networkQueue = mutableListOf<NetRequest>()

    fun getJson(_url: String) = doGet(_url)
    fun postJson(data: String) = doPost(data)
    fun postJson(data: String, _url: String) = doPost(data, _url)


    private fun processQueue() {
        for( request in networkQueue){
            when(request.type){
                RequestType.POST -> doPost(request)
                RequestType.GET -> doGet(request)
            }
        }
    }

    private fun doGet(request: NetRequest) = doGet(request.uri)
    private fun doGet(_url:String): String? {
        if(isNetworkAvailable()){
            var getResult: String = ""
            Fuel.get(_url).responseString { _, _, result->
                when(result){
                    is Result.Failure -> Log.w(_tag, Exception("Failed to GET $_url"))
                    is Result.Success -> getResult = result.get()
                }
            }

            return getResult
        }
        return null
    }

    private fun doPost(request: NetRequest) = doPost(request.body, request.uri)
    private fun doPost(data: String, _url: String = url) {
        //toast
        Log.i(_tag, "Send data to server")
        try {
            Fuel.post(_url)
                .jsonBody(data)
                .response{ result -> Log.i(_tag, result.component2().toString()) }
        } catch (e: Exception) {
            Log.e(_tag, e.message.toString())
        }finally {
            Log.i(_tag, "Finished sendPost")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE)

        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }
}