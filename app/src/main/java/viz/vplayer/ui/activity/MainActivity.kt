package viz.vplayer.ui.activity

import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.BounceInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.get
import androidx.navigation.ui.NavigationUI
import bolts.Continuation
import bolts.Task
import bolts.Task.UI_THREAD_EXECUTOR
import com.google.android.gms.ads.*
import com.huawei.hms.analytics.HiAnalytics
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.silencedut.fpsviewer.FpsViewer
import com.viz.tools.Toast
import com.viz.tools.apk.NetWorkUtils.*
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.xutils.common.Callback
import org.xutils.http.HttpMethod
import org.xutils.http.HttpTask
import org.xutils.http.RequestParams
import org.xutils.x
import viz.commonlib.event.ScanEvent
import viz.commonlib.event.SignEvent
import viz.commonlib.huawei.account.LoginUtil
import viz.commonlib.util.MyObserver
import viz.commonlib.util.REQUEST_CODE_SCAN_ONE
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.dagger2.MyObserverModule
import viz.vplayer.eventbus.*
import viz.vplayer.util.FileUtil
import viz.vplayer.util.NetUtil
import viz.vplayer.util.UrlUtil
import viz.vplayer.util.continueWithEnd
import viz.vplayer.video.FloatPlayerView
import viz.vplayer.video.FloatWindow
import viz.vplayer.video.MoveType
import viz.vplayer.video.Screen
import viz.vplayer.vm.HtmlVM
import viz.vplayer.vm.MainVM
import java.io.File
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.inject.Inject


class MainActivity : BaseActivity(), View.OnClickListener {
    @Inject
    lateinit var mo: MyObserver
    private val navController by lazy { findNavController(R.id.main_content) }
    private var loginUtil: LoginUtil? = null
    private var adView: AdView? = null
    private val adColseTime = 10

    override fun getContentViewId(): Int = R.layout.activity_main
    override fun useEventBus(): Boolean = true
    override fun isSetPaddingTop(): Boolean = true
    private var es: ScheduledExecutorService = ScheduledThreadPoolExecutor(
        1,
        BasicThreadFactory.Builder().namingPattern("pool-fd-%d").daemon(true).build()
    )

    override fun getPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                WRITE_EXTERNAL_STORAGE,
                ACCESS_NETWORK_STATE,
                ACCESS_WIFI_STATE,
                READ_PHONE_STATE,
                GET_TASKS,
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                ACCESS_LOCATION_EXTRA_COMMANDS,
                ACCESS_MEDIA_LOCATION,
                READ_EXTERNAL_STORAGE,
                CAMERA
            )
        } else {
            arrayOf(
                WRITE_EXTERNAL_STORAGE,
                ACCESS_NETWORK_STATE,
                ACCESS_WIFI_STATE,
                READ_PHONE_STATE,
                GET_TASKS,
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                ACCESS_LOCATION_EXTRA_COMMANDS,
                READ_EXTERNAL_STORAGE,
                CAMERA
            )
        }

    override fun getPermissionsTips(): String = "需要存储,网络,手机信息,悬浮窗,位置等权限"

    private lateinit var htmlVM: HtmlVM
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        htmlVM = ViewModelProvider(this).get(HtmlVM::class.java)
//        htmlVM.autoGet("https://www.weixintv8.com/index.php?m=vod-search&wd=%E9%80%9F%E5%BA%A6%E4%B8%8E%E6%BF%80%E6%83%85")
//        htmlVM.autoGet("http://www.lexianglive.com/index.php?s=vod-search-name&wd=%E9%80%9F%E5%BA%A6%E4%B8%8E%E6%BF%80%E6%83%85")
//        htmlVM.autoGet("http://5nj.com/index.php?m=vod-search&wd=%E9%80%9F%E5%BA%A6%E4%B8%8E%E6%BF%80%E6%83%85")
        htmlVM.autoGet("https://so.iqiyi.com/so/q_%E9%80%9F%E5%BA%A6%E4%B8%8E%E6%BF%80%E6%83%85")
        FpsViewer.getViewer().appendSection("MainActivity")
        Task.callInBackground {
            l.start("华为分析")
            //        HiAnalyticsTools.enableLog()
            val hiAnalyticsInstance = HiAnalytics.getInstance(this)
            hiAnalyticsInstance.setAutoCollectionEnabled(true)
            l.end("华为分析")
        }.continueWithEnd("初始化华为分析")
        l.start("mainactivity")
        app.appComponent!!.mainActivitySubcomponentBuilder()
            .myObserverModule(MyObserverModule(lifecycle, javaClass.name))
            .create(this)
            .inject(this)
        initViews()
        initListener()

        val to = intent.getIntExtra("to", -1)
        if (to != -1) {
            changeBottomNavigationViewSelectItem(to)
        }
        val netWorkType = getNetWorkType(applicationContext)
        when (netWorkType) {
            NETWORK_TYPE_WIFI -> {
            }
            NETWORK_TYPE_UNKNOWN -> {
            }
            NETWORK_TYPE_DISCONNECT -> {
                l.d("无网络")
                EventBus.getDefault().postSticky(NetEvent(false))
            }
            else -> {
                if (!BuildConfig.DEBUG) {
                    l.d("移动网络禁止下载")
                    EventBus.getDefault().postSticky(NetEvent(false))
                }
            }
        }
        checkNet()
