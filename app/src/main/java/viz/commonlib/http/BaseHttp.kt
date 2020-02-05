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
            excludeUrls: MutableList<String> = mutableListOf()
        ): T {
            return HttpUtil.createHttp<T>(url, debug)
        }
    }
}