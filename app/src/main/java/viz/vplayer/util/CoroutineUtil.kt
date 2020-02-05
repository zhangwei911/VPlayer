package viz.vplayer.util

import com.viz.tools.l
import kotlinx.coroutines.CoroutineExceptionHandler

class CoroutineUtil {
    //自定义CoroutineExceptionHandler示例
    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        throwable.printStackTrace()
        l.e(throwable.localizedMessage)
    }
}