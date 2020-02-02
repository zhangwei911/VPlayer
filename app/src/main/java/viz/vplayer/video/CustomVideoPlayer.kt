package viz.vplayer.video

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.SeekBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.utils.Debuger
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import com.viz.tools.Toast
import kotlinx.android.synthetic.main.video_custom.view.*
import viz.vplayer.R
import viz.vplayer.adapter.SelectEpisodesAdapter

class CustomVideoPlayer : StandardGSYVideoPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var selectEpisodes: (() -> MutableList<String>)? = null
        set(value) {
            textView_select_episodes.visibility = if (value != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }
    var episodesClick: ((recyclerView: RecyclerView) -> Unit)? = null
    private var videoList = mutableListOf<String>()
    private var selectEpisodesAdapter: SelectEpisodesAdapter? = null
    private var speedIndex = 2

    override fun getLayoutId(): Int {
        return R.layout.video_custom
    }

    fun getUrl(): String {
        return mUrl!!
    }

    fun getOriginalUrl(): String {
        return mOriginUrl!!
    }

    override fun init(context: Context?) {
        super.init(context)
        textView_select_episodes.setOnClickListener {
            if (selectEpisodes != null) {
                if (recyclerView_select_episodes.visibility == View.VISIBLE) {
                    recyclerView_select_episodes.visibility = View.GONE
                } else {
                    recyclerView_select_episodes.visibility = View.VISIBLE
                    videoList = selectEpisodes!!.invoke()
                    selectEpisodesAdapter = SelectEpisodesAdapter(context!!)
                    selectEpisodesAdapter!!.data = videoList
                    val gridLayoutManager = GridLayoutManager(context, 6)
                    recyclerView_select_episodes.layoutManager = gridLayoutManager
                    recyclerView_select_episodes.adapter = selectEpisodesAdapter
                    selectEpisodesAdapter?.notifyDataSetChanged()
                    episodesClick?.invoke(recyclerView_select_episodes)
                }
            }
        }
        val speedArr = mutableListOf(0.1f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f)
        textView_speed_play.text = "${speedArr[2]}x"
        textView_speed_play.setOnClickListener {
            speedIndex++
            if (speedIndex >= speedArr.size) {
                speedIndex = 0
            }
            val speed = speedArr[speedIndex]
            textView_speed_play.text = "${speed}x"
            setSpeedPlaying(speed, true)
        }
        textView_speed_play.setOnLongClickListener {
            if (speedIndex != 2) {
                speedIndex = 2
                val speed = speedArr[speedIndex]
                textView_speed_play.text = "${speed}x"
                setSpeedPlaying(speed, true)
            }
            return@setOnLongClickListener true
        }
        textView_download.setOnClickListener {
            customClick?.invoke(it.id)
        }
    }

    override fun onBufferingUpdate(percent: Int) {
        super.onBufferingUpdate(percent)
        if (textView_speed.visibility == View.VISIBLE) {
            textView_speed.text = netSpeedText
        }
    }

    override fun startWindowFullscreen(
        context: Context?,
        actionBar: Boolean,
        statusBar: Boolean
    ): GSYBaseVideoPlayer? {
        val gsyBaseVideoPlayer =
            super.startWindowFullscreen(context, actionBar, statusBar)
        val customVideoPlayer: CustomVideoPlayer = gsyBaseVideoPlayer as CustomVideoPlayer
        return gsyBaseVideoPlayer
    }


    override fun showSmallVideo(
        size: Point?,
        actionBar: Boolean,
        statusBar: Boolean
    ): GSYBaseVideoPlayer? { //下面这里替换成你自己的强制转化
        val customVideoPlayer: CustomVideoPlayer =
            super.showSmallVideo(size, actionBar, statusBar) as CustomVideoPlayer
        customVideoPlayer.mStartButton.visibility = View.GONE
        customVideoPlayer.mStartButton = null
        return customVideoPlayer
    }

    override fun cloneParams(from: GSYBaseVideoPlayer, to: GSYBaseVideoPlayer) {
        super.cloneParams(from, to)
        val sf: CustomVideoPlayer = from as CustomVideoPlayer
        val st: CustomVideoPlayer = to as CustomVideoPlayer
        st.mShowFullAnimation = sf.mShowFullAnimation
    }


    /**
     * 退出window层播放全屏效果
     */
    override fun clearFullscreenLayout() {
        if (!mFullAnimEnd) {
            return
        }
        mIfCurrentIsFullscreen = false
        var delay = 0
        if (mOrientationUtils != null) {
            delay = mOrientationUtils.backToProtVideo()
            mOrientationUtils.isEnable = false
            if (mOrientationUtils != null) {
                mOrientationUtils.releaseListener()
                mOrientationUtils = null
            }
        }
        if (!mShowFullAnimation) {
            delay = 0
        }
        val vp =
            CommonUtil.scanForActivity(context).findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
        val oldF: View = vp.findViewById(fullId)
        if (oldF != null) { //此处fix bug#265，推出全屏的时候，虚拟按键问题
            val gsyVideoPlayer: CustomVideoPlayer = oldF as CustomVideoPlayer
            gsyVideoPlayer.mIfCurrentIsFullscreen = false
        }
        if (delay == 0) {
            backToNormal()
        } else {
            postDelayed({ backToNormal() }, delay.toLong())
        }
    }


    /******************* 下方两个重载方法，在播放开始前不屏蔽封面，不需要可屏蔽  */
    override fun onSurfaceUpdated(surface: Surface?) {
        super.onSurfaceUpdated(surface)
        if (mThumbImageViewLayout != null && mThumbImageViewLayout.visibility == View.VISIBLE) {
            mThumbImageViewLayout.visibility = View.INVISIBLE
        }
    }

    override fun setViewShowState(view: View, visibility: Int) {
        if (view === mThumbImageViewLayout && visibility != View.VISIBLE) {
            return
        }
        super.setViewShowState(view, visibility)
    }

    override fun onSurfaceAvailable(surface: Surface?) {
        super.onSurfaceAvailable(surface)
        if (GSYVideoType.getRenderType() != GSYVideoType.TEXTURE) {
            if (mThumbImageViewLayout != null && mThumbImageViewLayout.visibility == View.VISIBLE) {
                mThumbImageViewLayout.visibility = View.INVISIBLE
            }
        }
    }

    /******************* 下方重载方法，在播放开始不显示底部进度和按键，不需要可屏蔽  */
    protected var byStartedClick = false

    override fun onClickUiToggle() {
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            setViewShowState(mLockScreen, View.VISIBLE)
            return
        }
        val visibility = if (mBottomContainer.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        if (selectEpisodes != null) {
            textView_select_episodes.visibility = visibility
        }
        setViewShowState(textView_speed, visibility)
        customVisibility?.invoke(visibility)
        byStartedClick = true
        super.onClickUiToggle()
    }

    var customVisibility: ((visibility: Int) -> Unit)? = null

    var customClick: ((vid:Int) -> Unit)? = null

    override fun hideAllWidget() {
        super.hideAllWidget()
        setViewShowState(textView_select_episodes, View.GONE)
        setViewShowState(recyclerView_select_episodes, View.GONE)
        setViewShowState(textView_speed, View.GONE)
        customVisibility?.invoke(View.GONE)
        if (currentState == GSYVideoView.CURRENT_STATE_PREPAREING || currentState == GSYVideoView.CURRENT_STATE_ERROR) {
            setViewShowState(mTopContainer, View.VISIBLE)
            setViewShowState(textView_select_episodes, View.VISIBLE)
            setViewShowState(textView_speed, View.VISIBLE)
        }
    }

    override fun changeUiToNormal() {
        super.changeUiToNormal()
        setViewShowState(textView_select_episodes, View.VISIBLE)
        setViewShowState(recyclerView_select_episodes, View.GONE)
        setViewShowState(textView_speed, View.VISIBLE)
        customVisibility?.invoke(View.VISIBLE)
        byStartedClick = false
    }

    override fun changeUiToPreparingShow() {
        super.changeUiToPreparingShow()
        Debuger.printfLog("Sample changeUiToPreparingShow")
        setViewShowState(mBottomContainer, View.INVISIBLE)
        setViewShowState(mStartButton, View.INVISIBLE)
        setViewShowState(textView_speed, View.VISIBLE)
    }

    override fun changeUiToPlayingBufferingShow() {
        super.changeUiToPlayingBufferingShow()
        Debuger.printfLog("Sample changeUiToPlayingBufferingShow")
        setViewShowState(textView_speed, View.VISIBLE)
        if (!byStartedClick) {
            setViewShowState(mBottomContainer, View.INVISIBLE)
            setViewShowState(mStartButton, View.INVISIBLE)
        }
    }

    override fun changeUiToPlayingShow() {
        super.changeUiToPlayingShow()
        Debuger.printfLog("Sample changeUiToPlayingShow")
        if (!byStartedClick) {
            setViewShowState(mBottomContainer, View.INVISIBLE)
            setViewShowState(mStartButton, View.INVISIBLE)
        }
    }

    override fun onPrepared() {
        setViewShowState(mTopContainer, View.VISIBLE)
        setViewShowState(textView_select_episodes, View.VISIBLE)
        setViewShowState(textView_speed, View.VISIBLE)
        super.onPrepared()
    }

    override fun startAfterPrepared() {
        super.startAfterPrepared()
        mThreshold = 10
        mSeekRatio = 10f
        gestureDetector = GestureDetector(
            context.applicationContext,
            object : SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val x: Float = e.x
                    val y: Float = e.y
                    val screenWidth = CommonUtil.getScreenWidth(context)
                    when {
                        x >= screenWidth * 0.8 -> {
                            if (currentState == GSYVideoView.CURRENT_STATE_PLAYING || currentState == GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START) {
                                seekTo((currentPositionWhenPlaying + 10 * 1000).toLong())
                                Toast.show(context, "快进10s")
                            }
                        }
                        x < screenWidth * 0.2 -> {
                            if (currentState == GSYVideoView.CURRENT_STATE_PLAYING || currentState == GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START) {
                                seekTo((currentPositionWhenPlaying - 10 * 1000).toLong())
                                Toast.show(context, "快退10s")
                            }
                        }
                        else -> {
                            touchDoubleUp()
                        }
                    }
                    return super.onDoubleTap(e)
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (!mChangePosition && !mChangeVolume && !mBrightness) {
                        onClickUiToggle()
                    }
                    return super.onSingleTapConfirmed(e)
                }
            })
        Debuger.printfLog("Sample startAfterPrepared")
        setViewShowState(mBottomContainer, View.INVISIBLE)
        setViewShowState(mStartButton, View.INVISIBLE)
        setViewShowState(mBottomProgressBar, View.VISIBLE)
        setViewShowState(textView_speed, View.VISIBLE)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        super.onProgressChanged(seekBar, progress, fromUser)
        if (fromUser) {
            val newCur = progress / 100.0 * gsyVideoManager.duration
            Toast.show(
                context, CommonUtil.stringForTime(newCur.toInt())
            )
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        byStartedClick = true
        super.onStartTrackingTouch(seekBar)
    }

    var onAutoCompletion: ((recyclerView: RecyclerView) -> Unit)? = null
    override fun onAutoCompletion() {
        super.onAutoCompletion()
        onAutoCompletion?.invoke(recyclerView_select_episodes)
    }
}