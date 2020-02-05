package viz.commonlib.http

import com.google.gson.JsonParseException
import com.viz.tools.TimeFormat
import com.viz.tools.l
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class VCallback<T>(
    /**
     * code == 200时调用
     */
    private val onResult: (call: Call<T>, response: Response<T>, result: T) -> Unit,
    /**
     * code != 200/异常时调用
     */
    private val onError: ((errorEntity: ErrorEntity, call: Call<T>, t: Throwable?, response: Response<T>?) -> Unit) = { errorEntity, call, t, response ->
        l.e(errorEntity)
    }
) : Callback<T> {

    override fun onFailure(call: Call<T>, t: Throwable) {
        val errorEntity = ErrorEntity()
        errorEntity.message = t.message!!
        errorEntity.url = call.request().url.toString()
        errorEntity.timestamp = TimeFormat.getCurrentTimeTimeZone(TimeFormat.TIMEZONE_SHANGHAI)
        errorEntity.path = call.request().url.toUri().path
        when (t.cause) {
            is SocketTimeoutException -> {
                errorEntity.error = "请求超时"
            }
            is ConnectException -> {
                errorEntity.error = "无法连接到服务器，请检查网络连接后再试！"
            }
            is UnknownHostException -> {
                errorEntity.error = "无连接异常"
            }
            is JsonParseException -> {
                errorEntity.error = "数据解析异常"
            }
            is JSONException -> {
                errorEntity.error = "数据解析异常"
            }
            else -> {
                errorEntity.error = "未知异常"
            }
        }
        onError.invoke(errorEntity, call, t, null)
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        try {
            when {
                response.code() == 200 -> {
                    val body = response.body()
                    if (body != null) {
                        l.d(body)
                        onResult.invoke(call, response, body)
                    } else {
                        val errorEntity = ErrorEntity()
                        errorEntity.message = "body is empty"
                        errorEntity.error = "body is empty"
                        errorEntity.status = response.code()
                        errorEntity.timestamp =
                            TimeFormat.getCurrentTimeTimeZone(TimeFormat.TIMEZONE_SHANGHAI)
                        errorEntity.path = response.raw().request.url.encodedPath
                        errorEntity.url = response.raw().request.url.toString()
                        onError.invoke(errorEntity, call, null, response)
                    }
                }
                else -> {
                    if (null != response.errorBody()) {
                        //解析后台返回的错误信息
                        var errorEntity = ErrorEntity()
                        try {
                            errorEntity = ErrorEntity(response.errorBody()!!.string())
                            if (errorEntity.message.isNullOrEmpty()) {
                                errorEntity.message = response.message()
                                errorEntity.error = response.message()
                                errorEntity.status = response.code()
                                errorEntity.timestamp =
                                    TimeFormat.getCurrentTimeTimeZone(TimeFormat.TIMEZONE_SHANGHAI)
                                errorEntity.path = response.raw().request.url.encodedPath
                            }
                            errorEntity.url = response.raw().request.url.toString()
                        } catch (e: IOException) {
                            l.e("ErrorEntity解析错误:" + e.message)
                            errorEntity.message = "解析错误"
                            errorEntity.error = response.message()
                            errorEntity.status = response.code()
                            errorEntity.timestamp =
                                TimeFormat.getCurrentTimeTimeZone(TimeFormat.TIMEZONE_SHANGHAI)
                            errorEntity.path = response.raw().request.url.encodedPath
                            errorEntity.url = response.raw().request.url.toString()
                        }
                        onError.invoke(errorEntity, call, null, response)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            var errorEntity = ErrorEntity()
            errorEntity.message = "数据异常"
            errorEntity.error = response.message()
            errorEntity.status = response.code()
            errorEntity.timestamp =
                TimeFormat.getCurrentTimeTimeZone(TimeFormat.TIMEZONE_SHANGHAI)
            errorEntity.path = response.raw().request.url.encodedPath
            errorEntity.url = response.raw().request.url.toString()
            onError.invoke(errorEntity, call, null, response)
        }
    }
}