package viz.vplayer.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.viz.tools.l
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import viz.vplayer.util.App
import viz.vplayer.R
import java.io.File

abstract class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    lateinit var app: App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as App
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(getContentViewId())
        supportActionBar?.hide()
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

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        l.ifo(requestCode, perms.toString())
        val pn = perms.joinToString { it }
        l.i("需要以下权限:$pn")
        startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        l.ifo(requestCode, perms.toString())
        for (perm in perms) if (perm == Manifest.permission.READ_PHONE_STATE) {
        }
    }
}