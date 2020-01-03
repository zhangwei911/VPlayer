package viz.vplayer.glide

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OkHttpUtils {
    companion object {
        var TIMEOUT = 15
        private var client: OkHttpClient? = null
        fun getClient(): OkHttpClient {
            if (client == null) {
                synchronized(OkHttpUtils::class) {
                    if (client == null) {
                        client = OkHttpClient.Builder()
                            .retryOnConnectionFailure(true)
                            .connectTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
                            .build()
                    }
                }
            }
            return client!!
        }
    }
}