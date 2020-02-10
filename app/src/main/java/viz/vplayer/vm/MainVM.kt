package viz.vplayer.vm

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.espresso.idling.net.UriIdlingResource
import bolts.Task
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import org.jsoup.Jsoup
import viz.commonlib.http.VCallback
import viz.vplayer.bean.*
import viz.vplayer.http.HttpApi
import viz.vplayer.util.*
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException


class MainVM(private val state: SavedStateHandle) : ViewModel() {
    val search = MutableLiveData<MutableList<SearchBean>>()
    val episodes = MutableLiveData<MutableList<String>>()
    val play = MutableLiveData<VideoInfoBean>()
    val errorInfo = MutableLiveData<ErrorInfo>()
    val rules = MutableLiveData<Pair<String, String>>()
    val jsonBeanList = MutableLiveData<MutableList<JsonBean>>()
    val addCache = MutableLiveData<Int>()
    val webView = MutableLiveData<String>()
    private val KEY_SEARCH_INFO = "searchInfo"
    private val KEY_SEARCH_URL = "searchUrl"
    private val KEY_SEARCH_RESULT = "searchResult"

    fun getSearchInfo(): LiveData<String> {
        return state.getLiveData<String>(KEY_SEARCH_INFO)
    }

    fun saveSearchInfo(value: String) {
//        state.set(KEY_SEARCH_INFO, value)
    }

    fun getSearchUrl(): LiveData<String> {
        return state.getLiveData<String>(KEY_SEARCH_URL)
    }

    fun saveSearchUrl(searchUrl: String) {
//        state.set(KEY_SEARCH_URL, searchUrl)
    }

    fun getSearchResult(): LiveData<MutableList<SearchBean>> {
        return state.getLiveData<MutableList<SearchBean>>(KEY_SEARCH_RESULT)
    }

    fun saveSearchResult(searchBeanList: MutableList<SearchBean>) {
//        state.set(KEY_SEARCH_RESULT, searchBeanList)
    }

