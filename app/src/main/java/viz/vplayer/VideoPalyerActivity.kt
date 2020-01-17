package viz.vplayer

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.viz.tools.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_custom.*
import viz.vplayer.bean.HtmlBean
import viz.vplayer.bean.VideoHtmlResultBean
import viz.vplayer.bean.VideoInfoBean
import viz.vplayer.vm.MainVM
import viz.vplayer.vm.VideoVM

class VideoPalyerActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_video)
        supportActionBar?.hide()
        videoVM.play.observe(this, Observer { videoInfoBean ->
            loadingView_player.visibility = View.GONE
            gsyVideoPLayer.setUp(videoInfoBean.url, true, videoInfoBean.title)
            gsyVideoPLayer.startPlayLogic()
        })
        mainVM.errorInfo.observe(this, Observer { errorMsg ->
            loadingView.visibility = View.GONE
            Toast.showLong(this, errorMsg)
        })
        initVideo()
        val url = intent.getStringExtra("url")
        title = intent.getStringExtra("title") ?: ""
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
                url, title + if (episodes!![0].endsWith("m3u8")) {
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
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int, e: MotionEvent) {
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
                                    html!!.videoHtmlResultBean
                                )
                            }
                            recyclerView_select_episodes.visibility = View.GONE
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
    }

    override fun onPause() {
        super.onPause()
        gsyVideoPLayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        gsyVideoPLayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
    }
}