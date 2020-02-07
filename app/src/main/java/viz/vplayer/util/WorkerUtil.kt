package viz.vplayer.util

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.viz.tools.MD5Util
import com.viz.tools.l
import viz.vplayer.BuildConfig
import viz.vplayer.worker.DownloadWorker
import java.util.concurrent.TimeUnit

object WorkerUtil {
    fun startWorker(
        videoUrl: String,
        videoTitle: String,
        videoImgUrl: String,
        searchUrl: String,
        duration: Long,
        appContext: Context,
        lo: LifecycleOwner
    ) {
        val uniqueName = MD5Util.MD5(videoUrl)
        val wis = WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWork(uniqueName).get()
        wis.forEach {
            if (it.state == WorkInfo.State.FAILED) {
                WorkManager.getInstance(appContext).cancelWorkById(it.id)
                wis.remove(it)
            }
        }
        if (wis.isNullOrEmpty()) {
            val constraintsBuilder = Constraints.Builder().apply {
                if (!BuildConfig.DEBUG) {
                    setRequiredNetworkType(NetworkType.UNMETERED)// 网络状态
                    setRequiresBatteryNotLow(true)                 // 不在电量不足时执行
//                setRequiresCharging(true)                      // 在充电时执行
                    setRequiresStorageNotLow(true)                 // 不在存储容量不足时执行
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    setRequiresDeviceIdle(true)  // 在待机状态下执行
//                }
                }
            }
            val constraints = constraintsBuilder
                .build()
            val downloadWorkerRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        "videoUrl" to videoUrl,
                        "videoTitle" to videoTitle,
                        "videoImgUrl" to videoImgUrl,
                        "searchUrl" to searchUrl,
                        "duration" to duration
                    )
                )
                .setConstraints(constraints)
                .keepResultsForAtLeast(1, TimeUnit.HOURS)//设置任务的保存时间
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                    uniqueName,
                    ExistingWorkPolicy.KEEP,
                    downloadWorkerRequest
                )
        }
        WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWorkLiveData(uniqueName)
            .observe(lo, Observer { workInfoList ->
                workInfoList.forEachIndexed { index, workInfo ->
                    if (workInfo != null) {
                        l.d(workInfo.state)
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                l.i("下载成功")
                            }
                            WorkInfo.State.FAILED -> {
                                l.e(workInfo)
                                l.e(workInfo.outputData.getString("errMsg"))
                            }
                            WorkInfo.State.RUNNING -> {
                                val progress = workInfo.progress.getFloat("progress", 0.00f)
                                l.d("[${uniqueName}]下载进度:$progress")
                            }
                            else -> {
                                l.i(workInfo.state)
                            }
                        }
                    } else {
                        l.e("workInfo == null")
                    }
                }
            })
    }

    fun isWorking(videoUrl: String, appContext: Context): Boolean {
        val uniqueName = MD5Util.MD5(videoUrl)
        val wis = WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWork(uniqueName).get()
        return wis.isNotEmpty()
    }
}