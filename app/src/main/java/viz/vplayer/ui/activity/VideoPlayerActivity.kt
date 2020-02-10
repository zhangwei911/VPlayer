package viz.vplayer.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.animation.BounceInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_video.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.commonlib.util.MyObserver
import viz.vplayer.R
import viz.vplayer.adapter.SelectEpisodesAdapter
import viz.vplayer.bean.EpisodeListBean
import viz.vplayer.bean.HtmlBean
import viz.vplayer.bean.VideoInfoBean
import viz.vplayer.dagger2.MyObserverModule
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.eventbus.VideoEvent
import viz.vplayer.room.Episode
import viz.vplayer.room.VideoId
import viz.vplayer.room.VideoInfo
import viz.vplayer.util.*
import viz.vplayer.video.FloatPlayerView
import viz.vplayer.video.FloatWindow
import viz.vplayer.video.MoveType
import viz.vplayer.video.Screen
import viz.vplayer.vm.MainVM
import viz.vplayer.vm.VideoVM
import java.io.File
import javax.inject.Inject


class VideoPlayerActivity : BaseActivity() {
    @Inject
    lateinit var mo: MyObserver
    private val videoVM: VideoVM by lazy {
        ViewModelProvider(this).get(VideoVM::class.java)
    }
    private val mainVM: MainVM by lazy {
        ViewModelProvider(this).get(MainVM::class.java)
    }
    private var episodes: MutableList<String>? = null
    private var html: HtmlBean? = null
    private var title = ""
    private var img = ""
    private var searchUrl = ""
    private var duration = 0L
    var index = 0
    private var isWifi = true

    override fun getContentViewId(): Int = R.layout.activity_video
    override fun isFullScreen(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent!!.videoPlayerActivitySubcomponentBuilder()
            .myObserverModule(MyObserverModule(lifecycle, javaClass.name))
            .create(this)
            .inject(this)
        EventBus.getDefault().register(this)
        videoVM.play.observe(this, Observer { videoInfoBean ->
            videoInfoBean?.let {
                var url = videoInfoBean.url
                val urlUTF8 = UrlUtil.format(url)
                val ft = UrlUtil.generatLocalFileNameAndPath(this, url, true)
                val fileName = ft.first
                val target = ft.second
                val videoFile = File(target).apply {
                    url = if (exists()) {
                        Toast.show("已缓存,播放离线视频")
                        "file://$target"
                    } else {
                        urlUTF8
                    }
                }
                l.d("视频地址:$url")
                loadingView_player.visibility = View.GONE
                title = videoInfoBean.title
                duration = it.duration
                if (!title.contains(Regex("第[0-9]{1,3}集"))) {
                    title += "第${index + 1}集"
                }
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
                }.continueWithEnd("播放")
            }
        })
        mainVM.play.observe(this, Observer { videoInfoBean ->
            videoInfoBean?.let {
                videoVM.play.postValue(videoInfoBean)
            }
        })
        mainVM.errorInfo.observe(this, Observer { errorInfo ->
            errorInfo?.let {
                loadingView_player.visibility = View.GONE
                Toast.showLong(this, errorInfo.errMsg)
            }
        })
        mainVM.webView.observe(this, Observer {
            webView_for_get_url_player.loadUrl(it)
        })
        initVideo()
        var url = intent.getStringExtra("url")
        title = intent.getStringExtra("title") ?: ""
        img = intent.getStringExtra("img") ?: ""
        searchUrl = intent.getStringExtra("searchUrl") ?: ""
        duration = intent.getLongExtra("duration", 0)
        episodes = intent.getSerializableExtra("episodes") as MutableList<String>
        html = intent.getParcelableExtra("html")
        gsyVideoPLayer.selectEpisodes = {
            val episodeListBeans = mutableListOf<EpisodeListBean>()
            episodes?.forEachIndexed { index, s ->
                val elb = EpisodeListBean()
                elb.index = index
                elb.url = s
                episodeListBeans.add(elb)
            }
            episodeListBeans
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
    }

