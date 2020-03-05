package viz.vplayer.http

import okhttp3.Interceptor
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import viz.commonlib.http.BaseHttp
import viz.vplayer.util.DEFAULT_RULE_URL

interface HttpApi {
    @GET
    fun anyUrl(@Url url: String, @QueryMap param: MutableMap<String, String> = mutableMapOf()): Call<ResponseBody>

    @FormUrlEncoded
    @POST
    fun anyPostUrl(@Url url: String, @FieldMap param: MutableMap<String, String> = mutableMapOf()): Call<ResponseBody>

    @GET
    suspend fun anyUrlS(@Url url: String): Response<ResponseBody>

    companion object {
        fun createHttp(
            url: String = "http://www.baidu.com",
            debug: Boolean = true,
            interceptorList: MutableList<Interceptor> = mutableListOf<Interceptor>(),
            connectTimeout: Long = 60,
            readTimeout: Long = 60,
            writeTimeout: Long = 60,
            excludeUrls: MutableList<String> = mutableListOf(DEFAULT_RULE_URL,"http://www.baidu.com/",".m3u8"),
            addGsonConverterFactory: Boolean = true
        ): HttpApi {
            return BaseHttp.createHttp(
                url,
                debug,
                interceptorList = interceptorList,
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                excludeUrls = excludeUrls,
                addGsonConverterFactory = addGsonConverterFactory
            )
        }
    }
}