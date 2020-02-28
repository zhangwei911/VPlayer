package viz.vplayer.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.viz.tools.Toast
import com.viz.tools.l
import org.greenrobot.eventbus.EventBus
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import viz.commonlib.util.MyObserver
import viz.vplayer.R
import viz.vplayer.util.App
import java.io.File


abstract class BaseFragment : Fragment(), EasyPermissions.PermissionCallbacks {
    lateinit var mo: MyObserver
    lateinit var app: App
    var isWifi = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mo = MyObserver(lifecycle, getFragmentClassName())
        val v = inflater.inflate(getContentViewId(), container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(useEventBus()) {
            if(!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
        }
        app = activity!!.application as App
        updateBG()
        val permissions = getPermissions()
        if (permissions.isNotEmpty()) {
            if (EasyPermissions.hasPermissions(app.applicationContext, *permissions)) {
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
        view!!.findViewById<ImageButton>(R.id.imageButton_back)?.setOnClickListener {
            activity?.finish()
        }
        view!!.findViewById<TextView>(R.id.textView_title)?.text = getCommonTtile()

    }

    open fun updateBG() {
        val cbg = getCustomBackground()
        if (cbg.toString().isNotEmpty()) {
            setCustomBackground(cbg)
        } else {
            val sp = activity!!.getSharedPreferences("base", Context.MODE_PRIVATE)
            val isGlobalBG = sp.getBoolean("globalBGF", false)
            l.d(isGlobalBG)
            if (isGlobalBG) {
                val globalBGType = sp.getInt("globalBGTypeF", -1)
                l.d(globalBGType)
                if (globalBGType == 0) {
                    val bgResId = sp.getInt("globalResIdF", -1)
                    if (bgResId != -1) {
                        setCustomBackground(bgResId)
                    }
                } else if (globalBGType == 1) {
                    val bgResString = sp.getString("globalResStringF", "") ?: ""
                    l.d(bgResString)
                    if (bgResString.isNotEmpty()) {
                        setCustomBackground(bgResString)
                    }
                }
            }
        }
    }

    protected abstract fun getFragmentClassName(): String

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
                view?.setBackgroundResource(cbg)
            } else if (cbg is String && cbg.isNotEmpty()) {
                val bgFile = File(cbg)
                if (!bgFile.exists()) {
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view?.background = Drawable.createFromPath(cbg.toString())
                } else {
                    view?.setBackgroundDrawable(Drawable.createFromPath(cbg.toString()))
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
            if(!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity!!.packageName}")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            }
        } else {
            startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
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
                        if(!Settings.canDrawOverlays(context)){
                            Toast.show(context,"悬浮窗权限没有打开")
                        }else{
                            Toast.show(context,"悬浮窗权限已打开")
                        }
                    }
                }
            }
        }
    }

    open protected fun useEventBus():Boolean = false

    fun handleOnActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        childFragmentManager.fragments.forEachIndexed { index, fragment ->
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(useEventBus()) {
            EventBus.getDefault().unregister(this)
        }
    }
}