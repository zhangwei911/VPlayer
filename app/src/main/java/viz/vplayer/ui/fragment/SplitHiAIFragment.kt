package viz.vplayer.ui.fragment

import android.graphics.*
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.huawei.hiai.pdk.pluginservice.ILoadPluginCallback
import com.huawei.hiai.pdk.resultcode.HwHiAIResultCode
import com.huawei.hiai.vision.common.ConnectionCallback
import com.huawei.hiai.vision.common.VisionBase
import com.huawei.hiai.vision.common.VisionImage
import com.huawei.hiai.vision.common.VisionImageMetadata
import com.huawei.hiai.vision.image.segmentation.ImageSegmentation
import com.huawei.hiai.vision.image.segmentation.SegConfiguration
import com.huawei.hiai.vision.visionkit.image.ImageResult
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_split_ai.*
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import viz.commonlib.camera.Camera2Proxy
import viz.commonlib.camera.CameraVM
import viz.vplayer.R
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class SplitHiAIFragment : BaseFragment() {
    override fun getFragmentClassName(): String = "SplitFragment"
    override fun getContentViewId(): Int = R.layout.fragment_split_ai
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
        BasicThreadFactory.Builder().namingPattern("pool-split-%d").daemon(true).build()
    )
    private var imageSegmentation: ImageSegmentation? = null
    private var build: SegConfiguration? = null
    private var builder: VisionImageMetadata.Builder? = null
    private var metadata: VisionImageMetadata? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        VisionBase.init(context, object : ConnectionCallback {
            override fun onServiceConnect() {
                l.d()
                //当与服务连接成功时，会调用此回调方法；
                //您可以在这里进行detector类的初始化、标记服务连接状态等
            }

            override fun onServiceDisconnect() {
                l.d()
                //当与服务断开时，会调用此回调方法；
                //您可以选择在这里进行服务的重连，或者对异常进行处理；
            }
        })
        imageSegmentation = ImageSegmentation(context!!.applicationContext)
        build = SegConfiguration.Builder().setProcessMode(SegConfiguration.MODE_IN)
            .setSegmentationType(SegConfiguration.TYPE_PORTRAIT_SEGMENTATION_VIDEO)
            .setOutputType(SegConfiguration.OUTPUT_TYPE_ARGB_BITMAP).build()
        cameraVM = activity?.run {
            ViewModelProvider(this).get(CameraVM::class.java)
        } ?: throw Exception("Invalid Activity")
        surfaceView_ai.setZOrderOnTop(true)  //处于顶层
        surfaceView_ai.holder.setFormat(PixelFormat.TRANSPARENT)  //设置surface为透明
        surfaceHolder = surfaceView_ai.holder //设置一下SurfaceView并获取到surfaceHolder便于后面画框
        camera2Proxy = textureView_ai.cameraProxy
        textureView_ai.post {
            textureViewWidth = textureView_ai.width
            textureViewHeight = textureView_ai.height
            builder = VisionImageMetadata.Builder().apply {
                setFormat(17)
                setRotation(270)
                setWidth(textureViewWidth)
                setHeight(textureViewHeight)
            }
            metadata = builder!!.build()
        }
        textureView_ai.onSurfaceTextureAvailable = { width, height, previewWidth, previewHeight ->
            captureWidth = previewWidth
            captureHeight = previewHeight
            captureBitmap =
                Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.ARGB_8888)
        }
        textureView_ai.onSurfaceTextureUpdated = {
            if (lastTimeFR >= 0 && System.currentTimeMillis() - lastTimeFR > 50) {
                lastTimeFR = System.currentTimeMillis()
                textureView_ai.getBitmap(captureBitmap)
                cameraVM.frame.postValue(captureBitmap)
            }
        }
        cameraVM.frame.observe(viewLifecycleOwner, Observer { frameBitmap ->
            es.execute {
                val image = VisionImage.fromBitmap(frameBitmap)
                image?.apply {
                    val mImageResult = ImageResult()
                    val resultCode = imageSegmentation!!.doSegmentation(this, mImageResult, null)
                    val canvas = if (surfaceHolder != null) {
                        surfaceHolder!!.lockCanvas()
                    } else {
                        Canvas()
                    }
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    canvas.drawBitmap(mImageResult.bitmap,0f,0f,null)
                    surfaceHolder?.unlockCanvasAndPost(canvas)
                }
            }
        })

    }

    private fun getAvailability() {
        val availability = imageSegmentation!!.getAvailability()
        if (availability == HwHiAIResultCode.AIRESULT_PLUGIN_PENDING_UPDATE) {
            val lock = ReentrantLock()
            val condition = lock.newCondition()
            val mResultCode = intArrayOf(HwHiAIResultCode.AIRESULT_UNKOWN)
            val cb = object : ILoadPluginCallback.Stub() {
                override fun onResult(resultCode: Int) {
                    bolts.Task.callInBackground {
                        mResultCode[0] = resultCode
                        if (resultCode == 101) {
                            try {
                                bolts.Task.delay(10)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        lock.lock()
                        try {
                            condition.signalAll()
                        } finally {
                            lock.unlock()
                        }
                    }
                }

                override fun onProgress(progress: Int) {
                    l.d("progress:$progress")
                }
            };
            /**Plugin download.*/
            imageSegmentation?.loadPlugin(cb)
            lock.lock()
            try {
                /**Timeout judgment.*/
                condition.await(90, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                l.e(e.message)
            } finally {
                lock.unlock()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!es.isShutdown) {
            es.shutdown()
        }
    }
}