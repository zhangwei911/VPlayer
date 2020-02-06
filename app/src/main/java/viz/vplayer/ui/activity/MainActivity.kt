package viz.vplayer.ui.activity

import android.Manifest.permission.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.work.ListenableWorker
import androidx.work.workDataOf
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.listener.ISchedulers
import com.arialyy.aria.util.ALog
import com.arialyy.aria.util.NetUtils
import com.viz.tools.Toast
import com.viz.tools.apk.NetWorkUtils
import com.viz.tools.apk.NetWorkUtils.*
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.commonlib.http.VCallback
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.eventbus.CommonInfoEvent
import viz.vplayer.eventbus.InfoType
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.http.HttpApi
import viz.vplayer.util.CoroutineUtil
import viz.vplayer.util.NetUtil


class MainActivity : BaseActivity(), View.OnClickListener {
    private val navController by lazy { findNavController(R.id.main_content) }

    override fun getContentViewId(): Int = R.layout.activity_main
    override fun useEventBus(): Boolean = true

    override fun getPermissions(): Array<String> = arrayOf(
        WRITE_EXTERNAL_STORAGE,
        ACCESS_NETWORK_STATE,
        ACCESS_WIFI_STATE,
        READ_PHONE_STATE,
        GET_TASKS,
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION,
        ACCESS_LOCATION_EXTRA_COMMANDS,
        ACCESS_MEDIA_LOCATION
    )

    override fun getPermissionsTips(): String = "需要存储,网络,手机信息,悬浮窗,位置等权限"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                if(!BuildConfig.DEBUG) {
                    l.d("移动网络禁止下载")
                    EventBus.getDefault().postSticky(NetEvent(false))
                }
            }
        }
        checkNet()
    }

    fun checkNet() {
        GlobalScope.launch(CoroutineUtil().handler) {
            val isConnect = NetUtil().netCheck()
            EventBus.getDefault().postSticky(NetEvent(isConnect))
            if (!isConnect) {
                EventBus.getDefault()
                    .postSticky(CommonInfoEvent(true, getString(R.string.network_invalid)))
                runOnUiThread {
                    Toast.show(R.string.network_invalid_tips)
                }
            }
        }
    }

    fun changeBottomNavigationViewSelectItem(to: Int) {
        bottomNavigationView_main.selectedItemId = when (to) {
            R.id.homeFragment -> {
                R.id.action_search
            }
            R.id.localFragment -> {
                R.id.action_download
            }
            else -> {
                navController.navigate(to)
                0
            }
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
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {

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
}
