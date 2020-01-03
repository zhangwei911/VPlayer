package viz.vplayer

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import bolts.Task
import com.lidroid.xutils.HttpUtils
import com.lidroid.xutils.exception.HttpException
import com.lidroid.xutils.http.ResponseInfo
import com.lidroid.xutils.http.callback.RequestCallBack
import com.lidroid.xutils.http.client.HttpRequest
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_custom.*
import org.jsoup.Jsoup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_video)
        supportActionBar?.hide()
        videoVM.play.observe(this, Observer { videoInfoBean ->
            gsyVideoPLayer.setUp(videoInfoBean.url, true, videoInfoBean.title)
            gsyVideoPLayer.startPlayLogic()
        })
        mainVM.errorInfo.observe(this, Observer { errorMsg ->
            Toast.showLong(this, errorMsg)
        })
        initVideo()
        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val duration = intent.getLongExtra("duration", 0)
        episodes = intent.getSerializableExtra("episodes") as MutableList<String>
        gsyVideoPLayer.selectEpisodes = {
            episodes!!
        }
        if (url.isNullOrEmpty() || title.isNullOrEmpty()) {
            Toast.show(this, "参数错误")
            return
        }
        videoVM.play.postValue(VideoInfoBean(url, title, duration))
        mainVM.play.observe(this, Observer { videoInfoBean ->
            videoVM.play.postValue(videoInfoBean)
        })
    }

    private fun initVideo() {
        //全屏
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
        gsyVideoPLayer.titleTextView.visibility = View.VISIBLE
        gsyVideoPLayer.setIsTouchWiget(true)
        gsyVideoPLayer.episodesClick = {
            it.addOnItemTouchListener(
                RecyclerItemClickListener(
                    this,
                    it,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                            mainVM.getVideoInfo(episodes!![position])
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