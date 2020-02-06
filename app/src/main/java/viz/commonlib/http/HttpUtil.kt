package viz.commonlib.http

import com.google.gson.GsonBuilder
import com.viz.tools.l
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import viz.vplayer.util.App
import java.util.concurrent.TimeUnit


interface HttpUtil {
    companion object {
        inline fun <reified T> createHttp(
            url: String,
            debug: Boolean = false,
            interceptorList: MutableList<Interceptor> = mutableListOf<Interceptor>(),
            connectTimeout: Long = 60,
            readTimeout: Long = 60,
            writeTimeout: Long = 60,
            excludeUrls: MutableList<String> = mutableListOf()
        ): T {
            val gson = GsonBuilder()
                //配置你的Gson
                .setDateFormat("yyyy-MM-dd hh:mm:ss")
                .create()
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                l.d("API", it)
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC
            val builder = OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .addInterceptor(logger)
            interceptorList.forEach {
                builder.addInterceptor(it)
            }
            if (debug) {
                builder.addInterceptor(
                    ResultInterceptor(
                        excludeUrls
                    )
                )
            }
            builder.addInterceptor(NetworkInterceptor(App.instance.applicationContext))
            return Retrofit.Builder()
                .baseUrl(url)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build().create(T::class.java)
        }
    }
}