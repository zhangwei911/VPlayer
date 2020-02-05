package com.secway.happyvoice.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

class NetworkInterceptor(private val applicationContext: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.getNetworkCapabilities(cm.activeNetwork)
        val isConnected = activeNetwork != null && activeNetwork.hasCapability(NET_CAPABILITY_VALIDATED)

        return if(isConnected){
            chain.proceed(chain.request())
        }else{
            val noNetworkJson = "{\"code\":30000,\"msg\":\"没有连接网络\"}"
            Response.Builder()
                    .code(30000)
                    .message(noNetworkJson)
                    .protocol(Protocol.HTTP_1_0)
                    .request(chain.request())
                    .body(noNetworkJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type","application/json")
                    .addHeader("charset","UTF-8")
                    .build()
        }
    }
}