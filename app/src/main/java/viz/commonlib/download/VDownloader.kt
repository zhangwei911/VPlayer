package viz.commonlib.download

import android.content.Context
import android.net.Uri
import bolts.Task
import com.liulishuo.okdownload.*
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.viz.tools.MD5Util
import com.viz.tools.Math
import com.viz.tools.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import viz.commonlib.download.room.M3U8
import viz.commonlib.download.room.TS
import viz.commonlib.http.VCallback
import viz.vplayer.BuildConfig
import viz.vplayer.http.HttpApi
import viz.vplayer.util.*
import java.io.File
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.abs


class VDownloader {
    private var appContext: Context? = null
    private var customDir: String? = null
    private var DEBUG = false
    private var childDirName = "VDownloader"
    private var isSetChild = false
    private var isSetCustomDir = false
    private var downloadUrl: String? = null
    private var fileName: String? = null
    private var INDEX_TAG = 1
    private var lastSendTime = 0L
    private var m3u8 = M3U8()
    private var tsList = mutableListOf<TS>()
    private var tsMapList = mutableMapOf<String, TS>()
    private val isTaskLog = false
    var onProgress: ((progress: Float) -> Unit)? = null
    var onSuccess: ((filePath: String) -> Unit)? = null
    private val downloadList = mutableListOf<String>()
    private var es: ScheduledExecutorService = ScheduledThreadPoolExecutor(
        10,
        BasicThreadFactory.Builder().namingPattern("pool-check-%d").daemon(true).build()
    )
    private var listenerMulti: DownloadContextListener = object : DownloadContextListener {
        override fun taskEnd(
            context: DownloadContext,
            task: DownloadTask,
            cause: EndCause,
            realCause: java.lang.Exception?,
            remainCount: Int
        ) {
//            l.d(task)
        }

        override fun queueEnd(context: DownloadContext) {
            l.d("下载完成[$downloadUrl]")
//            allDownload {
                merge()
//            }
        }

    }

    private var listener: DownloadListener3 = object : DownloadListener3() {
        override fun warn(task: DownloadTask) {
        }

        override fun connected(
            task: DownloadTask,
            blockCount: Int,
            currentOffset: Long,
            totalLength: Long
        ) {
        }

        override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            l.d(task)
        }

        override fun started(task: DownloadTask) {
        }

        override fun completed(task: DownloadTask) {
            val percent1 = BigDecimal(1)
            taskList[task.url] = percent1
            Task.callInBackground {
                var ts = App.instance.db.tsDao().getByUrl(task.url)
                val isInsert = ts == null
                if (isInsert) {
                    ts = TS()
                    ts.url = task.url
                    ts.index = task.getTag(INDEX_TAG) as Int
                    ts.m3u8_id = m3u8.id
                }
                ts.path = task.file!!.absolutePath
                ts.progress = 100
                ts.status = 2
                if (isInsert) {
                    App.instance.db.tsDao().insertAll(ts)
                } else {
                    App.instance.db.tsDao().updateAll(ts)
                }
            }.continueWithEnd("任务[${task.url}]完成", isTaskLog)
            if (System.currentTimeMillis() - lastSendTime > 1000) {
                lastSendTime = System.currentTimeMillis()
                var progressTotal = computeProgress()
                l.d("progressTotal:${progressTotal} taskCount:${taskList.count {
                    it.value > BigDecimal.ZERO && it.value < percent1
                }} completCount:${taskList.count { it.value.compareTo(percent1) == 0 }} totalCount:${taskList.size}")
                onProgress?.invoke(progressTotal)
            }
        }

        override fun error(task: DownloadTask, e: java.lang.Exception) {
            l.df(task.url, " error:", e.message)
        }

        override fun canceled(task: DownloadTask) {
        }

        override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