//        Task.callInBackground {
//            val vDownloader = VDownloader()
//            vDownloader.init(applicationContext)
//            vDownloader.apply {
////                load("http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8")
////                load("https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8")
////                load("http://192.168.1.7/index.m3u8")
//                load("http://192.168.1.7/index1.m3u8")
//                start()
////                merge()
//            }
//        }.continueWithEnd("下载M3U8")
        loginUtil = LoginUtil(this)
        l.end("mainactivity")
        Task.callInBackground {
            MobileAds.initialize(this) {}
            val adRequest = AdRequest.Builder().build()
            runOnUiThread {
                if (adView == null) {
                    adView = AdView(this)
                    adView!!.id = R.id.adView
                    constraintLayout_main.addView(
                        adView,
                        constraintLayout_main.indexOfChild(imageButton_close_ads) - 1
                    )
                    val cs = ConstraintSet()
                    cs.clone(constraintLayout_main)
                    cs.connect(
                        adView!!.id,
                        ConstraintSet.START,
                        R.id.constraintLayout_main,
                        ConstraintSet.START
                    )
                    cs.connect(
                        adView!!.id,
                        ConstraintSet.END,
                        R.id.constraintLayout_main,
                        ConstraintSet.END
                    )
                    cs.connect(
                        adView!!.id,
                        ConstraintSet.BOTTOM,
                        R.id.textView_version,
                        ConstraintSet.TOP
                    )
                    cs.connect(
                        R.id.imageButton_close_ads,
                        ConstraintSet.END,
                        R.id.constraintLayout_main,
                        ConstraintSet.END
                    )
                    cs.connect(
                        R.id.imageButton_close_ads,
                        ConstraintSet.BOTTOM,
                        adView!!.id,
                        ConstraintSet.TOP
                    )
                    cs.applyTo(constraintLayout_main)
                    adView!!.adSize = adSize
                    adView!!.adUnitId = if (BuildConfig.DEBUG) {
                        "ca-app-pub-3940256099942544/6300978111"
                    } else {
                        "ca-app-pub-2409784170808286/3785657334"
                    }
                    adView!!.adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            imageButton_close_ads.visibility = View.VISIBLE
                            Toast.show("广告将在${adColseTime}s后关闭")
                            Task.delay(adColseTime * 1000L).continueWith(Continuation<Void, Void> {
                                imageButton_close_ads.performClick()
                                return@Continuation null
                            }, UI_THREAD_EXECUTOR)
                        }
                    }
                    adView!!.loadAd(adRequest)
                }
            }
        }.continueWithEnd("初始化google ads")