    private fun initVideo() {
        //全屏
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
        gsyVideoPLayer.run {
            titleTextView.visibility = View.VISIBLE
            setIsTouchWiget(true)
            isIfCurrentIsFullscreen = true
            isNeedLockFull = true
            episodesClick = { rv, adapter ->
                rv.addOnItemTouchListener(
                    RecyclerItemClickListener(
                        this@VideoPlayerActivity,
                        rv,
                        object :
                            RecyclerItemClickListener.OnItemClickListener {
                            override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                                if (!isWifi) {
                                    Toast.show("非WIFI连接或没有网络")
                                    return
                                }
                                var canSelect = true
                                if (adapter is SelectEpisodesAdapter) {
                                    canSelect = adapter.select(position)
                                }
                                if (canSelect) {
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
                                }
                                rv.visibility = View.GONE
                            }

                            override fun onItemLongClick(
                                view: View,
                                position: Int,
                                e: MotionEvent
                            ) {
                            }

                            override fun onItemDoubleClick(
                                view: View,
                                position: Int,
                                e: MotionEvent
                            ) {
                            }

                        })
                )
            }
            backButton.setOnClickListener {
                finish()
            }
            fullscreenButton.visibility = View.GONE
            setGSYVideoProgressListener { progress, secProgress, currentPosition, duration ->
                //            l.df(progress, secProgress, currentPosition, duration)
            }
            onAutoCompletion = { recyclerView ->
                val childView = recyclerView.getChildAt(index + 1)
                val vh =
                    recyclerView.getChildViewHolder(childView) as SelectEpisodesAdapter.ViewHolder
                vh.autoClick()
            }
            customVisibility = { visibility ->
                imageButton_float.visibility = visibility
            }
            customClick = {
                when (it) {
                    R.id.textView_download -> {
                        val url = getOriginalUrl()
                        download(url, title, img)
                    }
                }
            }
        }
        imageButton_float.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    MaterialDialog(applicationContext).show {
                        title(R.string.float_play)
                        message(
                            text = getString(R.string.float_tips)
                        )
                        negativeButton(R.string.open_float, click = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
                        })
                        positiveButton(R.string.cancel)
                        lifecycleOwner(this@VideoPlayerActivity)
                    }
                    return@setOnClickListener
                }
            }
            float(videoVM.play.value!!.url, videoVM.play.value!!.title)
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }
        initWebView()
    }

    private fun initWebView() {
        val webViewUtil = WebViewUtil()
        webViewUtil.initWebView(
            webView_for_get_url_player,
            { html!!.videoHtmlResultBean }
        ) { url ->
            //https://data.nmbaojie.com/zhilian.php?auth_key=1581337202-0-0-337bf041aae5d3bb6b01ac8bd0fba2fe&url=https://data.nmbaojie.com/tuchuang/jingyinglvshi_19.m3u8?auth_key=1581337202-0-0-96a9d05fc2b73403d6fec06f800b37bd
            val reg =
                Regex("https://data.nmbaojie.com/zhilian.php\\?auth_key=[a-z0-9].*&url=https://data.nmbaojie.com/[a-z0-9_].*\\.m3u8\\?auth_key=[a-z0-9].*")
            val urlSearchResult = reg.find(url)
            if (urlSearchResult != null) {
                val vhrb = html!!.videoHtmlResultBean.copy()
                vhrb.isFrame = false
                mainVM.getVideoInfo(
                    url,
                    vhrb,
                    img
                )
            } else {
                mainVM.play.postValue(
                    VideoInfoBean(
                        url,
                        title,
                        0,
                        img
                    )
                )
            }
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
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(
                "currentPos",
                floatPlayerView.videoPlayer!!.currentPositionWhenPlaying
            )
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
            l.start("VideoPlayerActivity-OnPause")
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
                    }.continueWithEnd("更新视频信息")
                }
            }
            l.end("VideoPlayerActivity-OnPause")
        }.continueWithEnd("保存视频信息")
        if (FloatWindow.get() == null) {
            gsyVideoPLayer.onVideoPause()
        }
    }

    private fun download(videoUrl: String, videoTitle: String, videoImgUrl: String) {
        WorkerUtil.startWorker(
            videoUrl,
            videoTitle,
            videoImgUrl,
            searchUrl,
            duration,
            applicationContext,
            this
        )
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun netEvent(netEvent: NetEvent) {
        isWifi = netEvent.isWifi
    }
}