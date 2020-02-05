package viz.vplayer.util

import com.viz.tools.l
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.http.HttpApi

class NetUtil {
    suspend fun netCheck(): Boolean {
        val result = withContext(Dispatchers.IO) {
            HttpApi.createHttp().anyUrl("http://www.baidu.com").execute()
        }
        l.d(result.toString())
        val isWifi = if (result.code() == 30000) {
            l.d("网络不可用")
            false
        } else {
            true
        }
        EventBus.getDefault().postSticky(NetEvent(isWifi))
        return isWifi
    }
}