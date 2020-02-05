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


abstract class SimpleCallback<T> : Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) {
        when {
            response.code() == 200 -> onResult(call, response, response.body()!!)
            else -> {
                //解析后台返回的错误信息
                var errorEntity = ErrorEntity()
                try {
                    errorEntity = ErrorEntity(response.errorBody()!!.string())
                    errorEntity.url = response.raw().request.url.toString()
                } catch (e: IOException) {
                    l.e("ErrorEntity解析错误:" + e.message)
                }
                onError(errorEntity, call, null, response)
            }
        }
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        val errorEntity = ErrorEntity()
        errorEntity.message = t.message!!
        errorEntity.url = call.request().url.toString()
        errorEntity.timestamp = TimeFormat.getCurrentTime(TimeFormat.TIMEZONE_SHANGHAI)
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
        onError(errorEntity, call, t, null)
    }

    /**
     * code == 200时调用
     */
    abstract fun onResult(call: Call<T>, response: Response<T>, result: T)

    /**
     * code != 200/异常时调用
     */
    protected open fun onError(
        errorEntity: ErrorEntity,
        call: Call<T>,
        t: Throwable?,
        response: Response<T>?
    ) {
        l.e(errorEntity)
    }
}