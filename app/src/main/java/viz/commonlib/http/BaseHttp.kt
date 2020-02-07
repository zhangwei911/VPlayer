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
            if (addCommonHeader) {
                val mapUtilHeader = MapUtil<String, CommonInfo>()
                    .add("User-Agent", CommonInfo("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36"))
                if (jsonContentType) {
                    mapUtilHeader.add("Content-Type", CommonInfo("application/json"))
                } else {
                    if (contentType.isNotEmpty()) {
                        mapUtilHeader.add("Content-Type", CommonInfo(contentType))
                    }
                }
                interceptorList.add(
                    CommonInterceptor(
                        mapUtilHeader.map
                    )
                )
            }
            return HttpUtil.createHttp<T>(url, debug,interceptorList,connectTimeout,readTimeout, writeTimeout, excludeUrls, addGsonConverterFactory)
        }
    }
}