package viz.vplayer

import android.Manifest
import android.os.Bundle

class WebActivity : BaseActivity() {
    override fun getContentViewId(): Int = R.layout.activity_web

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

    }
}