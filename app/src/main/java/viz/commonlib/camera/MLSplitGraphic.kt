package viz.commonlib.camera

import android.graphics.Canvas
import android.graphics.Color
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentation

class MLSplitGraphic(
    private val overlay: GraphicOverlay,
    private val imageSegmentation: MLImageSegmentation
) : GraphicOverlay.Graphic(overlay) {
    override fun draw(canvas: Canvas?) {
        imageSegmentation.foreground?.let {
            canvas?.drawColor(Color.WHITE)
            canvas?.drawBitmap(it, 0f, 0f, null)
        }
    }
}