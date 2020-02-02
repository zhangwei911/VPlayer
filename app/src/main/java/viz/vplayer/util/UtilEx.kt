package viz.vplayer.util

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import bolts.Task
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
import com.viz.tools.MD5Util
import com.viz.tools.TimeFormat
import com.viz.tools.Toast
import com.viz.tools.l
import viz.vplayer.R
import viz.vplayer.room.Download
import viz.vplayer.room.NotificationId
import java.io.File
import java.net.URLDecoder
import kotlin.random.Random

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
    onProgress: (progress: Float) -> Unit = {},
    onResult: (target: String) -> Unit = {},
    onError: ((httpException: HttpException, errMsg: String) -> Unit)? = null,
    suffix: String = ""
): HttpHandler<File> {
    var download: Download? = null
    Task.callInBackground {
        l.start("bolts")
        download = App.instance.db.downloadDao().getByUrl(url + "1")
        if (download == null) {
            download = Download()
            download!!.videoUrl = url
            download!!.notificationId =
                "${TimeFormat.getCurrentTime("MMddHHmm")}${Random.nextInt(99)}".toInt()
            App.instance.db.downloadDao().insertAll(download!!)
        }
        l.end("bolts")
    }.continueWithEnd("下载数据记录")
    //int downloadLength = downloadFileSize(context, url);
    //判断手机内存是否够下载文件
    l.i(url)
    var oldReplaceWords = arrayListOf("+", " ", "*")
    var newReplaceWords = arrayListOf("%2B", "%20", "%2A")
    var urlUTF8 = URLDecoder.decode(url, "UTF-8")
    for (i in oldReplaceWords.indices) {
        urlUTF8 = urlUTF8.replace(oldReplaceWords[i], newReplaceWords[i])
    }
    l.i(urlUTF8)
    var uri = Uri.parse(urlUTF8)
    val fileName = MD5Util.MD5(url) + "." + if (suffix == "") {
        uri.pathSegments.last()
    } else {
        suffix
    }
    val target = FileUtil.getPath(context) + "/" + fileName
    l.i(target)
    var progressLast = 0.00f
    NotificationUtil.createNotificationChannel(context)
    val GROUP_KEY_WORK_VIDEO = "viz.vplayer.WORK_VIDEO"

    val builder = NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD).apply {
        setContentText("视频下载${fileName}")
        setContentTitle("0.00%")
        setSmallIcon(android.R.drawable.stat_sys_download)
        priority = NotificationCompat.PRIORITY_LOW
        setOnlyAlertOnce(true)
        setGroup(GROUP_KEY_WORK_VIDEO)
    }
    val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD).apply {
        setContentTitle("视频下载")
        //set content text to support devices running API level < 24
        setContentText("more download")
        setSmallIcon(R.drawable.vplayer_logo)
        //build summary info into InboxStyle template
//        .setStyle(NotificationCompat.InboxStyle()
//            .addLine("Alex Faarborg Check this out")
//            .addLine("Jeff Chang Launch Party")
//            .setBigContentTitle("2 new messages")
//            .setSummaryText("janedoe@example.com"))
        //specify which group this notification belongs to
        setGroup(GROUP_KEY_WORK_VIDEO)
        //set this notification as the summary for the group
        setGroupSummary(true)
        setOnlyAlertOnce(true)
    }
        .build()

    val PROGRESS_MAX = 100
    val handlerDownload = download(urlUTF8, target, true, true,
        object : RequestCallBack<File>() {
            override fun onStart() {
                super.onStart()
                Toast.show(context, "开始下载")
                l.d("开始下载")
            }

            override fun onLoading(
                total: Long, current: Long,
                isUploading: Boolean
            ) {
                super.onLoading(total, current, isUploading)
                l.d("$current/$total")
                var progress = String.format("%.2f", current.toFloat() / total * 100)
                if (progress.toFloat() - progressLast > 1) {
                    onProgress.invoke(progress.toFloat())
                    l.i(progress.toString())
                    Toast.show(
                        context,
                        "$progress%($current/$total)"
                    )
                    progressLast = progress.toFloat()
                    val notificationId = download!!.notificationId
                    NotificationManagerCompat.from(context).apply {
                        // Issue the initial notification with zero progress
                        builder.setProgress(PROGRESS_MAX, progress.toFloat().toInt(), false)
                        builder.setContentTitle("${progress.toFloat()}%")
                        notify(notificationId, builder.build())
                        notify(111111, summaryNotification)

                        // Do the job here that tracks the progress.
                        // Usually, this should be in a
                        // worker thread
                        // To show progress, update PROGRESS_CURRENT and update the notification with:
                        // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                        // notificationManager.notify(notificationId, builder.build());

                        if (progress.toFloat() == 100f) {
                            // When done, update the notification one more time to remove the progress bar
                            builder.setContentTitle("下载完成")
                                .setProgress(0, 0, false)
                            notify(notificationId, builder.build())
                        }
                    }
                }
            }

            override fun onSuccess(responseInfo: ResponseInfo<File>) {
                val filePath = responseInfo.result.absolutePath
                onResult.invoke(filePath)
                Toast.show(context, "下载成功\n$filePath")
                l.d("下载成功 $filePath")
                Task.callInBackground {
                    if (download != null) {
                        App.instance.db.downloadDao()
                            .deleteByNotificationId(NotificationId(download!!.notificationId))
                    }
                }.continueWithEnd("下载数据记录删除")
            }

            override fun onFailure(
                arg0: HttpException,
                arg1: String
            ) {
                onError?.invoke(arg0, arg1)
                Toast.show(context, "下载失败")
                l.e("下载失败")
            }
        })
    return handlerDownload
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


