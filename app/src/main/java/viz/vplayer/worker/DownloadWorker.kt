package viz.vplayer.worker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.workDataOf
import bolts.Task
import com.arialyy.annotations.TaskEnum
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.common.AbsNormalEntity
import com.arialyy.aria.core.download.m3u8.M3U8VodOption
import com.arialyy.aria.core.scheduler.M3U8PeerTaskListener
import com.arialyy.aria.core.scheduler.NormalTaskListener
import com.arialyy.aria.core.scheduler.SubTaskListener
import com.arialyy.aria.core.task.DownloadTask
import com.google.common.util.concurrent.ListenableFuture
import com.lidroid.xutils.HttpUtils
import com.viz.tools.MD5Util
import com.viz.tools.TimeFormat
import com.viz.tools.apk.NetWorkUtils
import com.viz.tools.l
import jaygoo.library.m3u8downloader.M3U8Downloader
import jaygoo.library.m3u8downloader.M3U8DownloaderConfig
import jaygoo.library.m3u8downloader.OnM3U8DownloadListener
import jaygoo.library.m3u8downloader.bean.M3U8Task
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import viz.commonlib.download.VDownloader
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.eventbus.CommonInfoEvent
import viz.vplayer.eventbus.DownloadProgressEvent
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.room.Download
import viz.vplayer.ui.activity.MainActivity
import viz.vplayer.util.*
import java.math.BigDecimal
import kotlin.random.Random

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    private var mFuture: SettableFuture<Result>? = null
    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {
        mFuture = SettableFuture.create()
        val videoUrl = inputData.getString("videoUrl")
        val videoTitle = inputData.getString("videoTitle")
        val videoImgUrl = inputData.getString("videoImgUrl")
        val searchUrl = inputData.getString("searchUrl")
        val duration = inputData.getLong("duration", 0)
        var result: Result? = null
        if (videoUrl.isNullOrEmpty()) {
            result = Result.failure()
            mFuture?.set(result)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                l.d(network)
            }
            val netWorkType = NetWorkUtils.getNetWorkType(applicationContext)
            when (netWorkType) {
                NetWorkUtils.NETWORK_TYPE_WIFI -> {
                }
                NetWorkUtils.NETWORK_TYPE_UNKNOWN -> {
                }
                NetWorkUtils.NETWORK_TYPE_DISCONNECT -> {
                    l.d("无网络")
                    EventBus.getDefault().postSticky(NetEvent(false))
                    result = Result.failure(workDataOf("errMsg" to "无网络", "videoUrl" to videoUrl))
                    mFuture?.set(result)
                    return mFuture!!
                }
                else -> {
                    if (!BuildConfig.DEBUG) {
                        l.d("移动网络禁止下载")
                        EventBus.getDefault().postSticky(NetEvent(false))
                        result = Result.failure(
                            workDataOf(
                                "errMsg" to "移动网络禁止下载",
                                "videoUrl" to videoUrl
                            )
                        )
                        mFuture?.set(result)
                        return mFuture!!
                    }
                }
            }
            GlobalScope.launch {
                try {
                    val isConnect = NetUtil().netCheck()
                    EventBus.getDefault().postSticky(NetEvent(isConnect))
                    if (!isConnect) {
                        EventBus.getDefault().postSticky(
                            CommonInfoEvent(
                                true,
                                applicationContext.getString(R.string.network_invalid)
                            )
                        )
                        result =
                            Result.failure(
                                workDataOf(
                                    "errMsg" to applicationContext.getString(R.string.network_invalid),
                                    "videoUrl" to videoUrl
                                )
                            )
                        mFuture?.set(result)
                        return@launch
                    }
                    var download: Download? = null
                    Task.callInBackground {
                        l.start("bolts")
                        download = App.instance.db.downloadDao().getByUrl(videoUrl)
                        if (download == null) {
                            download = Download()
                            download!!.workId = id.toString()
                            download!!.videoUrl = videoUrl
                            download!!.videoTitle = videoTitle!!
                            download!!.videoImgUrl = videoImgUrl!!
                            download!!.searchUrl = searchUrl!!
                            download!!.duration = duration!!
                            download!!.notificationId =
                                "${TimeFormat.getCurrentTime("MMddHHmm")}${Random.nextInt(99)}".toInt()
                            App.instance.db.downloadDao().insertAll(download!!)
                        }
                        l.end("bolts")
                    }.continueWithEnd("下载数据记录")
                    val ft = UrlUtil.generatLocalFileNameAndPath(applicationContext, videoUrl, true)
                    val fileName = ft.first
                    val target = ft.second
                    var progressLast = 0.00f
                    val PROGRESS_MAX = 100
                    val notificationUtil = NotificationUtil(applicationContext)
                    notificationUtil.createNotificationChannel()
                    val GROUP_KEY_WORK_VIDEO = "viz.vplayer.WORK_VIDEO"
                    val summaryNotification = notificationUtil.createNotificationBuilder(
                        CHANNEL_ID_DOWNLOAD,
                        GROUP_KEY_WORK_VIDEO,
                        "more download",
                        "视频下载",
                        R.drawable.vplayer_logo,
                        isGroupSummary = true
                    ).build()
                    if (videoUrl.contains(".m3u8")) {

                        val vDownloader = VDownloader()
                        vDownloader.init(applicationContext)
                        vDownloader.apply {
                            onProgress = { percent ->
                                val pbd = BigDecimal(percent.toString())
                                val progress =
                                    pbd.divide(BigDecimal("0.01"), 2, BigDecimal.ROUND_HALF_UP).toFloat()
                                setProgressAsync(
                                    workDataOf(
                                        "progress" to progress,
                                        "uniqueName" to MD5Util.MD5(videoUrl),
                                        "videoUrl" to videoUrl
                                    )
                                )
                                val intent =
                                    Intent(applicationContext, MainActivity::class.java)
                                intent.putExtra("to", 1)
                                val text = "视频下载${videoUrl}"
                                val builder = notificationUtil.createNotificationBuilder(
                                    CHANNEL_ID_DOWNLOAD,
                                    GROUP_KEY_WORK_VIDEO,
                                    "0.00%",
                                    text,
                                    android.R.drawable.stat_sys_download,
                                    isBigText = true,
                                    bigText = text,
                                    clickable = true,
                                    intent = intent
                                )
                                EventBus.getDefault()
                                    .postSticky(
                                        DownloadProgressEvent(
                                            progress.toInt(),
                                            videoUrl
                                        )
                                    )
                                download!!.progress = progress.toInt()
                                if (download!!.progress == 100) {
                                    download!!.status = 1
                                }
                                Task.callInBackground {
                                    App.instance.db.downloadDao().updateALl(download!!)
                                }.continueWithEnd("保存下载进度")
                                l.i(progress.toString())
                                progressLast = progress
                                val notificationId = download!!.notificationId
                                NotificationManagerCompat.from(applicationContext).apply {
                                    // Issue the initial notification with zero progress
                                    builder.apply {
                                        setProgress(PROGRESS_MAX, progress.toInt(), true)
                                        setContentTitle("${progress}%-${notificationId}")
                                        notify(notificationId, build())
                                        notify(111111, summaryNotification)

                                        if (progress == 100f) {
                                            val text = "视频下载${videoUrl}"
                                            setContentText(text)
                                            // When done, update the notification one more time to remove the progress bar
                                            setContentTitle("下载完成")
                                            setStyle(
                                                NotificationCompat.BigTextStyle()
                                                    .bigText(text)
                                            )
                                            setProgress(0, 0, false)
                                            setSmallIcon(android.R.drawable.stat_sys_download_done)
                                            notify(notificationId, build())
                                        }
                                    }
                                }
                            }
                            onSuccess = { filePath ->
                                val intent =
                                    Intent(applicationContext, MainActivity::class.java)
                                intent.putExtra("to", 1)
                                val text = "视频下载${videoUrl}"
                                val builder = notificationUtil.createNotificationBuilder(
                                    CHANNEL_ID_DOWNLOAD,
                                    GROUP_KEY_WORK_VIDEO,
                                    "0.00%",
                                    text,
                                    android.R.drawable.stat_sys_download,
                                    isBigText = true,
                                    bigText = text,
                                    clickable = true,
                                    intent = intent
                                )
                                val notificationId = download!!.notificationId
                                NotificationManagerCompat.from(applicationContext).apply {
                                    builder.apply {
                                        setContentText(text)
                                        // When done, update the notification one more time to remove the progress bar
                                        setContentTitle("下载完成")
                                        setStyle(
                                            NotificationCompat.BigTextStyle()
                                                .bigText(text)
                                        )
                                        setProgress(0, 0, false)
                                        setSmallIcon(android.R.drawable.stat_sys_download_done)
                                        notify(notificationId, build())
                                    }
                                }
                                l.d("下载成功 $filePath")
                                Task.callInBackground {
                                    if (download != null) {
                                        download!!.status = 1
                                        download!!.progress = 100
                                        App.instance.db.downloadDao().updateALl(download!!)
                                    }
                                }.continueWithEnd("下载数据记录删除")
                                EventBus.getDefault()
                                    .postSticky(
                                        DownloadProgressEvent(
                                            100,
                                            videoUrl
                                        )
                                    )
                                result = Result.success(workDataOf("videoLocalPath" to filePath))
                                mFuture?.set(result)
                            }
                            //                        load("http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8")
//                        load("https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8")
//                        load("http://192.168.1.7/index.m3u8")
//                        load("http://192.168.1.7/index1.m3u8")
                            load(videoUrl)
                            start()
                        }
//                    M3U8DownloaderConfig
//                        .build(applicationContext)
//                        .setSaveDir(FileUtil.getPath(applicationContext) + "/m3u8Video")
//                    M3U8Downloader.getInstance().download(videoUrl)
//                    M3U8Downloader.getInstance()
//                        .setOnM3U8DownloadListener(object : OnM3U8DownloadListener() {
//                            override fun onDownloadSuccess(task: M3U8Task) {
//                                super.onDownloadSuccess(task)
//                                Task.callInBackground {
//                                    if (download != null) {
//                                        download!!.status = 1
//                                        download!!.progress = 100
//                                        App.instance.db.downloadDao().updateALl(download!!)
//                                    }
//                                }.continueWithEnd("下载数据记录删除")
//                                result =
//                                    Result.success(
//                                        workDataOf(
//                                            "videoLocalPath" to task.m3U8.m3u8FilePath,
//                                            "videoUrl" to videoUrl
//                                        )
//                                    )
//                        EventBus.getDefault()
//                            .postSticky(
//                                DownloadProgressEvent(
//                                    100,
//                                    videoUrl
//                                )
//                            )
//                                mFuture?.set(result)
//                            }
//
//                            override fun onDownloadProgress(task: M3U8Task) {
//                                super.onDownloadProgress(task)
//                                val progress = task.progress
//                                if (progress - progressLast > 1 || progress == 100f || progress == 0f) {
//                                    setProgressAsync(
//                                        workDataOf(
//                                            "progress" to progress,
//                                            "uniqueName" to MD5Util.MD5(videoUrl),
//                                            "videoUrl" to videoUrl,
//                                            "url" to task.url
//                                        )
//                                    )
//                                    val intent =
//                                        Intent(applicationContext, MainActivity::class.java)
//                                    intent.putExtra("to", 1)
//                                    val text = "视频下载${task.url}"
//                                    val builder = notificationUtil.createNotificationBuilder(
//                                        CHANNEL_ID_DOWNLOAD,
//                                        GROUP_KEY_WORK_VIDEO,
//                                        "0.00%",
//                                        text,
//                                        android.R.drawable.stat_sys_download,
//                                        isBigText = true,
//                                        bigText = text,
//                                        clickable = true,
//                                        intent = intent
//                                    )
//                                    EventBus.getDefault()
//                                        .postSticky(
//                                            DownloadProgressEvent(
//                                                progress.toInt(),
//                                                videoUrl
//                                            )
//                                        )
//                                    download!!.progress = progress.toInt()
//                                    if (download!!.progress == 100) {
//                                        download!!.status = 1
//                                    }
//                                    Task.callInBackground {
//                                        App.instance.db.downloadDao().updateALl(download!!)
//                                    }.continueWithEnd("保存下载进度")
//                                    l.i(progress.toString())
//                                    progressLast = progress
//                                    val notificationId = download!!.notificationId
//                                    NotificationManagerCompat.from(applicationContext).apply {
//                                        // Issue the initial notification with zero progress
//                                        builder.apply {
//                                            setProgress(PROGRESS_MAX, progress.toInt(), true)
//                                            setContentTitle("${progress}%-${notificationId}")
//                                            notify(notificationId, build())
//                                            notify(111111, summaryNotification)
//
//                                            // Do the job here that tracks the progress.
//                                            // Usually, this should be in a
//                                            // worker thread
//                                            // To show progress, update PROGRESS_CURRENT and update the notification with:
//                                            // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
//                                            // notificationManager.notify(notificationId, builder.build());
//
//                                            if (progress == 100f) {
//                                                val text = "视频下载${task.url}"
//                                                setContentText(text)
//                                                // When done, update the notification one more time to remove the progress bar
//                                                setContentTitle("下载完成")
//                                                setStyle(
//                                                    NotificationCompat.BigTextStyle()
//                                                        .bigText(text)
//                                                )
//                                                setProgress(0, 0, false)
//                                                setSmallIcon(android.R.drawable.stat_sys_download_done)
//                                                notify(notificationId, build())
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            override fun onDownloadError(task: M3U8Task, errorMsg: Throwable) {
//                                super.onDownloadError(task, errorMsg)
//                                errorMsg.printStackTrace()
//                                l.e(
//                                    "AriaCallback",
//                                    String.format(
//                                        "onTaskFailWithEx key:%s errMsg:%s",
//                                        task.url,
//                                        errorMsg.message
//                                    )
//                                )
//                                val intent =
//                                    Intent(applicationContext, MainActivity::class.java)
//                                intent.putExtra("to", 1)
//                                val text = "视频下载失败${task.url}"
//                                val builder = notificationUtil.createNotificationBuilder(
//                                    CHANNEL_ID_DOWNLOAD,
//                                    GROUP_KEY_WORK_VIDEO,
//                                    "0.00%",
//                                    text,
//                                    android.R.drawable.stat_notify_error,
//                                    isBigText = true,
//                                    bigText = text,
//                                    clickable = true,
//                                    intent = intent
//                                )
//                                val notificationId = download!!.notificationId
//                                NotificationManagerCompat.from(applicationContext).apply {
//                                    // Issue the initial notification with zero progress
//                                    builder.apply {
//                                            val text = "视频下载失败${task.url}"
//                                            setContentText(text)
//                                            // When done, update the notification one more time to remove the progress bar
//                                            setContentTitle("下载完成")
//                                            setStyle(
//                                                NotificationCompat.BigTextStyle()
//                                                    .bigText(text)
//                                            )
//                                            setProgress(0, 0, false)
//                                            setSmallIcon(android.R.drawable.stat_notify_error)
//                                            notify(notificationId, build())
//                                    }
//                                }
//                            }
//                        })
//
//                    val option = M3U8VodOption() // 创建m3u8点播文件配置
//                        .apply {
//                            merge(true)
//                            setVodTsUrlConvert { m3u8Url, tsUrls ->
//                                // 转换ts文件的url地址
//                                val parentUrl = m3u8Url.substringBeforeLast("/") + "/"
//                                val newUrls = ArrayList<String>()
//                                tsUrls.forEachIndexed { index, s ->
//                                    s?.apply {
//                                        newUrls.add(parentUrl + this)
//                                    }
//                                }
//                                newUrls // 返回有效的ts文件url集合
//                            }
//                            setBandWidthUrlConverter {
//                                videoUrl.substringBeforeLast("/") + "/" + it
//                            }
//                        }
//                    val taskId = Aria.download(applicationContext)
//                        .load(videoUrl) // 设置点播文件下载地址
//                        .setFilePath(target) // 设置点播文件保存路径
//                        .ignoreFilePathOccupy()
//                        .m3u8VodOption(option)   // 调整下载模式为m3u8点播
//                        .create()
//
//                        Aria.download(this)
//                            .setM3U8PeerTaskListener(
//                                videoUrl,
//                                TaskEnum.M3U8_PEER,
//                                object : M3U8PeerTaskListener() {
//                                    override fun onPeerStart(
//                                        m3u8Url: String,
//                                        peerPath: String,
//                                        peerIndex: Int
//                                    ) {
//                                        super.onPeerStart(m3u8Url, peerPath, peerIndex)
//                                        l.d(
//                                            "AriaCallback",
//                                            String.format(
//                                                "onPeerStart m3u8Url:%s\npeerPath:%s\npeerIndex:%d",
//                                                m3u8Url,
//                                                peerPath,
//                                                peerIndex
//                                            )
//                                        )
//                                    }
//
//                                    override fun onPeerComplete(
//                                        m3u8Url: String,
//                                        peerPath: String,
//                                        peerIndex: Int
//                                    ) {
//                                        super.onPeerComplete(m3u8Url, peerPath, peerIndex)
//                                        l.d(
//                                            "AriaCallback",
//                                            String.format(
//                                                "onPeerComplete m3u8Url:%s\npeerPath:%s\npeerIndex:%d",
//                                                m3u8Url,
//                                                peerPath,
//                                                peerIndex
//                                            )
//                                        )
//                                    }
//
//                                    override fun onPeerFail(
//                                        m3u8Url: String,
//                                        peerPath: String,
//                                        peerIndex: Int
//                                    ) {
//                                        super.onPeerFail(m3u8Url, peerPath, peerIndex)
//                                        l.d(
//                                            "AriaCallback",
//                                            String.format(
//                                                "onPeerFail m3u8Url:%s\npeerPath:%s\npeerIndex:%d",
//                                                m3u8Url,
//                                                peerPath,
//                                                peerIndex
//                                            )
//                                        )
//                                    }
//                                })
//
//                        Aria.download(this).setNormalTaskListener(
//                            videoUrl,
//                            TaskEnum.DOWNLOAD,
//                            object : NormalTaskListener<DownloadTask>() {
//                                override fun onWait(task: DownloadTask) {
//                                    super.onWait(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onWait key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onPre(task: DownloadTask) {
//                                    super.onPre(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onPre key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskPre(task: DownloadTask) {
//                                    super.onTaskPre(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskPre key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskResume(task: DownloadTask) {
//                                    super.onTaskResume(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskResume key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskStart(task: DownloadTask) {
//                                    super.onTaskStart(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskStart key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskStop(task: DownloadTask) {
//                                    super.onTaskStop(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskStop key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskCancel(task: DownloadTask) {
//                                    super.onTaskCancel(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskCancel key:%s", task.key)
//                                    )
//                                }
//
//                                override fun onTaskFail(
//                                    task: DownloadTask,
//                                    e: Exception
//                                ) {
//                                    e?.apply {
//                                        task?.apply {
//                                            super.onTaskFail(task, e)
//                                            e.printStackTrace()
//                                            l.e(
//                                                "AriaCallback",
//                                                String.format("onTaskFailWithEx key:%s", task.key)
//                                            )
//                                        }
//                                    }
//                                }
//
//                                override fun onTaskComplete(task: DownloadTask) {
//                                    super.onTaskComplete(task)
//                                    if (task.key != videoUrl) {
//                                        return
//                                    }
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskComplete key:%s", task.key)
//                                    )
//                                    Task.callInBackground {
//                                        if (download != null) {
//                                            download!!.status = 1
//                                            download!!.progress = 100
//                                            App.instance.db.downloadDao().updateALl(download!!)
//                                        }
//                                    }.continueWithEnd("下载数据记录删除")
//                        EventBus.getDefault()
//                            .postSticky(
//                                DownloadProgressEvent(
//                                    100,
//                                    videoUrl
//                                )
//                            )
//                                    result =
//                                        Result.success(
//                                            workDataOf(
//                                                "videoLocalPath" to task.filePath,
//                                                "videoUrl" to videoUrl
//                                            )
//                                        )
//                                    mFuture?.set(result)
//                                }
//
//                                override fun onTaskRunning(task: DownloadTask) {
//                                    super.onTaskRunning(task)
//                                    if (task.key != videoUrl) {
//                                        return
//                                    }
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onTaskRunning key:%s", task.key)
//                                    )
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format(
//                                            "onTaskRunning filePath:%s speed:%s percent:%d",
//                                            task.filePath,
//                                            task.entity.convertSpeed,
//                                            task.entity.percent
//                                        )
//                                    )
//                                    val progress = task.entity.percent.toFloat()
//                                    if (progress - progressLast > 1 || progress == 100f || progress == 0f) {
//                                        setProgressAsync(
//                                            workDataOf(
//                                                "progress" to progress,
//                                                "uniqueName" to MD5Util.MD5(videoUrl),
//                                                "videoUrl" to videoUrl,
//                                                "url" to task.key
//                                            )
//                                        )
//                                        val intent =
//                                            Intent(applicationContext, MainActivity::class.java)
//                                        intent.putExtra("to", 1)
//                                        val text = "视频下载${task.key}"
//                                        val builder = notificationUtil.createNotificationBuilder(
//                                            CHANNEL_ID_DOWNLOAD,
//                                            GROUP_KEY_WORK_VIDEO,
//                                            "0.00%",
//                                            text,
//                                            android.R.drawable.stat_sys_download,
//                                            isBigText = true,
//                                            bigText = text,
//                                            clickable = true,
//                                            intent = intent
//                                        )
//                                        EventBus.getDefault()
//                                            .postSticky(
//                                                DownloadProgressEvent(
//                                                    progress.toInt(),
//                                                    videoUrl
//                                                )
//                                            )
//                                        download!!.progress = progress.toInt()
//                                        if (download!!.progress == 100) {
//                                            download!!.status = 1
//                                        }
//                                        Task.callInBackground {
//                                            App.instance.db.downloadDao().updateALl(download!!)
//                                        }.continueWithEnd("保存下载进度")
//                                        l.i(progress.toString())
//                                        progressLast = progress
//                                        val notificationId = download!!.notificationId
//                                        NotificationManagerCompat.from(applicationContext).apply {
//                                            // Issue the initial notification with zero progress
//                                            builder.apply {
//                                                setProgress(PROGRESS_MAX, progress.toInt(), true)
//                                                setContentTitle("${progress}%-${notificationId}")
//                                                notify(notificationId, build())
//                                                notify(111111, summaryNotification)
//
//                                                // Do the job here that tracks the progress.
//                                                // Usually, this should be in a
//                                                // worker thread
//                                                // To show progress, update PROGRESS_CURRENT and update the notification with:
//                                                // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
//                                                // notificationManager.notify(notificationId, builder.build());
//
//                                                if (progress == 100f) {
//                                                    val text = "视频下载${task.key}"
//                                                    setContentText(text)
//                                                    // When done, update the notification one more time to remove the progress bar
//                                                    setContentTitle("下载完成")
//                                                    setStyle(
//                                                        NotificationCompat.BigTextStyle()
//                                                            .bigText(text)
//                                                    )
//                                                    setProgress(0, 0, false)
//                                                    setSmallIcon(android.R.drawable.stat_sys_download_done)
//                                                    notify(notificationId, build())
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//
//                                override fun onNoSupportBreakPoint(task: DownloadTask) {
//                                    super.onNoSupportBreakPoint(task)
//                                    l.d(
//                                        "AriaCallback",
//                                        String.format("onNoSupportBreakPoint key:%", task.key)
//                                    )
//                                }
//                            })
//
//                        Aria.download(applicationContext)
//                            .setSubTaskListener(
//                                videoUrl,
//                                TaskEnum.DOWNLOAD_GROUP_SUB,
//                                object : SubTaskListener<DownloadTask, AbsNormalEntity>() {
//                                    override fun onSubTaskCancel(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskCancel(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskRunning(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskRunning(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskComplete(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskComplete(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskPre(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskPre(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskStart(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskStart(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskFail(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskFail(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskFail(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity,
//                                        e: java.lang.Exception
//                                    ) {
//                                        super.onSubTaskFail(task, subTask, e)
//                                        l.d()
//                                    }
//
//                                    override fun onSubTaskStop(
//                                        task: DownloadTask,
//                                        subTask: AbsNormalEntity
//                                    ) {
//                                        super.onSubTaskStop(task, subTask)
//                                        l.d()
//                                    }
//
//                                    override fun onNoSupportBreakPoint(task: DownloadTask) {
//                                        super.onNoSupportBreakPoint(task)
//                                        l.d()
//                                    }
//                                })
                    } else {
                        val http = HttpUtils()
                        http.download(
                            applicationContext,
                            videoUrl,
                            suffix = "",
                            onResult = { filePath ->
                                l.d("下载成功 $filePath")
                                Task.callInBackground {
                                    if (download != null) {
                                        download!!.status = 1
                                        download!!.progress = 100
                                        App.instance.db.downloadDao().updateALl(download!!)
                                    }
                                }.continueWithEnd("下载数据记录删除")
                                EventBus.getDefault()
                                    .postSticky(
                                        DownloadProgressEvent(
                                            100,
                                            videoUrl
                                        )
                                    )
                                result = Result.success(workDataOf("videoLocalPath" to filePath))
                                mFuture?.set(result)
                            },
                            onError = { httpException, errMsg ->
                                httpException.printStackTrace()
                                l.e("下载失败")
                                result = Result.failure(
                                    workDataOf(
                                        "errMsg" to errMsg,
                                        "videoUrl" to videoUrl
                                    )
                                )
                                mFuture?.set(result)
                            },
                            onProgress = { progress, current, total ->
                                if (progress - progressLast > 1 || progress == 100f || progress == 0f) {
                                    setProgressAsync(
                                        workDataOf(
                                            "progress" to progress,
                                            "uniqueName" to MD5Util.MD5(videoUrl),
                                            "videoUrl" to videoUrl
                                        )
                                    )
                                    val intent =
                                        Intent(applicationContext, MainActivity::class.java)
                                    intent.putExtra("to", 1)
                                    val text = "视频下载${videoUrl}"
                                    val builder = notificationUtil.createNotificationBuilder(
                                        CHANNEL_ID_DOWNLOAD,
                                        GROUP_KEY_WORK_VIDEO,
                                        "0.00%",
                                        text,
                                        android.R.drawable.stat_sys_download,
                                        isBigText = true,
                                        bigText = text,
                                        clickable = true,
                                        intent = intent
                                    )
                                    EventBus.getDefault()
                                        .postSticky(
                                            DownloadProgressEvent(
                                                progress.toInt(),
                                                videoUrl
                                            )
                                        )
                                    download!!.progress = progress.toInt()
                                    if (download!!.progress == 100) {
                                        download!!.status = 1
                                    }
                                    Task.callInBackground {
                                        App.instance.db.downloadDao().updateALl(download!!)
                                    }.continueWithEnd("保存下载进度")
                                    l.i(progress.toString())
                                    progressLast = progress
                                    val notificationId = download!!.notificationId
                                    NotificationManagerCompat.from(applicationContext).apply {
                                        // Issue the initial notification with zero progress
                                        builder.apply {
                                            setProgress(PROGRESS_MAX, progress.toInt(), true)
                                            setContentTitle("${progress}%-${notificationId}")
                                            notify(notificationId, build())
                                            notify(111111, summaryNotification)

                                            // Do the job here that tracks the progress.
                                            // Usually, this should be in a
                                            // worker thread
                                            // To show progress, update PROGRESS_CURRENT and update the notification with:
                                            // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                                            // notificationManager.notify(notificationId, builder.build());

                                            if (progress.toFloat() == 100f) {
                                                val text = "视频下载${fileName}"
                                                setContentText(text)
                                                // When done, update the notification one more time to remove the progress bar
                                                setContentTitle("下载完成")
                                                setStyle(
                                                    NotificationCompat.BigTextStyle()
                                                        .bigText(text)
                                                )
                                                setProgress(0, 0, false)
                                                setSmallIcon(android.R.drawable.stat_sys_download_done)
                                                notify(notificationId, build())
                                            }
                                        }
                                    }
                                }
                            })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return mFuture!!
    }
}