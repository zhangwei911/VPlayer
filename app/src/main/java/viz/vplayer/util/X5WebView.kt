package viz.vplayer.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Process
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.net.URLEncoder

class X5WebView : WebView {
    var title: TextView? = null
    private val client: WebViewClient =
        object : WebViewClient() {
            /**
             * 防止加载网页时调起系统浏览器
             */
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {
                com.viz.tools.l.d(url)
                view.loadUrl(
                    if (url.startsWith("https://v.qq.com/x/cover")||url.startsWith("https://m.v.qq.com/x")) {
                        "http://vip.jlsprh.com/index.php?url=" + URLEncoder.encode(url, "UTF-8")
                    } else {
                        url
                    }
                )
                return true
            }

            override fun onPageFinished(p0: WebView?, p1: String?) {
                super.onPageFinished(p0, p1)
                addImageClickListner()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    constructor(arg0: Context?, arg1: AttributeSet?) : super(
        arg0,
        arg1
    ) {
        this.webViewClient = client
        // this.setWebChromeClient(chromeClient);
// WebStorage webStorage = WebStorage.getInstance();
        initWebViewSettings()
        this.view.isClickable = true
    }

    // 注入js函数监听
    private fun addImageClickListner() {
        // 这段js函数的功能就是，遍历所有的img几点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
        this.loadUrl("javascript:(function(){" +
                "var objs = document.getElementsByTagName(\"a\"); " +
                "for(var i=0;i<objs.length;i++)  " +
                "{"
                + "    objs[i].οnclick=function()  " +
                "    {  "
                + "        window.longClick.onJsFunctionCalled(this.innerHTML);  " +
                "    }  " +
                "}" +
                "})()")
    }
    private fun initWebViewSettings() {
        val webSetting = this.settings
        webSetting.javaScriptEnabled = true
        this.addJavascriptInterface(object: WebViewJavaScriptFunction{
            override fun onJsFunctionCalled(tag: String?) {
                com.viz.tools.l.d(tag)
            }
        },"longClick")
        webSetting.javaScriptCanOpenWindowsAutomatically = true
        webSetting.allowFileAccess = true
        webSetting.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        webSetting.setSupportZoom(true)
        webSetting.builtInZoomControls = true
        webSetting.useWideViewPort = true
        webSetting.setSupportMultipleWindows(true)
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true)
        // webSetting.setDatabaseEnabled(true);
        webSetting.domStorageEnabled = true
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
        webSetting.pluginState = WebSettings.PluginState.ON_DEMAND
        // webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSetting.cacheMode = WebSettings.LOAD_NO_CACHE
        // this.getSettingsExtension().setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//extension
// settings 的设计
    }

    override fun drawChild(
        canvas: Canvas,
        child: View,
        drawingTime: Long
    ): Boolean {
        val ret = super.drawChild(canvas, child, drawingTime)
        canvas.save()
        val paint = Paint()
        paint.color = 0x7fff0000
        paint.textSize = 24f
        paint.isAntiAlias = true
        if (x5WebViewExtension != null) {
            canvas.drawText(
                this.context.packageName + "-pid:"
                        + Process.myPid(), 10f, 50f, paint
            )
            canvas.drawText(
                "X5  Core:" + QbSdk.getTbsVersion(this.context), 10f, 100f, paint
            )
        } else {
            canvas.drawText(
                this.context.packageName + "-pid:"
                        + Process.myPid(), 10f, 50f, paint
            )
            canvas.drawText("Sys Core", 10f, 100f, paint)
        }
        canvas.drawText(Build.MANUFACTURER, 10f, 150f, paint)
        canvas.drawText(Build.MODEL, 10f, 200f, paint)
        canvas.restore()
        return ret
    }

    constructor(arg0: Context?) : super(arg0) {
        setBackgroundColor(85621)
    }
}