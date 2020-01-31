package viz.vplayer.util

import bolts.Task
import com.lidroid.xutils.HttpUtils
import com.lidroid.xutils.exception.HttpException
import com.lidroid.xutils.http.RequestParams
import com.lidroid.xutils.http.ResponseInfo
import com.lidroid.xutils.http.callback.RequestCallBack
import com.lidroid.xutils.http.client.HttpRequest
import com.viz.tools.l

fun kotlin.String.subString(
    startIndexStr: String,
    lastIndexStr: String,
    isContainsStart: Boolean = false,
    isContainsLast: Boolean = false
): String {
    val indexStart = indexOf(startIndexStr)
    val indexLast = indexOf(lastIndexStr)
    if (indexStart > -1 && indexLast > -1) {
        return substring(
            indexStart + if (isContainsStart) {
                0
            } else {
                startIndexStr.length
            }, indexLast + if (isContainsLast) {
                lastIndexStr.length
            } else {
                0
            }
        )
    } else if (indexLast > -1 && indexStart == -1) {
        return substring(
            0, indexLast + if (isContainsLast) {
                lastIndexStr.length
            } else {
                0
            }
        )
    } else if (indexStart > -1 && indexLast == -1) {
        return substring(
            indexStart + if (isContainsStart) {
                0
            } else {
                startIndexStr.length
            }
        )
    } else {
        return substring(0)
    }
}

fun kotlin.String.subString(
    startIndex: Int,
    lastIndexStr: String,
    isContainsLast: Boolean = false
): String {
    val index = indexOf(lastIndexStr)
    if (index > -1) {
        return substring(
            startIndex, index + if (isContainsLast) {
                lastIndexStr.length
            } else {
                0
            }
        )
    } else {
        return substring(0)
    }
}

fun kotlin.String.subString(
    startIndexStr: String,
    lastIndex: Int,
    isContainsStart: Boolean = false
): String {
    val index = indexOf(startIndexStr)
    if (index > -1) {
        return substring(
            indexOf(startIndexStr) + if (isContainsStart) {
                0
            } else {
                startIndexStr.length
            }, lastIndex
        )
    } else {
        return substring(0, lastIndex)
    }
}

fun <TResult> Task<TResult>.continueWithEnd(taskName: String): Task<TResult> {
    return continueWith { t ->
        when {
            t.isCancelled -> {
                l.i("${taskName}任务取消")
            }
            t.isFaulted -> {
                val error = t.error
                l.e("${taskName}任务失败 $error")
                error.printStackTrace()
            }
            else -> {
                l.i("${taskName}任务成功")
            }
        }
        return@continueWith null
    }
}

fun <T> HttpUtils.send(
    url: String,
    onResult: (t: T) -> Unit,
    onError: (msg: String) -> Unit,
    method: HttpRequest.HttpMethod = HttpRequest.HttpMethod.GET,
    userAgent: String = DEFAULT_UA,
    charset: String = UTF8,
    timeout: Int = 1000 * 60,
    params: RequestParams? = null
) {
    configUserAgent(userAgent)
    configResponseTextCharset(charset)
    configCurrentHttpCacheExpiry(timeout.toLong()) //设置超时时间   60s
    send(
        method,
        url,
        params,
        object : RequestCallBack<T>() {
            override fun onSuccess(responseInfo: ResponseInfo<T>?) {
                if (responseInfo != null) {
                    onResult.invoke(responseInfo.result)
                } else {
                    onError.invoke("[$url]数据异常")
                }
            }

            override fun onFailure(error: HttpException?, msg: String?) {
                error?.printStackTrace()
                l.e(msg)
                onError.invoke("[$url]数据异常($msg)")
            }

        })
}