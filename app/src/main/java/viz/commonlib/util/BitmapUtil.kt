package viz.commonlib.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import com.viz.tools.l
import java.io.*
import java.util.*
import android.graphics.Bitmap
import viz.vplayer.util.FileUtil


object BitmapUtil {
    /**
     * 随机生产文件名
     *
     * @return
     */
    private fun generateFileName(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 保存bitmap到本地
     *
     * @param context
     * @param mBitmap
     * @return
     */
    fun saveBitmap(context: Context, mBitmap: Bitmap,cachePicPath:String = "cache"): String? {
        val savePath: String
        val filePic: File
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().absolutePath + "/" + context.packageName + "/$cachePicPath"
        } else {
            savePath = FileUtil.getPath(context) + "/$cachePicPath"
        }
        try {
            filePic = File(savePath + generateFileName() + ".jpg")
            if (!filePic.exists()) {
                filePic.parentFile.mkdirs()
                filePic.createNewFile()
            }
            val fos = FileOutputStream(filePic)
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return filePic.absolutePath
    }

    /**
     * 根据路径 转bitmap
     * @param file
     * @return
     */
    fun getBitMBitmap(path: String): Bitmap? {
        var map: Bitmap? = null
        try {
            map = BitmapFactory.decodeStream(FileInputStream(path))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return map
    }

    /**
     * Bitmap → byte[]
     */
    fun Bitmap2Bytes(bm: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    /**
     * byte[] → Bitmap
     */
    fun Bytes2Bimap(b: ByteArray): Bitmap? {
        return if (b.isNotEmpty()) {
            BitmapFactory.decodeByteArray(b, 0, b.size)
        } else {
            null
        }
    }

    /**
     * Bitmap缩放
     */
    fun zoomBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        val scaleWidth = width.toFloat() / w
        val scaleHeight = height.toFloat() / h
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    /**
     * Bitmap根据宽度比例缩放
     */
    fun zoomBitmapByWidth(bitmap: Bitmap, width: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        val scale = width.toFloat() / w
        matrix.setScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    /**
     * Bitmap根据高度比例缩放
     */
    fun zoomBitmapByHeight(bitmap: Bitmap, height: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        val scale = height.toFloat() / h
        matrix.setScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }

    /**
     * Bitmap自动选择高度/宽度比例缩放
     */
    fun zoomBitmapAuto(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        l.df(width, height)
        val w = bitmap.width
        val h = bitmap.height
        val ratioFront = h.toFloat() / w
        val ratio = height.toFloat() / width
        return if (ratioFront > ratio) {
            zoomBitmapByHeight(bitmap, height)
        } else {
            zoomBitmapByWidth(bitmap, width)
        }
    }

    /**
     * 根据宽度居中Bitmap,上下用黑色填充
     */
    fun mergeBitmap(frontBitmap: Bitmap, width: Int = 486, height: Int = 866): Bitmap? {
        val bmUse = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmUse.eraseColor(Color.parseColor("#000000"))
        l.df(bmUse.width, bmUse.height, frontBitmap.width, frontBitmap.height)
        val fbm = zoomBitmapAuto(frontBitmap, bmUse.width, bmUse.height)
        val bitmap = bmUse.copy(Bitmap.Config.ARGB_8888, true)
        val paint = Paint()
        val w = bitmap.width
        val h = bitmap.height

        val w_2 = fbm.width
        val h_2 = fbm.height
        l.df(w, h, w_2, h_2, fbm.width, fbm.height)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        canvas.drawBitmap(fbm, (Math.abs(w - w_2) / 2).toFloat(), (Math.abs(h - h_2) / 2).toFloat(), paint)
        return bitmap
    }

    /**
     * 将Drawable转化为Bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        // 取 drawable 的长宽
        val w = drawable.getIntrinsicWidth()
        val h = drawable.getIntrinsicHeight()

        // 取 drawable 的颜色格式
        val config = if (drawable.getOpacity() !== PixelFormat.OPAQUE)
            Bitmap.Config.ARGB_8888
        else
            Bitmap.Config.RGB_565
        // 建立对应 bitmap
        val bitmap = Bitmap.createBitmap(w, h, config)
        // 建立对应 bitmap 的画布
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        // 把 drawable 内容画到画布中
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 获得圆角图片
     */
    fun getRoundedCornerBitmap(bitmap: Bitmap, roundPx: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, w, h)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    /**
     *  Bitmap转换成Drawable
     */
    fun bitmapToDrawable(bm: Bitmap, context: Context): Drawable {
        return BitmapDrawable(context.resources, bm)
    }

    /**
     * Drawable缩放
     */
    fun zoomDrawable(drawable: Drawable, w: Int, h: Int): Drawable {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        // drawable转换成bitmap
        val oldbmp = drawableToBitmap(drawable)
        // 创建操作图片用的Matrix对象
        val matrix = Matrix()
        // 计算缩放比例
        val sx = w.toFloat() / width
        val sy = h.toFloat() / height
        // 设置缩放比例
        matrix.postScale(sx, sy)
        // 建立新的bitmap，其内容是对原bitmap的缩放后的图
        val newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height, matrix, true)
        return BitmapDrawable(newbmp)
    }

    /**
     * 图片 等比例压缩
     */
    fun compressEqualRatio(image: Bitmap): Bitmap? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        if (baos.toByteArray().size / 1024 > 1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset()//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 50, baos)//这里压缩50%，把压缩后的数据存放到baos中
        }
        var isBm = ByteArrayInputStream(baos.toByteArray())
        val newOpts = BitmapFactory.Options()
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true
        var bitmap = BitmapFactory.decodeStream(isBm, null, newOpts)
        newOpts.inJustDecodeBounds = false
        val w = newOpts.outWidth
        val h = newOpts.outHeight
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        val hh = 1280f//这里设置高度为800f
        val ww = 720f//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        var be = 1//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (newOpts.outWidth / ww).toInt()
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (newOpts.outHeight / hh).toInt()
        }
        if (be <= 0)
            be = 1
        newOpts.inSampleSize = be//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = ByteArrayInputStream(baos.toByteArray())
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts)
        return compressQuality(bitmap!!)//压缩好比例大小后再进行质量压缩
    }

    /**
     * 图片 质量压缩
     */
    fun compressQuality(image: Bitmap): Bitmap? {

        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        var options = 100
        while (baos.toByteArray().size / 1024 > 100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset()//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos)//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10//每次都减少10
        }
        val isBm = ByteArrayInputStream(baos.toByteArray())//把压缩后的数据baos存放到ByteArrayInputStream中
        return BitmapFactory.decodeStream(isBm, null, null)
    }

    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    fun rotateBitmap(origin: Bitmap?, alpha: Float): Bitmap? {
        if (origin == null) {
            return null
        }
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.setRotate(alpha)
        // 围绕原地进行旋转
        val newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        if (newBM == origin) {
            return newBM
        }
        origin.recycle()
        return newBM
    }
}