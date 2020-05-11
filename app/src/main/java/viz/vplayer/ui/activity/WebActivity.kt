package viz.vplayer.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import androidx.activity.addCallback
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.tencent.smtt.sdk.QbSdk
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_web.*
import okhttp3.ResponseBody
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import viz.commonlib.http.VCallback
import viz.commonlib.util.MyObserver
import viz.vplayer.R
import viz.vplayer.dagger2.MyObserverModule
import viz.vplayer.eventbus.InitEvent
import viz.vplayer.eventbus.TBSEvent
import viz.vplayer.eventbus.enum.INIT_TYPE
import viz.vplayer.http.HttpApi
import viz.vplayer.util.*
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import javax.inject.Inject


class WebActivity : BaseActivity(), View.OnClickListener {
    @Inject
    lateinit var mo: MyObserver

    override fun getContentViewId(): Int = R.layout.activity_web
    override fun getCommonTtile(): String = "网页"
    override fun isSetPaddingTop(): Boolean = true

    private val parseUrlList = mutableListOf<String>()
    private var parseUrl = ""
    private var isParse = false
    private var originalUrl = ""

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
        app.appComponent!!.webActivitySubcomponentBuilder()
            .myObserverModule(MyObserverModule(lifecycle, javaClass.name))
            .create(this)
            .inject(this)
        initViews()
        getParseUrl()
        if(QbSdk.isTbsCoreInited()) {
            val url = intent.getStringExtra("url", "")
            if (url.isNotEmpty()) {
                webView.loadUrl(url)
            }
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
        materialButton_url_parse.setOnClickListener(this)
        materialButton_url_parse.setOnLongClickListener {
            MaterialDialog(this@WebActivity).show {
                listItems(items = parseUrlList) { dialog, index, text ->
                    parseUrl = text.toString()
                    val sp = getSharedPreferences(WEB_SP, Context.MODE_PRIVATE)
                    sp.edit(commit = true) {
                        putString(PARSE_URL_SP, parseUrl)
                    }
                }
            }
            return@setOnLongClickListener true
        }
    }

