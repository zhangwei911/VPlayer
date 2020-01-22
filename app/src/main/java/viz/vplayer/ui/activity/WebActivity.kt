package viz.vplayer.ui.activity

import android.Manifest
import android.os.Bundle
import android.view.View
import com.viz.tools.Toast
import kotlinx.android.synthetic.main.activity_web.*
import viz.vplayer.R

class WebActivity : BaseActivity(), View.OnClickListener {
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
        materialButton_url.setOnClickListener(this)
        textInputEditText_url.setText("http://v.qq.com")
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {
            R.id.materialButton_url -> {
                val url = textInputEditText_url.text.toString()
                if (url.isNotEmpty()) {
                    webView.loadUrl(url)
                } else {
                    Toast.show(this, R.string.url_hint)
                }
            }
        }
    }
}