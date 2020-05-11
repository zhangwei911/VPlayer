package viz.commonlib.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest.Builder
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.SurfaceHolder
import java.util.*

class Camera2Proxy @TargetApi(Build.VERSION_CODES.M) constructor(private val mActivity: Activity) {
    private var mCameraId = CameraCharacteristics.LENS_FACING_BACK // 要打开的摄像头ID
    var previewSize // 预览大小
            : Size? = null
        private set
    private val mCameraManager // 相机管理者
            : CameraManager
    private var mCameraCharacteristics // 相机属性
            : CameraCharacteristics? = null
    private var mCameraDevice // 相机对象
            : CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mPreviewRequestBuilder // 相机预览请求的构造器
            : Builder? = null
    private var mPreviewRequest: CaptureRequest? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mImageReader: ImageReader? = null
    private var mPreviewSurface: Surface? = null
    private var mPreviewSurfaceTexture: SurfaceTexture? = null
    private val mOrientationEventListener: OrientationEventListener
    private var mDisplayRotate = 0
    private var mDeviceOrientation = 0 // 设备方向，由相机传感器获取
    private var mZoom = 1 // 缩放
    /**
     * 打开摄像头的回调
     */
    private val mStateCallback: StateCallback = object : StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            initPreviewRequest()
        }

        override fun onDisconnected(camera: CameraDevice) {
            releaseCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            releaseCamera()
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int) {
        startBackgroundThread() // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        mOrientationEventListener.enable()
        try {
            mCameraCharacteristics =
                mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId))
            val yourMinFocus = mCameraCharacteristics!!.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            val yourMaxFocus = mCameraCharacteristics!!.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
            Log.d(TAG,String.format("%f %f",yourMinFocus,yourMaxFocus))
            val map =
                mCameraCharacteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            // 拍照大小，选择能支持的一个最大的图片大小
            val largest = Collections.max(
                Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                CompareSizesByArea()
            )
            Log.d(
                TAG,
                "picture size: " + largest.width + "*" + largest.height
            )
            mImageReader = ImageReader.newInstance(
                largest.width,
                largest.height,
                ImageFormat.JPEG,
                2
            )
            // 预览大小，根据上面选择的拍照图片的长宽比，选择一个和控件长宽差不多的大小
            previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                width,
                height,
                largest
            )
            Log.d(
                TAG,
                "preview size: " + previewSize!!.width + "*" + previewSize!!.height
            )
            // 打开摄像头
            mCameraManager.openCamera(
                Integer.toString(mCameraId),
                mStateCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun releaseCamera() {
        if (null != mCaptureSession) {
            mCaptureSession?.close()
            mCaptureSession = null
        }
        if (mCameraDevice != null) {
            mCameraDevice?.close()
            mCameraDevice = null
        }
        if (mImageReader != null) {
            mImageReader?.close()
            mImageReader = null
        }
        mOrientationEventListener.disable()
        stopBackgroundThread() // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    fun setImageAvailableListener(onImageAvailableListener: OnImageAvailableListener?) {
        if (mImageReader == null) {
            Log.w(TAG, "setImageAvailableListener: mImageReader is null")
            return
        }
        mImageReader?.setOnImageAvailableListener(onImageAvailableListener, null)
    }

    fun setPreviewSurface(holder: SurfaceHolder) {
        mPreviewSurface = holder.surface
    }

    fun setPreviewSurface(surfaceTexture: SurfaceTexture?) {
        mPreviewSurfaceTexture = surfaceTexture
    }

    private fun initPreviewRequest() {
        try {
            mPreviewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            if (mPreviewSurfaceTexture != null && mPreviewSurface == null) { // use texture view
                mPreviewSurfaceTexture!!.setDefaultBufferSize(
                    previewSize!!.width,
                    previewSize!!.height
                )
                mPreviewSurface = Surface(mPreviewSurfaceTexture)
            }
            mPreviewRequestBuilder?.addTarget(mPreviewSurface!!) // 设置预览输出的 Surface
            mCameraDevice?.createCaptureSession(
                Arrays.asList(
                    mPreviewSurface,
                    mImageReader!!.surface
                ),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCaptureSession = session
                        // 设置连续自动对焦
                        mPreviewRequestBuilder?.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        // 设置自动曝光
                        mPreviewRequestBuilder?.set(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                        )
                        // 设置完后自动开始预览
                        mPreviewRequest = mPreviewRequestBuilder!!.build()
                        startPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(
                            TAG,
                            "ConfigureFailed. session: mCaptureSession"
                        )
                    }
                }, mBackgroundHandler
            ) // handle 传入 null 表示使用当前线程的 Looper
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startPreview() {
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(
                TAG,
                "startPreview: mCaptureSession or mPreviewRequestBuilder is null"
            )
            return
        }
        try { // 开始预览，即一直发送预览的请求
            mCaptureSession?.setRepeatingRequest(mPreviewRequest!!, null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(
                TAG,
                "stopPreview: mCaptureSession or mPreviewRequestBuilder is null"
            )
            return
        }
        try {
            mCaptureSession?.stopRepeating()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun captureStillPicture() {
        try {
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(mDeviceOrientation)
            )
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            val zoomRect =
                mPreviewRequestBuilder!!.get(CaptureRequest.SCALER_CROP_REGION)
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.abortCaptures()
            val time = System.currentTimeMillis()
            mCaptureSession!!.capture(captureBuilder.build(), object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.w(
                        TAG,
                        "onCaptureCompleted, time: " + (System.currentTimeMillis() - time)
                    )
                    try {
                        mPreviewRequestBuilder?.set(
                            CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                        )
                        mCaptureSession?.capture(
                            mPreviewRequestBuilder!!.build(),
                            null,
                            mBackgroundHandler
                        )
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                    startPreview()
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getJpegOrientation(deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        val facingFront =
            mCameraCharacteristics!!.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation
        // Calculate desired JPEG orientation relative to camera orientation to make
// the image upright relative to the device orientation
        val jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360
        Log.d(TAG, "jpegOrientation: $jpegOrientation")
        return jpegOrientation
    }

    val isFrontCamera: Boolean
        get() = mCameraId == CameraCharacteristics.LENS_FACING_BACK

    fun switchCamera(width: Int, height: Int) {
        mCameraId = mCameraId xor 1
        releaseCamera()
        openCamera(width, height)
    }

    private fun chooseOptimalSize(
        sizes: Array<Size>,
        viewWidth: Int,
        viewHeight: Int,
        pictureSize: Size
    ): Size {
        val totalRotation = rotation
        val swapRotation = totalRotation == 90 || totalRotation == 270
        val width = if (swapRotation) viewHeight else viewWidth
        val height = if (swapRotation) viewWidth else viewHeight
        return getSuitableSize(sizes, width, height, pictureSize)
    }

    private val rotation: Int
        private get() {
            var displayRotation = mActivity.windowManager.defaultDisplay.rotation
            when (displayRotation) {
                Surface.ROTATION_0 -> displayRotation = 90
                Surface.ROTATION_90 -> displayRotation = 0
                Surface.ROTATION_180 -> displayRotation = 270
                Surface.ROTATION_270 -> displayRotation = 180
            }
            val sensorOrientation =
                mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            mDisplayRotate = (displayRotation + sensorOrientation + 270) % 360
            return mDisplayRotate
        }

    private fun getSuitableSize(
        sizes: Array<Size>,
        width: Int,
        height: Int,
        pictureSize: Size
    ): Size {
        var minDelta = Integer.MAX_VALUE // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        var index = 0 // 最小的差值对应的索引坐标
        val aspectRatio = pictureSize.height * 1.0f / pictureSize.width
        Log.d(TAG, "getSuitableSize. aspectRatio: $aspectRatio")
        for (i in sizes.indices) {
            val size = sizes[i]
            // 先判断比例是否相等
            if (size.width * aspectRatio == size.height.toFloat()) {
                val delta = Math.abs(width - size.width)
                if (delta == 0) {
                    return size
                }
                if (minDelta > delta) {
                    minDelta = delta
                    index = i
                }
            }
        }
        return sizes[index]
    }

    fun handleZoom(isZoomIn: Boolean) {
        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
            return
        }
        val maxZoom =
            (mCameraCharacteristics!!.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)!!.toInt()
                    * 10)
        val rect =
            mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (isZoomIn && mZoom < maxZoom) {
            mZoom++
        } else if (mZoom > 1) {
            mZoom--
        }
        val minW = rect!!.width() / maxZoom
        val minH = rect.height() / maxZoom
        val difW = rect.width() - minW
        val difH = rect.height() - minH
        var cropW = difW * mZoom / 100
        var cropH = difH * mZoom / 100
        cropW -= cropW and 3
        cropH -= cropH and 3
        val zoomRect = Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH)
        mPreviewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        mPreviewRequest = mPreviewRequestBuilder!!.build()
        startPreview() // 需要重新 start preview 才能生效
    }

    fun focusOnPoint(x: Double, y: Double, width: Int, height: Int) {
        var x = x
        var y = y
        if (mCameraDevice == null || mPreviewRequestBuilder == null) {
            return
        }
        // 1. 先取相对于view上面的坐标
        var previewWidth = previewSize!!.width
        var previewHeight = previewSize!!.height
        if (mDisplayRotate == 90 || mDisplayRotate == 270) {
            previewWidth = previewSize!!.height
            previewHeight = previewSize!!.width
        }
        // 2. 计算摄像头取出的图像相对于view放大了多少，以及有多少偏移
        val tmp: Double
        var imgScale: Double
        var verticalOffset = 0.0
        var horizontalOffset = 0.0
        if (previewHeight * width > previewWidth * height) {
            imgScale = width * 1.0 / previewWidth
            verticalOffset = (previewHeight - height / imgScale) / 2
        } else {
            imgScale = height * 1.0 / previewHeight
            horizontalOffset = (previewWidth - width / imgScale) / 2
        }
        // 3. 将点击的坐标转换为图像上的坐标
        x = x / imgScale + horizontalOffset
        y = y / imgScale + verticalOffset
        if (90 == mDisplayRotate) {
            tmp = x
            x = y
            y = previewSize!!.height - tmp
        } else if (270 == mDisplayRotate) {
            tmp = x
            x = previewSize!!.width - y
            y = tmp
        }
        // 4. 计算取到的图像相对于裁剪区域的缩放系数，以及位移
        var cropRegion = mPreviewRequestBuilder!!.get(CaptureRequest.SCALER_CROP_REGION)
        if (cropRegion == null) {
            Log.w(TAG, "can't get crop region")
            cropRegion =
                mCameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        }
        val cropWidth = cropRegion!!.width()
        val cropHeight = cropRegion.height()
        if (previewSize!!.height * cropWidth > previewSize!!.width * cropHeight) {
            imgScale = cropHeight * 1.0 / previewSize!!.height
            verticalOffset = 0.0
            horizontalOffset = (cropWidth - imgScale * previewSize!!.width) / 2
        } else {
            imgScale = cropWidth * 1.0 / previewSize!!.width
            horizontalOffset = 0.0
            verticalOffset = (cropHeight - imgScale * previewSize!!.height) / 2
        }
        // 5. 将点击区域相对于图像的坐标，转化为相对于成像区域的坐标
        x = x * imgScale + horizontalOffset + cropRegion.left
        y = y * imgScale + verticalOffset + cropRegion.top
        val tapAreaRatio = 0.1
        val rect = Rect()
        rect.left =
            clamp((x - tapAreaRatio / 2 * cropRegion.width()).toInt(), 0, cropRegion.width())
        rect.right =
            clamp((x + tapAreaRatio / 2 * cropRegion.width()).toInt(), 0, cropRegion.width())
        rect.top =
            clamp((y - tapAreaRatio / 2 * cropRegion.height()).toInt(), 0, cropRegion.height())
        rect.bottom =
            clamp((y + tapAreaRatio / 2 * cropRegion.height()).toInt(), 0, cropRegion.height())
        // 6. 设置 AF、AE 的测光区域，即上述得到的 rect
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_REGIONS,
            arrayOf(MeteringRectangle(rect, 1000))
        )
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AE_REGIONS,
            arrayOf(MeteringRectangle(rect, 1000))
        )
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_AUTO
        )
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        mPreviewRequestBuilder?.set(
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
        )
        try { // 7. 发送上述设置的对焦请求，并监听回调
            mCaptureSession?.capture(
                mPreviewRequestBuilder!!.build(),
                mAfCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val mAfCaptureCallback: CaptureCallback = object : CaptureCallback() {
        private fun process(result: CaptureResult) {
            val state = result.get(CaptureResult.CONTROL_AF_STATE) ?: return
            if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                mPreviewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                )
                mPreviewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                mPreviewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
                startPreview()
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }

    private fun startBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null) {
            mBackgroundThread = HandlerThread("CameraBackground")
            mBackgroundThread?.start()
            mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) return max
        return if (x < min) min else x
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(
            lhs: Size,
            rhs: Size
        ): Int { // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    companion object {
        private const val TAG = "Camera2Proxy"
    }

    init {
        mCameraManager = mActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mOrientationEventListener = object : OrientationEventListener(mActivity) {
            override fun onOrientationChanged(orientation: Int) {
                mDeviceOrientation = orientation
            }
        }
    }
}