//        Task.callInBackground {
//            val params =
//                RequestParams("https://gnzk.yiya520.com/2019/08/08/HCDLq8fKK2XhlyL2/out003.ts")
//            params.isAutoResume = true
//            params.isAutoRename = true
//            params.saveFilePath = FileUtil.getPath(this) + "/$packageName/${UUID.randomUUID()}.ts"
//            params.executor = es
//            params.isCancelFast = true
//            params.method = HttpMethod.GET
//            val callback = object : Callback.CommonCallback<File> {
//                override fun onFinished() {
//                    l.d()
//                }
//
//                override fun onSuccess(result: File?) {
//                    if (result != null) {
//                        l.d(result.absolutePath)
//                    }
//                }
//
//                override fun onCancelled(cex: Callback.CancelledException?) {
//                    l.e()
//                }
//
//                override fun onError(ex: Throwable?, isOnCallback: Boolean) {
//                    ex?.printStackTrace()
//                }
//            }
//            val httpTask = HttpTask<File>(params, null, callback)
//            x.task().startTasks(object : Callback.GroupCallback<HttpTask<File>> {
//                override fun onFinished(item: HttpTask<File>?) {
//                    l.d()
//                }
//
//                override fun onSuccess(item: HttpTask<File>?) {
//                    l.d()
//                }
//
//                override fun onCancelled(item: HttpTask<File>?, cex: Callback.CancelledException?) {
//                    l.e()
//                }
//
//                override fun onAllFinished() {
//                    l.d()
//                }
//
//                override fun onError(item: HttpTask<File>?, ex: Throwable?, isOnCallback: Boolean) {
//                    ex?.printStackTrace()
//                }
//            }, httpTask)
//        }.continueWithEnd("xutil3下载测试")
    }

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = constraintLayout_main.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    fun checkNet() {
        GlobalScope.launch() {
            try {
                val isConnect = NetUtil().netCheck()
                EventBus.getDefault().postSticky(NetEvent(isConnect))
                if (!isConnect) {
                    EventBus.getDefault()
                        .postSticky(CommonInfoEvent(true, getString(R.string.network_invalid)))
                    runOnUiThread {
                        Toast.show(R.string.network_invalid_tips)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeBottomNavigationViewSelectItem(to: Int) {
        val sii = when (to) {
            navController.graph[R.id.homeFragment].id -> {
                R.id.action_search
            }
            navController.graph[R.id.localFragment].id -> {
                R.id.action_download
            }
            R.id.homeFragment -> {
                R.id.action_search
            }
            R.id.localFragment -> {
                R.id.action_download
            }
            0 -> {
                R.id.action_search
            }
            1 -> {
                R.id.action_download
            }
            else -> {
                -1
            }
        }
        if (sii != -1) {
            bottomNavigationView_main.selectedItemId = sii
        }
    }

    fun initViews() {
        NavigationUI.setupWithNavController(bottomNavigationView_main, navController)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            changeBottomNavigationViewSelectItem(destination.id)
        }
        textView_version.text = "版本号:" + packageManager.getPackageInfo(packageName, 0).versionName
    }

    private fun initListener() {
        bottomNavigationView_main.setOnNavigationItemSelectedListener { item ->
            val itemId = item.itemId
            when (itemId) {
                R.id.action_search -> {
                    if (navController.currentDestination!!.id != R.id.homeFragment) {
                        navController.navigate(R.id.homeFragment)
                    }
                    true
                }
                R.id.action_download -> {
                    if (navController.currentDestination!!.id != R.id.localFragment) {
                        navController.navigate(R.id.localFragment)
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
        bottomNavigationView_main.setOnNavigationItemReselectedListener { item ->
            navController.apply {
                currentDestination?.apply {
                    popBackStack(id, false)
                }
            }
        }
        imageButton_close_ads.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {
            R.id.imageButton_close_ads -> {
                adView?.destroy()
                adView?.visibility = View.GONE
                imageButton_close_ads.visibility = View.GONE
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun commonInfo(commonInfoEvent: CommonInfoEvent) {
        commonInfoEvent.apply {
            textView_common_info.visibility = if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
            textView_common_info.text = text
            textView_common_info.setTextColor(textColor)
            textView_common_info.setBackgroundColor(backgroundColor)
            if (show) {
                textView_common_info.setOnClickListener {
                    when (type) {
                        InfoType.NETWORK -> {
                            checkNet()
                        }
                    }
                }
            } else {
                textView_common_info.setOnClickListener(null)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun mlEvent(mlEvent: MLEvent) {
        mlEvent?.apply {
            when (type) {
                0 -> {
                    navController.navigate(R.id.cameraFragment)
                }
                1 -> {
                    navController.navigate(R.id.splitFragment)
                }
                2 -> {
                    navController.navigate(R.id.splitHiAIFragment)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun testEvent(testEvent: TestEvent) {
        testEvent?.apply {
            when (type) {
                0 -> {
                    navController.navigate(R.id.webParseFragment)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun signEvent(signEvent: SignEvent) {
        signEvent?.apply {
            when (type) {
                0 -> {
                    loginUtil?.signIn()
                }
                1 -> {
                    loginUtil?.signInCode()
                }
                2 -> {
                    loginUtil?.silentSignIn()
                }
                3 -> {
                    loginUtil?.silentSignInCode()
                }
                4 -> {
                    loginUtil?.signOut()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun scanEvent(scanEvent: ScanEvent) {
        scanEvent?.apply {
            when (requestCode) {
                REQUEST_CODE_SCAN_ONE -> {
                    ScanUtil.startScan(this@MainActivity, requestCode, null)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            val obj: HmsScan = data.getParcelableExtra(ScanUtil.RESULT)
            obj?.apply {
                l.d(originalValue)
                when (scanTypeForm) {
                    HmsScan.URL_FORM -> {
                        l.d(linkUrl)
                        val bundle = Bundle()
                        bundle.putString(
                            "url",
                            originalValue
                        )
                        navController.navigate(R.id.webActivity, bundle)
                    }
                    HmsScan.PURE_TEXT_FORM -> {
                        EventBus.getDefault().postSticky(KWEvent(originalValue))
                    }
                }
            }
        }
        supportFragmentManager.fragments.forEachIndexed { index, fragment ->
            fragment.onActivityResult(requestCode, resultCode, data)
        }
        loginUtil?.handleOnActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        FpsViewer.getViewer().removeSection("MainActivity")
    }
}
