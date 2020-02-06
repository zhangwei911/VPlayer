package viz.vplayer.http

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url
import viz.commonlib.http.BaseHttp

interface HttpApi {
    @GET
    fun anyUrl(@Url url: String, @QueryMap param: MutableMap<String, String> = mutableMapOf()): Call<ResponseBody>

    companion object {
        fun createHttp(url: String = "http://www.baidu.com", debug: Boolean = true): HttpApi {
            return BaseHttp.createHttp(url, debug)
        }
    }
}