package viz.commonlib.http

import okhttp3.Interceptor

interface BaseHttp {
    companion object {
        inline fun <reified T> createHttp(
            url: String = "",
            addCommonHeader: Boolean = true,
            addCommonPostJson: Boolean = true,
            debug: Boolean = true,
            jsonContentType: Boolean = true,
            contentType: String = "",
            commonDataMethod: CommonInterceptor.CommonRequestType = CommonInterceptor.CommonRequestType.POST_JSON,
            interceptorList: MutableList<Interceptor> = mutableListOf<Interceptor>(),
            connectTimeout: Long = 60,
            readTimeout: Long = 60,
            writeTimeout: Long = 60,
            excludeUrls: MutableList<String> = mutableListOf(),
            addGsonConverterFactory: Boolean = true
        ): T {
            return HttpUtil.createHttp<T>(url, debug,interceptorList,connectTimeout,readTimeout, writeTimeout, excludeUrls, addGsonConverterFactory)
        }
    }
}