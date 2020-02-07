package viz.vplayer.ui.activity

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import androidx.activity.addCallback
import com.viz.tools.Toast
import kotlinx.android.synthetic.main.activity_web.*
import viz.vplayer.R
import viz.vplayer.util.WebViewJavaScriptFunction
import viz.vplayer.util.getStringExtra


class WebActivity : BaseActivity(), View.OnClickListener {
    override fun getContentViewId(): Int = R.layout.activity_web
    override fun getCommonTtile(): String = "网页"

    override fun getPermissions(): Array<String> = arrayOf(
        Manifest.permission.GET_TASKS,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun getPermissionsTips(): String = "需要权限"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        val url = intent.getStringExtra("url", "")
        if (url.isNotEmpty()) {
            webView.loadUrl(url)
        }

        val callback = onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
                setWebInfo(webView.url)
            } else {
                navigateUpTo(Intent(this@WebActivity, MainActivity::class.java))
            }
        }
    }

    private fun initViews() {
        materialButton_url.setOnClickListener(this)
        initWebView()
    }

    private fun initWebView() {
        window.setFormat(PixelFormat.TRANSLUCENT)
        webView.shouldOverrideUrlLoading = { url ->
            setWebInfo(url)
        }
        webView.onPageFinished = { url ->
            setWebInfo(url)
        }
        webView.view.overScrollMode = View.OVER_SCROLL_ALWAYS
        webView.addJavascriptInterface(object : WebViewJavaScriptFunction {
            override fun onJsFunctionCalled(tag: String?) { // TODO Auto-generated method stub
            }

            @JavascriptInterface
            fun onX5ButtonClicked() {
                enableX5FullscreenFunc()
            }

            @JavascriptInterface
            fun onCustomButtonClicked() {
                disableX5FullscreenFunc()
            }

            @JavascriptInterface
            fun onLiteWndButtonClicked() {
                enableLiteWndFunc()
            }

            @JavascriptInterface
            fun onPageVideoClicked() {
                enablePageVideoFunc()
            }
        }, "Android")
    }

    override fun getCommonBack() {
        navigateUpTo(Intent(this, MainActivity::class.java))
    }

    private fun setWebInfo(url: String) {
        textInputEditText_url.setText(url)
        setCommonTitle(webView.getTitle())
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {
            R.id.materialButton_url -> {
                val url = textInputEditText_url.text.toString()
                if (url.isNotEmpty()) {
                    webView.loadUrl(url)
                } else {
                    Toast.show(this, R.string.url_hint)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) { // TODO Auto-generated method stub
        try {
            super.onConfigurationChanged(newConfig)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // /////////////////////////////////////////
// 向webview发出信息
    private fun enableX5FullscreenFunc() {
        if (webView.x5WebViewExtension != null) {
            Toast.show("开启X5全屏播放模式")
            val data = Bundle()
            data.putBoolean("standardFullScreen", false) // true表示标准全屏，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", false) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            webView.x5WebViewExtension.invokeMiscMethod(
                "setVideoParams",
                data
            )
        }
    }

    private fun disableX5FullscreenFunc() {
        if (webView.x5WebViewExtension != null) {
            Toast.show("恢复webkit初始状态")
            val data = Bundle()
            data.putBoolean(
                "standardFullScreen",
                true
            ) // true表示标准全屏，会调起onShowCustomView()，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", false) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            webView.x5WebViewExtension.invokeMiscMethod(
                "setVideoParams",
                data
            )
        }
    }

    private fun enableLiteWndFunc() {
        if (webView.x5WebViewExtension != null) {
            Toast.show("开启小窗模式")
            val data = Bundle()
            data.putBoolean(
                "standardFullScreen",
                false
            ) // true表示标准全屏，会调起onShowCustomView()，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", true) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            webView.x5WebViewExtension.invokeMiscMethod(
                "setVideoParams",
                data
            )
        }
    }

    private fun enablePageVideoFunc() {
        if (webView.x5WebViewExtension != null) {
            Toast.show("页面内全屏播放模式")
            val data = Bundle()
            data.putBoolean(
                "standardFullScreen",
                false
            ) // true表示标准全屏，会调起onShowCustomView()，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", false) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 1) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            webView.x5WebViewExtension.invokeMiscMethod(
                "setVideoParams",
                data
            )
        }
    }

}
