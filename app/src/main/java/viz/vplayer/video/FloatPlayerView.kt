package viz.vplayer.video

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import viz.vplayer.R

/**
 * 适配了悬浮窗的view
 * Created by guoshuyu on 2017/12/25.
 */
class FloatPlayerView : FrameLayout {
    var videoPlayer: FloatingVideo? = null
    var title = "测试视频"
    var url = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4"

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        videoPlayer = FloatingVideo(context)
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = Gravity.CENTER
        addView(videoPlayer, layoutParams)
        videoPlayer!!.setUp(url, true, title)
        //增加封面
/*ImageView imageView = new ImageView(getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
        videoPlayer.setThumbImageView(imageView);*/
//是否可以滑动调整
        videoPlayer!!.setIsTouchWiget(false)
    }

    fun onPause() {
        videoPlayer!!.currentPlayer.onVideoPause()
    }

    fun onResume() {
        videoPlayer!!.currentPlayer.onVideoResume()
    }
}