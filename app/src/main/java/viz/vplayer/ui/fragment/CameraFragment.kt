package viz.vplayer.ui.fragment

import android.graphics.*
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.face.MLFace
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_camera.*
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import viz.commonlib.camera.Camera2Proxy
import viz.commonlib.camera.CameraVM
import viz.vplayer.R
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor


class CameraFragment : BaseFragment() {
    override fun getFragmentClassName(): String = "CameraFragment"
    override fun getContentViewId(): Int = R.layout.fragment_camera
    private var surfaceHolder: SurfaceHolder? = null
    private var camera2Proxy: Camera2Proxy? = null
    private var textureViewWidth = 0
    private var textureViewHeight = 0
    private var captureHeight: Int = 0
    private var captureWidth: Int = 0
    private var captureBitmap: Bitmap? = null
    private var lastTimeFR = 0L
    private lateinit var cameraVM: CameraVM
    private var es: ScheduledExecutorService = ScheduledThreadPoolExecutor(
        1,
        BasicThreadFactory.Builder().namingPattern("pool-fd-%d").daemon(true).build()
    )

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        cameraVM = activity?.run {
            ViewModelProvider(this).get(CameraVM::class.java)
        } ?: throw Exception("Invalid Activity")
        surfaceView.setZOrderOnTop(true)  //处于顶层
        surfaceView.holder.setFormat(PixelFormat.TRANSPARENT)  //设置surface为透明
        surfaceHolder = surfaceView.holder //设置一下SurfaceView并获取到surfaceHolder便于后面画框
        camera2Proxy = textureView.cameraProxy
        textureView.post {
            textureViewWidth = textureView.width
            textureViewHeight = textureView.height
        }
        textureView.onSurfaceTextureAvailable = { width, height, previewWidth, previewHeight ->
            captureWidth = previewWidth
            captureHeight = previewHeight
            captureBitmap =
                Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.ARGB_8888)
        }
        textureView.onSurfaceTextureUpdated = {
            if (lastTimeFR >= 0 && System.currentTimeMillis() - lastTimeFR > 50) {
                lastTimeFR = System.currentTimeMillis()
                textureView.getBitmap(captureBitmap)
                cameraVM.frame.postValue(captureBitmap)
            }
        }
        cameraVM.frame.observe(viewLifecycleOwner, Observer { frameBitmap ->
            es.execute {
                val analyzer = MLAnalyzerFactory.getInstance().faceAnalyzer
                val frame = MLFrame.fromBitmap(frameBitmap)
                val task: Task<List<MLFace>> = analyzer.asyncAnalyseFrame(frame)
                task.addOnSuccessListener { faces ->
                    l.d("检测成功")
                    l.d(faces)
                    drawRectangle(faces.toMutableList())
                }.addOnFailureListener { e ->
                    e.printStackTrace()
                    l.e("检测失败${e.message}")
                }
                analyzer?.stop()
            }
        })
    }

    private val maxFaceNumber = 5

    private fun drawRectangle(models: MutableList<MLFace>) {
        if (surfaceHolder == null) {
            return
        }
        try {
            //定义画笔
            val mpaint = Paint()
            mpaint.color = Color.BLUE
            mpaint.isAntiAlias = true//去锯齿
            mpaint.style = Paint.Style.STROKE//空心
            // 设置paint的外框宽度
            mpaint.strokeWidth = 2f

            var canvas = Canvas()
            if (surfaceHolder != null) {
                canvas = surfaceHolder!!.lockCanvas() ?: Canvas()
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) //清楚掉上一次的画框。
//        val dm = resources.displayMetrics

            models.forEachIndexed { index, faceInfo ->
                if (index >= maxFaceNumber) {
                    return
                }
                val draw_r = Rect(
                    faceInfo.border.left * textureViewWidth / captureWidth,
                    faceInfo.border.top * textureViewHeight / captureHeight,
                    faceInfo.border.right * textureViewWidth / captureWidth,
                    faceInfo.border.bottom * textureViewHeight / captureHeight
                )
                canvas.drawRect(draw_r, mpaint)
            }
            val defaultPaint = Paint()
            defaultPaint.color = Color.GREEN
            defaultPaint.isAntiAlias = true
            defaultPaint.strokeWidth = 3f
            defaultPaint.style = Paint.Style.STROKE//空心
            val defaultRect = Rect(
                0,
                0,
                textureViewWidth,
                textureViewHeight
            )
            canvas.drawRect(defaultRect, defaultPaint)
            surfaceHolder?.unlockCanvasAndPost(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!es.isShutdown) {
            es.shutdownNow()
        }
    }
}