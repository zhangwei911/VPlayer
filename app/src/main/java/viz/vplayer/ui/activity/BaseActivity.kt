package viz.vplayer.ui.activity

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.viz.tools.Toast
import com.viz.tools.l
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import viz.commonlib.util.StatusBarUtil
import viz.vplayer.R
import viz.vplayer.util.App
import java.io.File


abstract class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    lateinit var app: App
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        super.onCreate(savedInstanceState)
        if (useEventBus()) {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
        }
        app = application as App
        if (isFullScreen()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        window.setBackgroundDrawableResource(R.drawable.main_bg)
        StatusBarUtil.StatusBarLightMode(this)
        makeStatusBarTransparent(this)
//        window.decorView.fitsSystemWindows = true
        val vlp = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val view = LayoutInflater.from(this).inflate(getContentViewId(), null, false)
        view.setPadding(0, CommonUtil.getStatusBarHeight(this), 0, 0)
        setContentView(view, vlp)
        if (isNoTitle()) {
            supportActionBar?.hide()
        }
        updateBG()
        val permissions = getPermissions()
        if (permissions.isNotEmpty()) {
            if (EasyPermissions.hasPermissions(applicationContext, *permissions)) {
                hasPermissions()
            } else {
                EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(this, 10000, *permissions)
                        .setTheme(R.style.Common_Style_AlertDialog)
                        .setRationale(getPermissionsTips())
                        .build()
                )
            }
        }
        findViewById<ImageButton>(R.id.imageButton_back)?.setOnClickListener {
            getCommonBack()
        }
        setCommonTitle(getCommonTtile())
    }

    private fun makeStatusBarTransparent(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            val option =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.decorView.systemUiVisibility = option
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    open fun updateBG() {
        val cbg = getCustomBackground()
        if (cbg.toString().isNotEmpty()) {
            setCustomBackground(cbg)
        } else {
            val sp = getSharedPreferences("base", Context.MODE_PRIVATE)
            val isGlobalBG = sp.getBoolean("globalBG", false)
            l.d(isGlobalBG)
            if (isGlobalBG) {
                val globalBGType = sp.getInt("globalBGType", -1)
                l.d(globalBGType)
                if (globalBGType == 0) {
                    val bgResId = sp.getInt("globalResId", -1)
                    if (bgResId != -1) {
                        setCustomBackground(bgResId)
                    }
                } else if (globalBGType == 1) {
                    val bgResString = sp.getString("globalResString", "") ?: ""
                    l.d(bgResString)
                    if (bgResString.isNotEmpty()) {
                        setCustomBackground(bgResString)
                    }
                }
            }
        }
    }

    fun setCommonTitle(title: String) {
        findViewById<TextView>(R.id.textView_title)?.text = title
    }

    protected open fun isFullScreen(): Boolean = false
    protected open fun isNoTitle(): Boolean = true

    protected open fun getCommonTtile(): String = ""

    protected open fun getCommonBack() {
        finish()
    }

    /**
     * 当前布局文件资源
     */
    protected abstract fun getContentViewId(): Int

    /**
     * 需要的权限列表
     */
    protected open fun getPermissions(): Array<String> = arrayOf()

    /**
     * 需要的权限提示文字
     */
    protected open fun getPermissionsTips(): String = ""

    /**
     * 拥有权限时需要执行的部分
     */
    protected open fun hasPermissions() {}

    protected open fun getCustomBackground(): Any {
        return ""
    }

    /**
     * 设置背景
     */
    protected open fun setCustomBackground(cbg: Any) {
        try {
            if (cbg is Int && cbg != -1) {
                window.decorView.setBackgroundResource(cbg)
            } else if (cbg is String && cbg.isNotEmpty()) {
                val bgFile = File(cbg)
                if (!bgFile.exists()) {
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    window.decorView.background = Drawable.createFromPath(cbg.toString())
                } else {
                    window.decorView.setBackgroundDrawable(Drawable.createFromPath(cbg.toString()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        l.ifo(requestCode, permissions.toString())
    }

    var OVERLAY_PERMISSION_REQ_CODE = 1234
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        l.ifo(requestCode, perms.toString())
        val pn = perms.joinToString { it }
        l.i("需要以下权限:$pn")
        AppSettingsDialog.Builder(this).build().show()
//            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        l.ifo(requestCode, perms.toString())
        for (perm in perms) if (perm == Manifest.permission.READ_PHONE_STATE) {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQ_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(this)) {
                            Toast.show(this, "悬浮窗权限没有打开")
                        } else {
                            Toast.show(this, "悬浮窗权限已打开")
                        }
                    }
                }
            }
        }
    }

    protected open fun useEventBus(): Boolean = false

    override fun onDestroy() {
        super.onDestroy()
        if (useEventBus()) {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
        }
    }
}