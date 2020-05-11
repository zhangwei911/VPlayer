package viz.vplayer.ui.fragment

import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_web_parse.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup
import viz.vplayer.R
import viz.vplayer.eventbus.InitEvent
import viz.vplayer.eventbus.enum.INIT_TYPE
import viz.vplayer.util.WebViewJavaScriptFunction

class WebParseFragment : BaseFragment() {
    override fun getFragmentClassName(): String = "WebParseFragment"
    override fun getContentViewId(): Int = R.layout.fragment_web_parse
    override fun useEventBus(): Boolean = true
    private var isInitWebView = false
    private var url =
        "https://www.weixintv8.com/index.php?m=vod-search&wd=%E9%80%9F%E5%BA%A6%E4%B8%8E%E6%BF%80%E6%83%85"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    fun initWebView() {
        isInitWebView = true
        x5WebView_parse.onPageFinished = { url ->
            //            x5WebView_parse.loadUrl(
//                "javascript:var allAElement = document.getElementsByTagName('a');" +
//                        "            for (let index = 0; index < allAElement.length; index++) {" +
//                        "                const element = allAElement[index];" +
//                        "                element.setAttribute('href','javascript:void(0);');" +
//                        "                element.onclick = function(){" +
//                        "                    window.htmlGet.showSource(a.outerHTML)" +
//                        "                };" +
//                        "            }"
//            )

            x5WebView_parse.loadUrl("javascript:var html = '<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>';window.htmlGet.showSource(html);")
        }
        x5WebView_parse.shouldOverrideUrlLoading = { url ->

        }
        x5WebView_parse.view.overScrollMode = View.OVER_SCROLL_ALWAYS
        x5WebView_parse.addJavascriptInterface(object : WebViewJavaScriptFunction {
            override fun onJsFunctionCalled(tag: String?) {
            }

            @JavascriptInterface
            fun showSource(
                html: String
            ) {
                l.d(html)
                val doc = Jsoup.parse(html)
                val uls = doc.select("ul")
                val allLi = mutableListOf<String>()
                uls?.forEach { ul ->
                    val lis = ul.select("li")
                    lis?.forEach { li ->
                        val a = li.select("a")
                        val href = if (a != null) {
                            a[0].attr("href")
                        } else {
                            "[null]"
                        }
                        val img = li.select("img")
                        val imgUrl = if (img != null) {
                            img[0].attr("src")
                        } else {
                            "[null]"
                        }
                        val text = li.text() ?: "[null]"
                        allLi.add("href=$href imgUrl=$imgUrl text=$text")
                    }
                }
                l.d(allLi)
            }
        }, "htmlGet")
    }

    override fun onResume() {
        super.onResume()
        if (isInitWebView) {
            x5WebView_parse.loadUrl(url)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun initEvent(initEvent: InitEvent) {
        initEvent?.apply {
            when (type) {
                INIT_TYPE.TBS -> {
                    initWebView()
                    x5WebView_parse.loadUrl(url)
                }
            }
        }
    }
}