    private fun getParseUrl() {
        Task.callInBackground {
            try {
                HttpApi.createHttp().anyUrl(DEFAULT_PARSE_URL)
                    .enqueue(VCallback<ResponseBody>(onResult = { call, response, result ->
                        var rBody: String? = null
                        val responseBody = response.body()
                        val UTF8 = Charset.forName("UTF-8")
                        if (responseBody != null) {
                            val source = responseBody!!.source()
                            source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
                            val buffer = source.buffer()

                            var charset = UTF8
                            val contentType = responseBody.contentType()
                            if (contentType != null) {
                                try {
                                    charset = contentType.charset(UTF8)
                                } catch (e: UnsupportedCharsetException) {
                                    e.printStackTrace()
                                }

                            }
                            rBody = buffer.clone().readString(charset)
                        } else {
                            Toast.show("获取parseUrl数据为空")
                            return@VCallback
                        }
                        if (rBody.isJson()) {
                            val jsonArr = JSONArray(rBody)
                            parseUrlList.clear()
                            val sp = getSharedPreferences(WEB_SP, Context.MODE_PRIVATE)
                            parseUrl = sp.getString(PARSE_URL_SP, "") ?: ""
                            for (index in 0 until jsonArr.length()) {
                                parseUrlList.add(jsonArr[index].toString())
                            }
                            if (!parseUrlList.contains(parseUrl)) {
                                parseUrl = parseUrlList[0]
                            }
                        } else {
                            Toast.show("解析parseUrl数据异常")
                        }
                    }, onError = { errorEntity, call, t, response ->
                        Toast.show("解析parseUrl(${errorEntity.error}-${errorEntity.message})")
                    }))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.continueWithEnd("抓取网页")
    }

    private fun initWebView() {
        window.setFormat(PixelFormat.TRANSLUCENT)
        webView.shouldOverrideUrlLoading = { url ->
            setWebInfo(url)
        }
        webView.onPageFinished = { url ->
            setWebInfo(url)
            try {
                val videoManagerClass =
                    Class.forName("com.tencent.mtt.video.internal.engine.VideoManager")
                val videoManagerNewInstance = videoManagerClass.newInstance()
                val ins = videoManagerClass.getMethod("getInstance")
                val videoManagerInstance = ins.invoke(videoManagerNewInstance)
                val urlMethod = videoManagerClass.getMethod("getCurrentVideoUrl")
                val currentUrl = urlMethod.invoke(videoManagerInstance) ?: "null"
                l.d(currentUrl.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
//            webView.loadUrl(
//                "javascript:var html = '<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>';" +
//                        "var api = html.match('\\.post\\\\(\"(.*)\", \\{')[1];" +
//                        "var wap = html.match('\"wap\": \"(.*)\",')[1];" +
//                        "var ip = html.match('\"ip\": \"([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\"')[1];" +
//                        " \$.post(api, {\n" +
//                        "                    \"wap\": wap,\n" +
//                        "                    \"time\": m3,\n" +
//                        "                    \"ip\": ip,\n" +
//                        "                    \"key\": m1,\n" +
//                        "                    \"key2\": m2,\n" +
//                        "                    \"key3\": ഈഇആഅ,\n" +
//                        "                    \"url\": h2_url\n" +
//                        "                }, function(data) {window.htmlGet.showSource(ഈഇആഅ,data['url'],window.location.href,m1,m2,m3,h2_url,html);},'json')"
//            )
//            webView.loadUrl(
//                "javascript:var html = '<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>';" +
//                        "var api = html.match('\\.post\\\\(\"(.*)\", \\{')[1];" +
//                        "var wap = html.match('\"wap\": \"(.*)\",')[1];" +
//                        "var ip = html.match('\"ip\": \"([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\"')[1];" +
//                        " \$.post(api, {\n" +
//                        "                    \"wap\": wap,\n" +
//                        "                    \"time\": m3,\n" +
//                        "                    \"ip\": ip,\n" +
//                        "                    \"key\": m1,\n" +
//                        "                    \"key2\": m2,\n" +
//                        "                    \"key3\": ഈഇആഅ,\n" +
//                        "                    \"url\": h2_url\n" +
//                        "                }, function(data) {window.htmlGet.getUrl(data['url']);},'json')"
//            )
//            webView.loadUrl("javascript:window.htmlGet.showSource(window.sessionStorage);")
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
        webView.addJavascriptInterface(object : WebViewJavaScriptFunction {
            override fun onJsFunctionCalled(tag: String?) {
            }

            @JavascriptInterface
            fun getUrl(url: String) {
                l.d(url)
            }

            @JavascriptInterface
            fun showSource(
                test: String,
                videoUrl: String,
                currentUrl: String,
                m1: String,
                m2: String,
                m3: String,
                h2_url: String,
                html: String
            ) {
//                l.d(test)
//                l.df(videoUrl, m1, m2, m3, h2_url)
//                val ipReg = Regex("\"ip\": \"([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\"")
//                val ip = ipReg.find(html)!!.groupValues[1]
//                val key3Reg = Regex("\"key3\": (.*),\n")
//                val key3 = key3Reg.find(html)!!.groupValues[1]
//                val wapReg = Regex("\"wap\": \"(.*)\",")
//                val wap = wapReg.find(html)!!.groupValues[1]
//                val urlReg = Regex("\\$\\.post\\(\"(.*)\", \\{")
//                val postUrl = urlReg.find(html)!!.groupValues[1]
//                val aUrl = URL(currentUrl)
//                val parseUrl = URL(aUrl, postUrl)
//                val params = mutableMapOf<String, String>()
//                params["ip"] = ip
//                params["key"] = m1
//                params["key2"] = m2
//                params["key3"] = test
//                params["time"] = m3
//                params["wap"] = wap
//                params["url"] = h2_url
//                HttpApi.createHttp().anyPostUrl(parseUrl.toString(),params).enqueue(VCallback<ResponseBody>(onResult = {call, response, resultResponseBody ->
//                    val result = resultResponseBody.string()
//                    l.d(result)
//                    val jsonObject = JSONObject(result)
//                    val code = jsonObject.get("success")
//                    if(code.toString() == "1") {
//                        val url = jsonObject.get("url")
//                        l.d(url)
//                    }
//                },onError = {errorEntity, call, t, response ->
//                    l.e(errorEntity)
//                    t?.printStackTrace()
//                }))
            }
        }, "htmlGet")
        webView.setOnLongClickListener {
            val hitTestResult = webView.getHitTestResult()
            l.d(hitTestResult.extra)
            l.d(hitTestResult.type)
            return@setOnLongClickListener true
        }
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
                    if (url.startsWith("http")
                        || url.startsWith("https")
                        || url.startsWith("file")
                        || url.startsWith("javascript")
                    ) {
                        webView.loadUrl(url)
                    } else {
                        webView.loadUrl("https://www.baidu.com/s?ie=UTF-8&wd=$url")
                    }
                } else {
                    Toast.show(this, R.string.url_hint)
                }
            }
            R.id.materialButton_url_parse -> {
                if (parseUrl.isEmpty()) {
                    Toast.show("解析地址为空,无法解析")
                }
                val finalUrl = String.format(parseUrl, webView.url)
                if (Uri.parse(webView.url).host == Uri.parse(finalUrl).host) {
                    webView.loadUrl(webView.url)
                } else {
                    if(originalUrl.isEmpty()){
                        originalUrl = webView.url
                    }
                    webView.loadUrl(String.format(parseUrl, originalUrl))
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun initEvent(initEvent: InitEvent) {
        initEvent?.apply {
            when (type) {
                INIT_TYPE.TBS -> {
                    initWebView()
                    val url = intent.getStringExtra("url", "")
                    if (url.isNotEmpty()) {
                        webView.loadUrl(url)
                    }
                }
            }
        }
    }
}
