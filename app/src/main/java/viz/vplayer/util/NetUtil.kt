package viz.vplayer.util

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.RequiresPermission
import com.viz.tools.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import viz.vplayer.R
import viz.vplayer.eventbus.CommonInfoEvent
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.http.HttpApi


class NetUtil {
    suspend fun <T : Any> apiCall(call: suspend () -> T): T {
        return withContext(Dispatchers.IO) { call.invoke() }.apply {
            // 特殊处理

        }
    }

    suspend fun netCheck(): Boolean {
        try {
            val result = apiCall {
                HttpApi.createHttp(addGsonConverterFactory = false).anyUrlS("")
            }
            l.d(result)
            val isWifi = if (result.code() == 30000) {
                l.d("网络不可用")
                false
            } else {
                true
            }
            EventBus.getDefault().postSticky(NetEvent(isWifi))
            EventBus.getDefault()
                .postSticky(
                    CommonInfoEvent(
                        !isWifi,
                        App.instance.getString(R.string.network_invalid)
                    )
                )
            return isWifi
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }


    /**
     * 网络是否已连接
     *
     * @return true:已连接 false:未连接
     */
    @RequiresPermission(ACCESS_NETWORK_STATE)
    fun iConnected(@NonNull context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities =
                    manager.getNetworkCapabilities(manager.activeNetwork)
                if (networkCapabilities != null) {
                    return (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                }
            } else {
                val networkInfo = manager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        }
        return false
    }

    /**
     * Wifi是否已连接
     *
     * @return true:已连接 false:未连接
     */
    fun isWifiConnected(@NonNull context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities =
                    manager.getNetworkCapabilities(manager.activeNetwork)
                if (networkCapabilities != null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
            } else {
                val networkInfo = manager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
            }
        }
        return false
    }

    /**
     * 是否为流量
     */
    fun isMobileData(@NonNull context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities =
                    manager.getNetworkCapabilities(manager.activeNetwork)
                if (networkCapabilities != null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            } else {
                val networkInfo = manager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_MOBILE
            }
        }
        return false
    }
}