package viz.vplayer.ui.fragment

import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLAnalyzer.MLTransactor
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentation
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationAnalyzer
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_split.*
import viz.commonlib.camera.MLSplitGraphic
import viz.vplayer.R
import java.io.IOException


class SplitFragment : BaseFragment() {
    override fun getFragmentClassName(): String = "SplitFragment"
    override fun getContentViewId(): Int = R.layout.fragment_split
    private var analyzer:MLImageSegmentationAnalyzer?=null
    private var mLensEngine: LensEngine? = null
    private val lensType = LensEngine.FRONT_LENS
    var mlsNeedToDetect = true
    private var isStarted = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        createAnalyzer()
        createLensEngine()
    }

    private fun createAnalyzer(){
        analyzer = MLAnalyzerFactory.getInstance().imageSegmentationAnalyzer
        analyzer?.setTransactor(object : MLTransactor<MLImageSegmentation> {
            override fun transactResult(results: MLAnalyzer.Result<MLImageSegmentation?>) {
                val items: SparseArray<MLImageSegmentation?> = results.analyseList
                l.d(items.toString())
                items.forEach { key, value ->
                    val mlSplitGraphic = MLSplitGraphic(graphicOverlay,value!!)
                    graphicOverlay.add(mlSplitGraphic)
                }
            }

            override fun destroy() {
            }

        })
    }

    private fun createLensEngine() {
        // Create LensEngine
        this.mLensEngine = LensEngine.Creator(activity!!.applicationContext, analyzer).setLensType(this.lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0f)
            .enableAutomaticFocus(true)
            .create()
    }

    private fun startLensEngine() {
        if (mLensEngine != null) {
            try {
                lensEnginePreview.start(mLensEngine, graphicOverlay)
            } catch (e: IOException) {
                l.e(
                    "Failed to start lens engine.${e.message}"
                )
                mLensEngine!!.release()
                mLensEngine = null
            }
        }
    }


    private fun stopPreview() {
        this.mlsNeedToDetect = false
        if (mLensEngine != null) {
            mLensEngine!!.release()
        }
        if (analyzer != null) {
            try {
                analyzer!!.stop()
            } catch (e: IOException) {
                Log.d("object", "Stop failed: " + e.message)
            }
        }
        this.isStarted = false
    }

    private fun startPreview() {
        if (this.isStarted) {
            return
        }
        this.createAnalyzer()
        this.lensEnginePreview.release()
        createLensEngine()
        startLensEngine()
        this.isStarted = true
    }

    override fun onResume() {
        super.onResume()
        startLensEngine()
    }

    override fun onPause() {
        super.onPause()
        this.lensEnginePreview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLensEngine != null) {
            mLensEngine!!.release()
        }
        if (analyzer != null) {
            try {
                analyzer!!.stop()
            } catch (e: IOException) {
                l.e(
                    "Stop failed: " + e.message
                )
            }
        }
    }
}