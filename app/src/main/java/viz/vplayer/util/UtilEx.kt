package viz.vplayer.util

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import bolts.Task
import com.bumptech.glide.Glide
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lidroid.xutils.HttpUtils
import com.lidroid.xutils.exception.HttpException
import com.lidroid.xutils.http.HttpHandler
import com.lidroid.xutils.http.RequestParams
import com.lidroid.xutils.http.ResponseInfo
import com.lidroid.xutils.http.callback.RequestCallBack
import com.lidroid.xutils.http.client.HttpRequest
import com.viz.tools.l
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import java.io.File

fun String.fileScheme(): String {
    return "file://$this"
}

fun String.deleteFile() {
    if (isFileExist()) {
        File(this).delete()
    }
}

fun String.isFileExist(): Boolean {
    return File(this).exists()
}

fun String.createNewFile() {
    val file = File(this)
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }
}

//使用Channel 实现View 防止重复点击
fun View.setOnceClick(block: suspend () -> Unit) {
    val action = GlobalScope.actor<Unit> {
        for (event in channel) block()
    }
    setOnClickListener {
        action.offer(Unit)
    }
}

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

fun <TResult> Task<TResult>.continueWithEnd(
    taskName: String,
    isLog: Boolean = true
): Task<TResult> {
    return continueWith { t ->
        when {
            t.isCancelled -> {
                l.d("${taskName}任务取消")
            }
            t.isFaulted -> {
                val error = t.error
                l.d("${taskName}任务失败 $error")
                error.printStackTrace()
            }
            else -> {
                if (isLog) {
                    l.d("${taskName}任务成功")
                }
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
    timeout: Int = 1000 * 5,
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

fun String.isJson(isJsonObject: Boolean = false): Boolean {
    val jsonElement: JsonElement
    try {
        jsonElement = JsonParser.parseString(this)
    } catch (e: Exception) {
        return false
    }
    if (jsonElement == null) {
        return false
    }
    return if (isJsonObject) {
        jsonElement is JsonObject
    } else {
        jsonElement is JsonArray
    }
}

fun HttpUtils.download(
    context: Context,
    url: String,
    onStart: (() -> Unit)? = null,
    onProgress: ((progress: Float, current: Long, total: Long) -> Unit)? = null,
    onResult: ((target: String) -> Unit)? = null,
    onError: ((httpException: HttpException, errMsg: String) -> Unit)? = null,
    suffix: String = ""
): HttpHandler<File> {
    //int downloadLength = downloadFileSize(context, url);
    //判断手机内存是否够下载文件
    l.i(url)
    val urlUTF8 = UrlUtil.format(url)
    val ft = UrlUtil.generatLocalFileNameAndPath(context, url, true)
    val fileName = ft.first
    val target = ft.second

    val handlerDownload = download(urlUTF8, target, true, true,
        object : RequestCallBack<File>() {
            override fun onStart() {
                super.onStart()
                onStart?.invoke()
            }

            override fun onLoading(
                total: Long, current: Long,
                isUploading: Boolean
            ) {
                super.onLoading(total, current, isUploading)
                l.d("$current/$total")
                var progress = String.format("%.2f", current.toFloat() / total * 100)
                onProgress?.invoke(progress.toFloat(), current, total)
            }

            override fun onSuccess(responseInfo: ResponseInfo<File>) {
                val filePath = responseInfo.result.absolutePath
                onResult?.invoke(filePath)
            }

            override fun onFailure(
                arg0: HttpException,
                arg1: String
            ) {
                onError?.invoke(arg0, arg1)
            }
        })
    return handlerDownload
}

fun RecyclerView.imageListener(context: Context?) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    //当屏幕停止滚动，加载图片
                    try {
                        context?.let {
                            Glide.with(it).resumeRequests()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    //当屏幕滚动且用户使用的触碰或手指还在屏幕上，停止加载图片
                    try {
                        context?.let {
                            Glide.with(it).pauseRequests()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                RecyclerView.SCROLL_STATE_SETTLING -> {
                    //由于用户的操作，屏幕产生惯性滑动，停止加载图片
                    try {
                        context?.let {
                            Glide.with(it).pauseRequests()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    })
}
//自定义CoroutineExceptionHandler示例
//    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
//        throwable.printStackTrace()
//        l.e(throwable.localizedMessage)
//    }
//    GlobalScope.launch(handler){
//          download = getDownload(url)
//    }

//suspend fun getDownload(url: String): Download {
//    var download = App.instance.db.downloadDao().getByUrl(url)
//    if (download == null) {
//        download = Download()
//        download!!.videoUrl = url
//        download!!.notificationId =
//            "${TimeFormat.getCurrentTime("MMddHHmm")}${Random.nextInt(99)}".toInt()
//        App.instance.db.downloadDao().insertAll(download!!)
//    }
//    return download!!
//}


