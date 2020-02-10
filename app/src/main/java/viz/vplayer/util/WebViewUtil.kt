package viz.vplayer.util

import android.webkit.JavascriptInterface
import com.viz.tools.l
import viz.vplayer.bean.VideoHtmlResultBean

class WebViewUtil {
    fun initWebView(
        webView: X5WebView,
        getVideoHtmlResultBean: () -> VideoHtmlResultBean?,
        getUrl: (url: String) -> Unit
    ) {
        webView.onPageFinished = {
            getVideoHtmlResultBean.invoke()?.apply {
                if (isFromWebView) {
                    webView.loadUrl("javascript:$js")
                }
            }
        }
        webView.addJavascriptInterface(object : WebViewJavaScriptFunction {
            override fun onJsFunctionCalled(tag: String?) {
            }

            @JavascriptInterface
            fun getUrl(url: String) {
                l.d(url)
                getUrl.invoke(url)
            }

            @JavascriptInterface
            fun showSource(html: String) {
                l.d(html)
            }
        }, "htmlGet")
    }
}