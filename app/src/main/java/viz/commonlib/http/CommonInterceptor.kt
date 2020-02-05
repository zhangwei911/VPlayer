package viz.commonlib.http

import okhttp3.*
import okio.Buffer
import java.nio.charset.Charset


class CommonInterceptor(
    private val commonMap: Map<String, CommonInfo>,
    private val type: CommonRequestType = CommonRequestType.HEADER
) :
    Interceptor {
    enum class CommonRequestType {
        POST,
        POST_JSON,
        GET,
        HEADER
    }

    private val UTF8 = Charset.forName("UTF-8")
    private val commonJsonTag = "commonJson"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalHttpUrl = original.url
        val builderGet = originalHttpUrl.newBuilder()
        val url = originalHttpUrl.toUrl()
        val builderPost = FormBody.Builder()
        val jsonUtil = JsonUtil()
        val requestBody = original.body
        val builderHeader = chain.request().newBuilder()
        builderHeader.method(original.method, requestBody)
        var request = original
        if (requestBody is FormBody) {
            for (i in 0 until requestBody.size) {
                builderPost.add(requestBody.encodedName(i), requestBody.encodedValue(i))
            }
        } else if (requestBody is MultipartBody) {
            //..........非本文主题
        } else {
            val buffer = Buffer()
            if (requestBody != null) {
                requestBody.writeTo(buffer)

                var charset = UTF8
                val contentType = requestBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(UTF8)
                    if (charset != null) {
                        //读取原请求参数内容
                        val requestParams = buffer.readString(charset)
                        try {
                            jsonUtil.parseAndAddJson(commonJsonTag, requestParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        commonMap.forEach { map ->
            val commonInfo = map.value
            val includeUrls = commonInfo.includeUrls
            val excludeUrls = commonInfo.excludeUrls
            val includeUrlsSize = includeUrls.size
            val excludeUrlsSize = excludeUrls.size
            when {
                includeUrlsSize > 0 -> includeUrls.forEach { includeUrl ->
                    if (url.path == includeUrl) {
                        add(
                            builderHeader,
                            map,
                            commonInfo,
                            builderPost,
                            jsonUtil,
                            builderGet
                        )
                    }
                }
                excludeUrlsSize > 0 -> excludeUrls.forEach { excludeUrl ->
                    if (url.path != excludeUrl) {
                        add(
                            builderHeader,
                            map,
                            commonInfo,
                            builderPost,
                            jsonUtil,
                            builderGet
                        )
                    }
                }
                else -> add(
                    builderHeader,
                    map,
                    commonInfo,
                    builderPost,
                    jsonUtil,
                    builderGet
                )
            }
        }
        request = when (type) {
            CommonRequestType.HEADER -> {
                builderHeader.build()
            }
            CommonRequestType.POST -> {
                original.newBuilder().url(originalHttpUrl.toString()).post(builderPost.build())
                    .build()
            }
            CommonRequestType.POST_JSON -> {
                original.newBuilder().url(originalHttpUrl.toString())
                    .method(original.method, jsonUtil.getBody(commonJsonTag))
                    .build()
            }
            CommonRequestType.GET -> {
                val httpUrl = builderGet.build()
                val rBuilder = original.newBuilder()
                    .url(httpUrl)
                rBuilder.build()
            }
        }
        return chain.proceed(request)
    }

    private fun add(
        builderHeader: Request.Builder,
        map: Map.Entry<String, CommonInfo>,
        commonInfo: CommonInfo,
        builderPost: FormBody.Builder,
        jsonUtil: JsonUtil,
        builderGet: HttpUrl.Builder
    ) {
        when (type) {
            CommonRequestType.HEADER -> {
                builderHeader.addHeader(map.key, commonInfo.value)
            }
            CommonRequestType.POST -> {
                builderPost.add(map.key, commonInfo.value)
            }
            CommonRequestType.POST_JSON -> {
                jsonUtil.addJson(commonJsonTag, map.key, commonInfo.value)
            }
            CommonRequestType.GET -> {
                builderGet.addQueryParameter(map.key, commonInfo.value)
            }
        }
    }
}