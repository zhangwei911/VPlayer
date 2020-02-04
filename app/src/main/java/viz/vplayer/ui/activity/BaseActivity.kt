package viz.vplayer.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.viz.tools.Toast
import com.viz.tools.l
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import viz.vplayer.R
import viz.vplayer.util.App
import java.io.File


abstract class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    lateinit var app: App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(useEventBus()) {
            if(!EventBus.getDefault().isRegistered(this)) {
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
        setContentView(getContentViewId())
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
            finish()
        }
        findViewById<TextView>(R.id.textView_title)?.text = getCommonTtile()
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

    open protected fun isFullScreen(): Boolean = false
    open protected fun isNoTitle(): Boolean = true

    open protected fun getCommonTtile(): String = ""
    /**
     * 当前布局文件资源
     */
    protected abstract fun getContentViewId(): Int

    /**
     * 需要的权限列表
     */
    open protected fun getPermissions(): Array<String> = arrayOf()

    /**
     * 需要的权限提示文字
     */
    open protected fun getPermissionsTips(): String = ""

    /**
     * 拥有权限时需要执行的部分
     */
    open protected fun hasPermissions() {}

    open protected fun getCustomBackground(): Any {
        return ""
    }

    /**
     * 设置背景
     */
    open protected fun setCustomBackground(cbg: Any) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && perms.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            if(!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            }
        } else {
            AppSettingsDialog.Builder(this).build().show()
//            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }
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
                        if(!Settings.canDrawOverlays(this)){
                            Toast.show(this,"悬浮窗权限没有打开")
                        }else{
                            Toast.show(this,"悬浮窗权限已打开")
                        }
                    }
                }
            }
        }
    }

    open protected fun useEventBus():Boolean = false

    override fun onDestroy() {
        super.onDestroy()
        if(useEventBus()) {
            EventBus.getDefault().unregister(this)
        }
    }
}