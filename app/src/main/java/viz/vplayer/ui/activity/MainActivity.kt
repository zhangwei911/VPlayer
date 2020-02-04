package viz.vplayer.ui.activity

import android.Manifest.permission.*
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.activity_main.*
import viz.vplayer.R

class MainActivity : BaseActivity(), View.OnClickListener {
    private val navController by lazy { findNavController(R.id.main_content) }

    override fun getContentViewId(): Int = R.layout.activity_main

    override fun getPermissions(): Array<String> = arrayOf(
        WRITE_EXTERNAL_STORAGE,
        ACCESS_NETWORK_STATE,
        ACCESS_WIFI_STATE,
        READ_PHONE_STATE,
        GET_TASKS,
        SYSTEM_ALERT_WINDOW,
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
}