    fun getJson(rulesUrl: String) {
        Task.callInBackground {
            GlobalScope.launch {
                try {
                    HttpApi.createHttp().anyUrl(rulesUrl)
                        .enqueue(VCallback<ResponseBody>(onResult = { call, response, result ->
                            var rBody: String? = null
                            val responseBody = response.body()
                            val UTF8 = Charset.forName("UTF-8")
                            if (responseBody != null) {
                                val source = responseBody!!.source()
                                source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
                                val buffer = source.buffer()

                                var charset = UTF8
                                val contentType = responseBody.contentType()
                                if (contentType != null) {
                                    try {
                                        charset = contentType.charset(UTF8)
                                    } catch (e: UnsupportedCharsetException) {
                                        e.printStackTrace()
                                    }

                                }
                                rBody = buffer.clone().readString(charset)
                            } else {
                                errorInfo.postValue(
                                    ErrorInfo(
                                        "获取json规则数据为空",
                                        ErrorCode.ERR_JSON_EMPTY,
                                        rulesUrl
                                    )
                                )
                                return@VCallback
                            }
                            if (rBody.isJson()) {
                                rules.postValue(Pair(rulesUrl, rBody))
                            } else {
                                errorInfo.postValue(
                                    ErrorInfo(
                                        "解析rule规则数据异常",
                                        ErrorCode.ERR_JSON_INVALID,
                                        rulesUrl
                                    )
                                )
                            }
                        }, onError = { errorEntity, call, t, response ->
                            errorInfo.postValue(
                                ErrorInfo(
                                    "获取json规则(${errorEntity.error}-${errorEntity.message})",
                                    ErrorCode.ERR_JSON_INVALID,
                                    rulesUrl
                                )
                            )
                        }))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.continueWithEnd("抓取网页")
    }

    fun searchVideos(
        from: Int,
        paramsMap: MutableMap<String, String>,
        videoSearchUrl: String,
        searchHtmlResultBean: SearchHtmlResultBean,
        uriIdlingResource: UriIdlingResource? = null
    ) {
        Task.callInBackground {
            uriIdlingResource?.beginLoad(videoSearchUrl)
            GlobalScope.launch {
                try {
                    HttpApi.createHttp().anyUrl(videoSearchUrl, paramsMap)
                        .enqueue(VCallback<ResponseBody>(onResult = { call, response, resultResponseBody ->
                            try {
                                val result = resultResponseBody.string()
                                val searchList = mutableListOf<SearchBean>()
                                val doc = Jsoup.parse(result)
                                val uls = doc.select(searchHtmlResultBean.mainCss)
                                val ul = uls[searchHtmlResultBean.mainListIndex]
                                val lis = ul.select(searchHtmlResultBean.searchListCss)
                                lis.forEachIndexed { index, element ->
                                    val nameElements = element.select(searchHtmlResultBean.nameCss)
                                    val nameElement = nameElements[searchHtmlResultBean.nameIndex]
                                    val name = if (searchHtmlResultBean.isNameAttr) {
                                        nameElement.attr(searchHtmlResultBean.nameAttr)
                                    } else {
                                        nameElement.html()
                                    }
                                    val imgElements = element.select(searchHtmlResultBean.imgCss)
                                    val imgElement = imgElements[searchHtmlResultBean.imgIndex]
                                    val img = if (searchHtmlResultBean.isImgAttr) {
                                        imgElement.attr(searchHtmlResultBean.imgAttr)
                                    } else {
                                        imgElement.html()
                                    }
                                    val descElements = element.select(searchHtmlResultBean.descCss)
                                    val descElement = descElements[searchHtmlResultBean.descIndex]
                                    val desc = if (searchHtmlResultBean.isDescAttr) {
                                        descElement.attr(searchHtmlResultBean.descAttr)
                                    } else {
                                        descElement.html()
                                    }
                                    val urlElements = element.select(searchHtmlResultBean.urlCss)
                                    val urlElement = urlElements[searchHtmlResultBean.urlIndex]
                                    val url = if (searchHtmlResultBean.isUrlAttr) {
                                        urlElement.attr(searchHtmlResultBean.urlAttr)
                                    } else {
                                        urlElement.html()
                                    }
                                    val uri = Uri.parse(videoSearchUrl)
                                    searchList.add(
                                        SearchBean(
                                            name,
                                            desc,
                                            img,
                                            if (searchHtmlResultBean.hasUrlPrefix) {
                                                ""
                                            } else {
                                                uri.scheme + "://" + uri.host
                                            } + url,
                                            from
                                        )
                                    )
                                }
                                search.postValue(searchList)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorInfo.postValue(ErrorInfo("获取搜索数据异常(${e.localizedMessage})"))
                            }
                            uriIdlingResource?.endLoad(videoSearchUrl)
                        }, onError = { errorEntity, call, t, response ->
                            errorInfo.postValue(
                                ErrorInfo(
                                    "获取搜索数据异常(${errorEntity.error}-${errorEntity.message})"
                                )
                            )
                            uriIdlingResource?.endLoad(videoSearchUrl)
                        }))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.continueWithEnd("抓取网页")
    }

    fun getVideoEpisodesInfo(singleVideoPageUrl: String, episodesBean: EpisodesBean) {
        Task.callInBackground {
            GlobalScope.launch {
                try {
                    HttpApi.createHttp().anyUrl(singleVideoPageUrl)
                        .enqueue(VCallback<ResponseBody>(onResult = { call, response, resultResponseBody ->
                            try {
                                val result = resultResponseBody.string()
                                val doc = Jsoup.parse(result)
                                val episodeList = mutableListOf<String>()
                                val mainCss = episodesBean.mainCss
                                var divs = doc.select(mainCss)
                                val mainCssRegStr = ":[a-z]{2}\\(([0-9]{1,3})\\)"
                                val regMain = Regex(mainCssRegStr)
                                if (mainCss.contains(regMain) && divs.size == 0) {
                                    val resultList = regMain.find(mainCss)
                                    if (resultList != null) {
                                        val index = resultList.groupValues[1].toInt()
                                        for (i in index - 1 downTo 0) {
                                            val newListCss = mainCss.replace("($index)", "($i)")
                                            divs = doc.select(newListCss)
                                            if (divs.size > 0) {
                                                break
                                            }
                                        }
                                    }
                                }
                                val div = divs[episodesBean.mainIndex]
                                if (episodesBean.isReg) {
                                    val script = div.html()
                                    var successRegCount = 0
                                    episodesBean.regStrList.forEachIndexed { index, regStr ->
                                        val reg = Regex(regStr)
                                        val result = reg.find(script)
                                        if (result != null) {
                                            val urlList =
                                                result.groupValues[episodesBean.regIndexList[index]].split(
                                                    episodesBean.regSplitList[index]
                                                )
                                            val regList = Regex(episodesBean.regItemStr)
                                            for (urlStr in urlList) {
                                                val resultList = regList.find(urlStr)
                                                if (resultList != null) {
                                                    episodeList.add(
                                                        if (episodesBean.isRegNeedDecoder) {
                                                            URLDecoder.decode(resultList.groupValues[episodesBean.regItemIndex])
                                                        } else {
                                                            resultList.groupValues[episodesBean.regItemIndex]
                                                        }
                                                    )
                                                    successRegCount++
                                                } else {
                                                    if (successRegCount == 0 && index == episodesBean.regStrList.size - 1) {
                                                        errorInfo.postValue(ErrorInfo("获取剧集url数据异常(正则表达式可能错了)"))
                                                    }
                                                    break
                                                }
                                            }
                                        } else {
                                            errorInfo.postValue(ErrorInfo("获取剧集数据异常(正则表达式可能错了)"))
                                        }
                                    }
                                } else {
                                    val listCss = episodesBean.listCss
                                    var lis = div.select(listCss)
                                    val listCssRegStr = ":[a-z]{2}\\(([0-9]{1,3})\\)"
                                    val regList = Regex(listCssRegStr)
                                    if (listCss.contains(regList) && lis.size == 0) {
                                        val resultList = regList.find(listCss)
                                        if (resultList != null) {
                                            val index = resultList.groupValues[1].toInt()
                                            for (i in index - 1 downTo 0) {
                                                val newListCss = listCss.replace("($index)", "($i)")
                                                lis = div.select(newListCss)
                                                if (lis.size > 0) {
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    val uri = Uri.parse(singleVideoPageUrl)
                                    lis.forEachIndexed { index, element ->
                                        val aArr = element.select(episodesBean.listItemCss)
                                        val a = aArr[episodesBean.listItemIndex]
                                        val href = if (episodesBean.hasUrlPrefix) {
                                            ""
                                        } else {
                                            uri.scheme + "://" + uri.host
                                        } + if (episodesBean.isListItemAttr) {
                                            a.attr(episodesBean.listItemAttr)
                                        } else {
                                            a.html()
                                        }
                                        episodeList.add(href)
                                    }
                                }
                                episodes.postValue(episodeList)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorInfo.postValue(ErrorInfo("获取搜索数据异常(${e.localizedMessage})"))
                            }
                        }, onError = { errorEntity, call, t, response ->
                            errorInfo.postValue(
                                ErrorInfo(
                                    "获取搜索数据异常(${errorEntity.error}-${errorEntity.message})"
                                )
                            )
                        }))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.continueWithEnd("抓取网页")
    }

    fun getVideoInfo(
        singleVideoPageUrl: String,
        videoHtmlResultBean: VideoHtmlResultBean,
        img: String,
        isDownload: Boolean = false,
        isLastDownload: Boolean = false
    ) {
        Task.callInBackground {
            GlobalScope.launch {
                try {
                    HttpApi.createHttp().anyUrl(singleVideoPageUrl)
                        .enqueue(VCallback<ResponseBody>(onResult = { call, response, resultResponseBody ->
                            try {
                                val result = resultResponseBody.string()
                                val doc = Jsoup.parse(result)
                                val title = doc.title()
                                if (videoHtmlResultBean.isFrame && !videoHtmlResultBean.isFrameProcess) {
                                    val iframes = doc.select("iframe")
                                    val iframe = iframes[videoHtmlResultBean.iframeIndex]
                                    val src = iframe.attr(videoHtmlResultBean.iframeAttr)
                                    if(videoHtmlResultBean.isFromWebView) {
                                        webView.postValue(src)
                                    }else{
                                        videoHtmlResultBean.isFrameProcess = true
                                        videoHtmlResultBean.title = title
                                        getVideoInfo(src!!, videoHtmlResultBean, img)
                                    }
                                    return@VCallback
                                }
                                val divs = doc.select(videoHtmlResultBean.mainCss)
                                val div = divs[videoHtmlResultBean.mainIndex]
                                val scripts = div.select(videoHtmlResultBean.itemCss)
                                val script = scripts[videoHtmlResultBean.itemIndex]
                                val videoList = mutableListOf<Pair<String, String>>()
                                if (videoHtmlResultBean.hasEpisodes) {
                                    val listParent =
                                        doc.select(videoHtmlResultBean.episodesMainCss)
                                    val list =
                                        listParent[videoHtmlResultBean.episodesMainIndex].getElementsByTag(
                                            videoHtmlResultBean.episodesListCss
                                        )
                                    val uri = Uri.parse(singleVideoPageUrl)
                                    list.forEachIndexed { index, element ->
                                        val urlArr = element.select(videoHtmlResultBean.urlCss)
                                        val url = urlArr[videoHtmlResultBean.urlIndex]
                                        val href = if (videoHtmlResultBean.isUrlAttr) {
                                            url.attr(videoHtmlResultBean.urlAttr)
                                        } else {
                                            url.html()
                                        }
                                        val nameArr = element.select(videoHtmlResultBean.nameCss)
                                        val nameElement = nameArr[videoHtmlResultBean.nameIndex]
                                        val name = if (videoHtmlResultBean.isNameAttr) {
                                            nameElement.attr(videoHtmlResultBean.nameAttr)
                                        } else {
                                            nameElement.html()
                                        }
                                        videoList.add(
                                            Pair(
                                                if (videoHtmlResultBean.hasUrlPrefix) {
                                                    ""
                                                } else {
                                                    uri.scheme + "://" + uri.host
                                                } + href, name
                                            )
                                        )
                                    }
                                }
                                if (videoHtmlResultBean.isReg) {
                                    val scriptHtml = script.html()
                                    val reg = Regex(videoHtmlResultBean.regStr)
                                    val regResult = reg.find(scriptHtml)
                                    if (regResult != null) {
                                        val regResultItem =
                                            regResult.groupValues[videoHtmlResultBean.regIndex]
                                        val jsonObject = JSONObject("{'urlVideo':'$regResultItem'}")
                                        val urlVideo = jsonObject.get("urlVideo")
                                        videoHtmlResultBean.isFrameProcess = false
                                        play.postValue(
                                            VideoInfoBean(
                                                urlVideo.toString(),
                                                if (title.isNullOrEmpty()) {
                                                    videoHtmlResultBean.title
                                                } else {
                                                    title
                                                },
                                                0,
                                                img,
                                                videoList,
                                                isDownload,
                                                isLastDownload
                                            )
                                        )
                                    } else {
                                        videoHtmlResultBean.isFrameProcess = false
                                        errorInfo.postValue(ErrorInfo("获取视频数据异常(正则表达式可能错了)"))
                                    }
                                } else {
                                    val videoElements = doc.select(videoHtmlResultBean.videoCss)
                                    val videoElement = videoElements[videoHtmlResultBean.videoIndex]
                                    val videoUrl = if (videoHtmlResultBean.isVideoUrlAttr) {
                                        videoElement.attr(videoHtmlResultBean.videoUrlAttr)
                                    } else {
                                        videoElement.html()
                                    }
                                    play.postValue(
                                        VideoInfoBean(
                                            videoUrl,
                                            title,
                                            0,
                                            img,
                                            videoList,
                                            isDownload,
                                            isLastDownload
                                        )
                                    )
                                }
//                        if (videoHtmlResultBean.isFrame && videoHtmlResultBean.isFrameProcess) {
//                            val url = script.attr(videoHtmlResultBean.itemAttr)
//                            play.postValue(VideoInfoBean(url, title, 0, videoList))
//                        }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                videoHtmlResultBean.isFrameProcess = false
                                errorInfo.postValue(ErrorInfo("获取视频数据异常(${e.message})"))
                            }

                        }, onError = { errorEntity, call, t, response ->
                            errorInfo.postValue(
                                ErrorInfo(
                                    "获取视频(${errorEntity.error}-${errorEntity.message})"
                                )
                            )
                        }))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.continueWithEnd("抓取网页")
    }
}
