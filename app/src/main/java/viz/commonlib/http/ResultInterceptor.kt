package viz.commonlib.http

import com.viz.tools.l
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit

class ResultInterceptor(private var excludeUrls: MutableList<String> = mutableListOf()) :
    Interceptor {
    private val UTF8 = Charset.forName("UTF-8")

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val rUrl = request.url.toString()
        val isLog = !excludeUrls.contains(request.url.toUrl().path)
        val response = chain.proceed(request)

        if (isLog) {
            val requestBody = request.body

            var body: String? = null

            if (requestBody != null) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                var charset = UTF8
                val contentType = requestBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(UTF8)
                }
                body = buffer.readString(charset)
            }
            l.dff(
                "发送请求\nmethod：%s\nurl：%s\nheaders: %s",
                request.method, rUrl, request.headers.toString()
            )
            parseBody(body, rUrl, "body")
            val startNs = System.nanoTime()
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

            val responseBody = response.body
            var rBody: String? = null

            if (response.promisesBody()) {
                val source = responseBody!!.source()
                source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
                val buffer = source.buffer()

                var charset = UTF8
                val contentType = responseBody.contentType()
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8)
                    } catch (e: UnsupportedCharsetException) {
                        e.printStackTrace()
                    }

                }
                rBody = buffer.clone().readString(charset)
            }

            l.dff(
                "收到响应 %s%s %ss\n请求url：%s",
                response.code.toString(), response.message, tookMs, response.request.url
            )

            parseBody(body, rUrl, "请求body")
            l.dff("响应body(%s)：%s", rUrl, rBody)
        }
        return response
    }

    private fun parseBody(body: String?, rUrl: String, tag: String) {
        if (body != null && body.length > 1024) {
            try {
                val bodyJson = JSONObject(body)
                for (key in bodyJson.keys()) {
                    val value = bodyJson[key].toString()
                    if (value.length > 1024) {
                        l.dff("$tag(%s)：%s>>%s", rUrl, key, value.substring(0, 1010) + "...数据超长省略")
                    } else {
                        l.dff("$tag(%s)：%s>>%s", rUrl, key, value)
                    }
                }
            } catch (e: Exception) {
                l.dff("$tag(%s)：%s", rUrl, body)
            }
        } else {
            l.dff("$tag(%s)：%s", rUrl, body)
        }
    }

}