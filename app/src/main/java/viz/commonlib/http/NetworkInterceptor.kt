package viz.commonlib.http

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.greenrobot.eventbus.EventBus
import viz.vplayer.R
import viz.vplayer.eventbus.CommonInfoEvent
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.util.NetUtil

class NetworkInterceptor(private val applicationContext: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val netUtil = NetUtil()
        val isConnected = netUtil.iConnected(applicationContext)

        return if (isConnected) {
            chain.proceed(chain.request())
        } else {
            EventBus.getDefault().postSticky(NetEvent(false))
            EventBus.getDefault()
                .postSticky(CommonInfoEvent(true, applicationContext.getString(R.string.network_invalid)))
            val noNetworkJson = "{\"code\":30000,\"msg\":\"没有连接网络\"}"
            Response.Builder()
                .code(30000)
                .message(noNetworkJson)
                .protocol(Protocol.HTTP_1_0)
                .request(chain.request())
                .body(noNetworkJson.toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .addHeader("charset", "UTF-8")
                .build()
        }
    }
}