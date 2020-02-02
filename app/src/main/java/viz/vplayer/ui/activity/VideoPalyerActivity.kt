package viz.vplayer.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.BounceInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import bolts.Task
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import com.viz.tools.MD5Util
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_video.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.vplayer.R
import viz.vplayer.adapter.SelectEpisodesAdapter
import viz.vplayer.bean.HtmlBean
import viz.vplayer.bean.VideoInfoBean
import viz.vplayer.eventbus.VideoEvent
import viz.vplayer.room.Episode
import viz.vplayer.room.VideoId
import viz.vplayer.room.VideoInfo
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.video.FloatPlayerView
import viz.vplayer.video.FloatWindow
import viz.vplayer.video.MoveType
import viz.vplayer.video.Screen
import viz.vplayer.vm.MainVM
import viz.vplayer.vm.VideoVM
import viz.vplayer.worker.DownloadWorker
import java.util.concurrent.TimeUnit


class VideoPalyerActivity : BaseActivity() {
    private val videoVM: VideoVM by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(VideoVM::class.java)
    }
    private val mainVM: MainVM by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MainVM::class.java)
    }
    private var episodes: MutableList<String>? = null
    private var html: HtmlBean? = null
    private var title = ""
    private var img = ""
    private var searchUrl = ""
    var index = 0

    override fun getContentViewId(): Int = R.layout.activity_video
    override fun isFullScreen(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        videoVM.play.observe(this, Observer { videoInfoBean ->
            var url = videoInfoBean.url
            l.d("视频地址:$url")
            loadingView_player.visibility = View.GONE
            gsyVideoPLayer.setUp(url, true, videoInfoBean.title)
            Task.callInBackground {
                val vi = app.db.videoInfoDao().getByUrl(videoInfoBean.url)
                l.d(vi)
                runOnUiThread {
                    if (vi != null) {
                        gsyVideoPLayer.seekOnStart = vi.currentPosition.toLong()
                    }
                    gsyVideoPLayer.startPlayLogic()
                }
            }.continueWith { task ->
                when {
                    task.isCancelled -> {
                        l.i("任务取消")
                    }
                    task.isFaulted -> {
                        val error = task.error
                        l.e("任务失败 $error")
                        error.printStackTrace()
                    }
                    else -> {
                        l.i("任务成功")
                    }
                }
                return@continueWith null
            }
        })
        mainVM.errorInfo.observe(this, Observer { errorInfo ->
            loadingView_player.visibility = View.GONE
            Toast.showLong(this, errorInfo.errMsg)
        })
        initVideo()
        val url = intent.getStringExtra("url")
        title = intent.getStringExtra("title") ?: ""
        img = intent.getStringExtra("img") ?: ""
        searchUrl = intent.getStringExtra("searchUrl") ?: ""
        val duration = intent.getLongExtra("duration", 0)
        episodes = intent.getSerializableExtra("episodes") as MutableList<String>
        html = intent.getParcelableExtra("html")
        gsyVideoPLayer.selectEpisodes = {
            episodes!!
        }
        if (url.isNullOrEmpty() || title.isNullOrEmpty()) {
            Toast.show(this, "参数错误")
            return
        }
        videoVM.play.postValue(
            VideoInfoBean(
                url, title + if (episodes!![0].endsWith(".m3u8")) {
                    "第1集"
                } else {
                    ""
                }, duration
            )
        )
        mainVM.play.observe(this, Observer { videoInfoBean ->
            videoVM.play.postValue(videoInfoBean)
        })
    }

    private fun initVideo() {
        //全屏
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
        gsyVideoPLayer.titleTextView.visibility = View.VISIBLE
        gsyVideoPLayer.setIsTouchWiget(true)
        gsyVideoPLayer.isIfCurrentIsFullscreen = true
        gsyVideoPLayer.isNeedLockFull = true
        gsyVideoPLayer.episodesClick = {
            it.addOnItemTouchListener(
                RecyclerItemClickListener(
                    this,
                    it,
                    object :
                        RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                            index = position
                            if (episodes!![position].endsWith("m3u8")) {
                                videoVM.play.postValue(
                                    VideoInfoBean(
                                        episodes!![position],
                                        title + "第${position + 1}集",
                                        0
                                    )
                                )
                            } else {
                                loadingView_player.visibility = View.VISIBLE
                                mainVM.getVideoInfo(
                                    episodes!![position],
                                    html!!.videoHtmlResultBean,
                                    img
                                )
                            }
                            it.visibility = View.GONE
                        }

                        override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                        }

                        override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                        }

                    })
            )
        }
        gsyVideoPLayer.backButton.setOnClickListener {
            finish()
        }
        gsyVideoPLayer.fullscreenButton.visibility = View.GONE
        gsyVideoPLayer.setGSYVideoProgressListener { progress, secProgress, currentPosition, duration ->
            //            l.df(progress, secProgress, currentPosition, duration)
        }
        gsyVideoPLayer.onAutoCompletion = { recyclerView ->
            val childView = recyclerView.getChildAt(index + 1)
            val vh = recyclerView.getChildViewHolder(childView) as SelectEpisodesAdapter.ViewHolder
            vh.autoClick()
        }
        gsyVideoPLayer.customVisibility = { visibility ->
            imageButton_float.visibility = visibility
        }
        gsyVideoPLayer.customClick = {
            when (it) {
                R.id.textView_download -> {
                    val url = gsyVideoPLayer.getOriginalUrl()
                    download(url)
                }
            }
        }
        imageButton_float.setOnClickListener {
            float(videoVM.play.value!!.url, videoVM.play.value!!.title)
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }
    }

    private fun float(url: String, title: String) {
        if (FloatWindow.get() != null) {
            return
        }
        val floatPlayerView = FloatPlayerView(applicationContext)
        floatPlayerView.url = url
        floatPlayerView.title = title
        floatPlayerView.videoPlayer!!.backToFull = {
            val intent = Intent(this, VideoPalyerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra("currentPos", floatPlayerView.videoPlayer!!.currentPositionWhenPlaying)
            EventBus.getDefault()
                .postSticky(VideoEvent(floatPlayerView.videoPlayer!!.currentPositionWhenPlaying))
            startActivity(intent)
            floatPlayerView.videoPlayer?.onVideoPause()
            FloatWindow.destroy()
        }
        floatPlayerView.videoPlayer?.setUp(url, true, title)
        floatPlayerView.videoPlayer?.seekOnStart =
            gsyVideoPLayer.currentPositionWhenPlaying.toLong()
        floatPlayerView.videoPlayer?.startPlayLogic()
        FloatWindow
            .with(applicationContext)
            .setView(floatPlayerView)
            .setWidth(Screen.height, 0.75f)
            .setHeight(Screen.height, 0.5f)
            .setX(Screen.width, 0.8f)
            .setY(Screen.height, 0.3f)
            .setMoveType(MoveType.slide)
            .setFilter(false)
            .setMoveStyle(500, BounceInterpolator())
            .build()
        FloatWindow.get().show()
    }

    override fun onPause() {
        super.onPause()
        Task.callInBackground {
            val vi = app.db.videoInfoDao().getByUrl(videoVM.play.value!!.url)
            l.d(vi)
            if (vi == null) {
                val videoInfo = VideoInfo()
                videoInfo.videoUrl = videoVM.play.value!!.url
                videoInfo.currentPosition = gsyVideoPLayer.currentPositionWhenPlaying
                videoInfo.videoTitle = title
                videoInfo.videoImgUrl = img
                videoInfo.duration = gsyVideoPLayer.duration
                videoInfo.searchUrl = searchUrl
                app.db.videoInfoDao().insertAll(videoInfo)
                val vin = app.db.videoInfoDao().getByUrl(videoInfo.videoUrl)
                val episodeList = mutableListOf<Episode>()
                episodes?.forEachIndexed { index, s ->
                    val episode = Episode()
                    episode.urlIndex = index
                    episode.url = s
                    episode.videoId = vin.id
                    episodeList.add(episode)
                }
                app.db.episodeDao().deleteByVid(VideoId(vin.id))
                app.db.episodeDao().insertAll(episodeList)
            } else {
                runOnUiThread {
                    vi.currentPosition = gsyVideoPLayer.currentPositionWhenPlaying
                    Task.callInBackground {
                        app.db.videoInfoDao().updateALl(vi)
                    }.continueWith { task ->
                        when {
                            task.isCancelled -> {
                                l.i("任务取消")
                            }
                            task.isFaulted -> {
                                val error = task.error
                                l.e("任务失败 $error")
                                error.printStackTrace()
                            }
                            else -> {
                                l.i("任务成功")
                            }
                        }
                        return@continueWith null
                    }
                }
            }
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("任务成功")
                }
            }
            return@continueWith null
        }
        if (FloatWindow.get() == null) {
            gsyVideoPLayer.onVideoPause()
        }
    }

    private fun download(videoUrl: String) {
        val constraintsBuilder = Constraints.Builder()
//            .setRequiredNetworkType(
//                NetworkType.UNMETERED
//            )// 网络状态
//            .setRequiresBatteryNotLow(true)                 // 不在电量不足时执行
//            .setRequiresCharging(true)                      // 在充电时执行
//            .setRequiresStorageNotLow(true)                 // 不在存储容量不足时执行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            constraintsBuilder.setRequiresDeviceIdle(true)  // 在待机状态下执行
        }
        val uniqueName = MD5Util.MD5(videoUrl)
        val wis = WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWork(uniqueName).get()
        if (wis.isNullOrEmpty()) {
            val constraints = constraintsBuilder
                .build()
            val downloadWorkerRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf("videoUrl" to videoUrl))
                .setConstraints(constraints)
                .keepResultsForAtLeast(1, TimeUnit.HOURS)//设置任务的保存时间
                .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, downloadWorkerRequest)
        }

        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData(uniqueName)
            .observe(this, Observer { workInfoList ->
                workInfoList.forEachIndexed { index, workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                l.i("下载成功")
                            }
                            WorkInfo.State.FAILED -> {
                                l.e(workInfo.outputData.getString("errMsg"))
                            }
                            WorkInfo.State.RUNNING -> {
                                l.d("下载进度:" + workInfo.progress.getFloat("progress", 0.00f))
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        gsyVideoPLayer.onVideoResume()
        val cPos = intent.getIntExtra("currentPos", -1)
        if (cPos != -1) {
            gsyVideoPLayer.seekTo(cPos.toLong())
            gsyVideoPLayer.startPlayLogic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.instance().releaseMediaPlayer()
        /**
         * 这里在返回主页的时候销毁了，因为不想和DEMO中其他页面冲突
         */
        FloatWindow.destroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        if (gsyVideoPLayer.currentState == GSYVideoView.CURRENT_STATE_PREPAREING || gsyVideoPLayer.currentState == GSYVideoView.CURRENT_STATE_ERROR) {
            super.onBackPressed()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun videoEvent(videoEvent: VideoEvent) {
        gsyVideoPLayer.seekOnStart = videoEvent.currentPosition.toLong()
    }
}