//            l.df(task, currentOffset, totalLength)

            var progress = Math.divide(currentOffset.toString(), totalLength.toString())
            val percent1 = BigDecimal(1)
            if (progress.toFloat() > 1) {
                progress = percent1
            }
            taskList[task.url] = progress
        }
    }

    private fun allDownload(success: () -> Unit) {
        var downloadCount = 0
        var result = true
        val countDownLatch = CountDownLatch(taskList.size)
        Task.callInBackground {
            val tss = App.instance.db.tsDao().getAllByM3U8Id(m3u8.id)
            val reDownloadList = mutableListOf<String>()
            if (tss.isNotEmpty()) {
                tss.forEachIndexed { index, ts ->
                    Task.call(Callable<Unit> {
                        l.d(ts.path)
                        try {
                            val duration = MediaUtil.getDuration(ts.path)
                            val isFinish = abs(duration - ts.duration) < 200
                            if (ts.path.isFileExist() && isFinish) {
                                downloadCount++
                            } else {
                                ts.path.deleteFile()
                                reDownloadList.add(ts.url)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ts.path.deleteFile()
                            reDownloadList.add(ts.url)
                        }
                        countDownLatch.countDown()
                        l.d("剩余${countDownLatch.count}个")
                    }, es).continueWithEnd("${ts.path}检测 index=$index")
                }
            }
            l.d("等待检测完成")
            countDownLatch.await()
            result = downloadCount > 0 && downloadCount == tss.size
            if (!result) {
                l.d("${m3u8.url}还有${reDownloadList.size}个ts没有下载完成,重新开始下载")
                download(reDownloadList)
            } else {
                l.d("${m3u8.url}所有ts文件下载完成")
                success.invoke()
            }
        }.continueWithEnd("检测ts是否准确下载")
    }

    fun merge() {
        GlobalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    l.d("开始合并")
                    val filePath = getParentFile().absolutePath + "/" + fileName + ".mp4"
                    filePath.deleteFile()
                    //TODO 根据tsList来合并
                    //直接合并速度最快
                    SpiltAndMerge.merge(
                        getParentFile().absolutePath,
                        filePath,
                        m3u8.id
                    )
//                    SpiltAndMerge.mergePM(
//                        getParentFile().absolutePath,
//                        getParentFile().absolutePath + "/" + UUID.randomUUID() + ".mp4"
//                    )
//                    SpiltAndMerge.mergeP(
//                        getParentFile().absolutePath,
//                        getParentFile().absolutePath + "/" + UUID.randomUUID() + ".mp4"
//                    )
                    l.d("合并完成")
                    onSuccess?.invoke(filePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val taskList = mutableMapOf<String, BigDecimal>()
    private val tsDurationList = mutableListOf<Float>()

//    companion object {
//        @Volatile
//        private var instance: VDownloader? = null
//
//        fun getInstance() = instance ?: synchronized(this) {
//            instance ?: VDownloader().apply { instance = this }
//        }
//    }

    /**
     * 初始化
     */
    fun init(appContext: Context) {
        this.appContext = appContext
    }

    fun setCustomDir(dir: String): VDownloader {
        try {
            if (dir.isNullOrEmpty()) {
                isSetCustomDir = false
                l.d("setCustomDir函数参数为空或null,将使用默认路径")
            } else {
                val cDir = File(dir)
                if (!cDir.exists()) {
                    if (DEBUG) {
                        l.d("创建[${dir}]目录")
                    }
                    cDir.mkdirs()
                }
                customDir = dir
                isSetCustomDir = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isSetCustomDir = false
        }
        return this
    }

    fun setChildDirName(dirName: String): VDownloader {
        if (dirName.isNullOrEmpty()) {
            isSetChild = false
            l.d("setChildDirName函数参数为空或null,将使用默认路径")
        } else {
            childDirName = dirName
            isSetChild = true
        }
        return this
    }

    fun setFileName(fileName: String): VDownloader {
        if (fileName.isNullOrEmpty()) {
            if (DEBUG) {
                l.d("fileName函数参数为空或null,将自动生成文件名")
            }
            this.fileName = MD5Util.MD5(downloadUrl) + ".m3u8"
        } else {
            this.fileName = fileName
        }
        return this
    }

    fun load(url: String): VDownloader {
        if (url.isNullOrEmpty()) {
            l.e("load函数参数url为空或null,将取消任务")
        } else {
            downloadUrl = url
        }
        return this
    }

    private fun readM3U8(url: String) {
//        if (File(m3u8.path).exists()) {
//            parseAndDownload(url, FileUtil.readStr(m3u8.path))
//            return
//        }
        l.d(url)
        HttpApi.createHttp().anyUrl(url)
            .enqueue(VCallback<ResponseBody>(onResult = { call, response, result ->
                var rBody: String? = null
                val responseBody = response.body()
                val UTF8 = Charset.forName("UTF-8")
                if (responseBody != null) {
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
                    parseAndDownload(url, rBody)
                } else {

                }
            }, onError = { errorEntity, call, t, response ->
                l.e(errorEntity)
            }))
    }

    private fun parseAndDownload(url: String, rBody: String) {
        Task.callInBackground {
            tsList = App.instance.db.tsDao().getAllByM3U8Id(m3u8.id)
            tsList.forEach {
                tsMapList[it.url] = it
            }
            if (!File(m3u8.path).exists()) {
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        FileUtil.write(rBody, m3u8.path)
                    }
                }
            }
        }.continueWithEnd("保存m3u8文件及信息,并开始解析下载", isTaskLog)
        val parseResult = parseM3U8Content(url, rBody)
        if (parseResult.first) {
            download(parseResult.second)
        } else {
            readM3U8(parseResult.second[0])
        }
    }

    private fun parseM3U8List(
        url: String,
        urlList: MutableList<String>
    ): Pair<Boolean, MutableList<String>> {
        val list = mutableListOf<String>()
        val uri = Uri.parse(url)
        var isTS = true
        var index = 0
        urlList.forEach {
            if (it.endsWith(".m3u8")) {
                isTS = false
                val m3u8Url = if (it.startsWith("/")) {
                    "${uri.scheme}://${uri.authority}"
                } else if (it.startsWith("http://") || it.startsWith("https://")) {
                    ""
                } else {
                    "${uri.scheme}://${uri.authority}${uri.path!!.substringBeforeLast("/")}/"
                }
                list.add(
                    m3u8Url + it
                )
            } else if (it.endsWith(".ts") || it.endsWith(".jpg")) {
                val urlPrefix = if (it.startsWith("/")) {
                    "${uri.scheme}://${uri.authority}"
                } else if (it.startsWith("http://") || it.startsWith("https://")) {
                    ""
                } else {
                    "${uri.scheme}://${uri.authority}${uri.path!!.substringBeforeLast("/")}/"
                }
                val tsUrl = urlPrefix + if (it.endsWith(".jpg")) {
                    it.replace(".jpg", ".ts")
                } else {
                    it
                }
                list.add(tsUrl)
                taskList[tsUrl] = BigDecimal(0)
                index++
            } else if (it.startsWith("#EXTINF")) {
                tsDurationList.add(it.split(":")[1].replace(",", "").toFloat())
            }
        }
        return Pair(isTS, list)
    }

    private fun parseM3U8Content(
        url: String,
        m3u8Content: String
    ): Pair<Boolean, MutableList<String>> {
        val lines = m3u8Content.split(Regex("\\n")).toMutableList()
        return parseM3U8List(url, lines)
    }

    private fun getParentFile(): File {
        if (fileName == null) {
            setFileName("")
        }
        return if (customDir == null) {
            File(FileUtil.getPath(appContext!!), "$childDirName/$fileName")
        } else {
            File("$customDir/$fileName")
        }
    }

    private fun download(urls: MutableList<String>) {
        if (appContext != null) {
            val parentFile = getParentFile()
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            DownloadDispatcher.setMaxParallelRunningCount(10)
            val builder = DownloadContext.QueueSet().apply {
                setParentPathFile(parentFile)
                minIntervalMillisCallbackProcess = 1000
                isPassIfAlreadyCompleted = true
                if (!BuildConfig.DEBUG) {
                    isWifiRequired = true
                }
            }
                .commit()
            var downloadCount = 0
            val tsAddList = arrayOf<TS>()
            urls.forEachIndexed { index, url ->
                val tsExist = tsMapList[url]
                val urlUse = URLDecoder.decode(url, "utf-8")
                if (tsList.isEmpty() || tsExist == null) {
                    downloadCount++
                    downloadList.add(urlUse)
                    builder.bind(urlUse).addTag(INDEX_TAG, index)
                    Task.callInBackground {
                        val ts = TS()
                        ts.m3u8_id = m3u8.id
                        ts.url = url
                        ts.duration = tsDurationList[index]
                        ts.index = index
                        ts.path = parentFile.absolutePath + "/" + url.substringAfterLast("/")
                        App.instance.db.tsDao().insertAll(ts)
                    }.continueWithEnd("ts信息处理", isTaskLog)
                } else {
                    if (!(tsExist.path.isFileExist() && tsExist.status == 2)) {
                        downloadCount++
                        downloadList.add(urlUse)
                        builder.bind(urlUse).addTag(INDEX_TAG, index)
                    }
                }
            }
            l.d("下载数量:$downloadCount")
            builder.setListener(listenerMulti)
            val context = builder.build()
            context.startOnParallel(listener)
//            val serialQueue = DownloadSerialQueue(listener)
//            urls.forEachIndexed { index, url ->
//                val task = DownloadTask.Builder(url, parentFile).apply {
//                    setFilename(url.substringAfterLast("/")) // the minimal interval millisecond for callback progress
//                    setMinIntervalMillisCallbackProcess(1000)
//                    // do re-download even if the task has already been completed in the past.
//                    setPassIfAlreadyCompleted(true)
//                    if (!BuildConfig.DEBUG) {
//                        setWifiRequired(true)
//                    }
//                }.build()
//                serialQueue.enqueue(task)
//            }
        } else {
            l.e("请调用VDownloader.init(this)")
        }
    }

    fun start() {
        downloadUrl?.apply {
            m3u8.url = this
            m3u8.path = getParentFile().absolutePath + "/index.m3u8"
            Task.callInBackground {
                val m = App.instance.db.m3u8Dao().getByUrl(this)
                if (m == null) {
                    App.instance.db.m3u8Dao().insertAll(m3u8)
                } else {
                    m3u8 = m
                }
            }.continueWithEnd("处理m3u8数据", isTaskLog)
            readM3U8(URLDecoder.decode(this, "utf-8"))
        }
    }

    private fun computeProgress(): Float {
        var progressTotal = BigDecimal(0)
        val tSize = BigDecimal(taskList.size)
        taskList.forEach {
            progressTotal += it.value.divide(tSize, 100, BigDecimal.ROUND_HALF_UP)
        }
        return progressTotal.toFloat()
    }
}