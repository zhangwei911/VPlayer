package viz.vplayer.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import com.lidroid.xutils.HttpUtils
import viz.vplayer.util.download

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    private var mFuture: SettableFuture<Result>? = null
    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {
        mFuture = SettableFuture.create()
        val videoUrl = inputData.getString("videoUrl")
        var result: Result? = null
        if (videoUrl.isNullOrEmpty()) {
            result = Result.failure()
            mFuture?.set(result)
        } else {
            if (videoUrl.contains(".m3u8")) {

            } else {
                val http = HttpUtils()
                http.download(applicationContext, videoUrl, suffix = "", onResult = { filePath ->
                    result = Result.success(workDataOf("videoLocalPath" to filePath))
                    mFuture?.set(result)
                }, onError = { httpException, errMsg ->
                    httpException.printStackTrace()
                    result = Result.failure(workDataOf("errMsg" to errMsg))
                    mFuture?.set(result)
                }, onProgress = { progress ->
                    setProgressAsync(workDataOf("progress" to progress))
                })
            }
        }
        return mFuture!!
    }
}