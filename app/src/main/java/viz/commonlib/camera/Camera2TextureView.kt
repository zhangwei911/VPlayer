package viz.commonlib.camera

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView

class Camera2TextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    TextureView(context, attrs, defStyleAttr, defStyleRes) {
    var cameraProxy: Camera2Proxy? = null
        private set
    private var mRatioWidth = 0
    private var mRatioHeight = 0
    private var mOldDistance = 0f
    private fun init(context: Context) {
        surfaceTextureListener = mSurfaceTextureListener
        cameraProxy = Camera2Proxy((context as Activity))
    }

    var onSurfaceTextureAvailable: ((width: Int, height: Int, previewWidth: Int, previewHeight: Int) -> Unit)? =
        null
    var onSurfaceTextureUpdated: (() -> Unit)? = null

    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            cameraProxy!!.setPreviewSurface(surface)
            cameraProxy!!.openCamera(width, height)
            // resize TextureView
            val previewWidth = cameraProxy!!.previewSize!!.width
            val previewHeight = cameraProxy!!.previewSize!!.height
            if (width > height) {
                setAspectRatio(previewWidth, previewHeight)
            } else {
                setAspectRatio(previewHeight, previewWidth)
            }
            onSurfaceTextureAvailable?.invoke(width, height, previewWidth, previewHeight)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            cameraProxy!!.releaseCamera()
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            onSurfaceTextureUpdated?.invoke()
        }
    }

    private fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        mRatioWidth = width
        mRatioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 1) {
            cameraProxy!!.focusOnPoint(
                event.x.toDouble(),
                event.y.toDouble(),
                width,
                height
            )
            return true
        }
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> mOldDistance =
                getFingerSpacing(event)
            MotionEvent.ACTION_MOVE -> {
                val newDistance = getFingerSpacing(event)
                if (newDistance > mOldDistance) {
                    cameraProxy!!.handleZoom(true)
                } else if (newDistance < mOldDistance) {
                    cameraProxy!!.handleZoom(false)
                }
                mOldDistance = newDistance
            }
            else -> {
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "CameraTextureView"
        private fun getFingerSpacing(event: MotionEvent): Float {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return Math.sqrt(x * x + y * y.toDouble()).toFloat()
        }
    }

    init {
        init(context)
    }
}
