package viz.commonlib.http

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

class FakeInterceptor(private val urlStr: String, private val respJson: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        val url = chain.request().url.toUrl()
        return if (url.path == urlStr) {
            response = Response.Builder()
                .code(200)
                .message(respJson)
                .protocol(Protocol.HTTP_1_0)
                .request(chain.request())
                .body(
                    respJson.toResponseBody("application/json".toMediaTypeOrNull())
                )
                .addHeader("content-type", "application/json")
                .addHeader("charset", "UTF-8")
                .build()
            response
        } else {
            chain.proceed(chain.request())
        }
    }
}