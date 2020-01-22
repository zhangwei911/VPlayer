package viz.vplayer.vm

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import bolts.Task
import com.lidroid.xutils.HttpUtils
import com.lidroid.xutils.exception.HttpException
import com.lidroid.xutils.http.RequestParams
import com.lidroid.xutils.http.ResponseInfo
import com.lidroid.xutils.http.callback.RequestCallBack
import com.lidroid.xutils.http.client.HttpRequest
import com.viz.tools.l
import org.json.JSONObject
import org.jsoup.Jsoup
import viz.vplayer.util.DEFAULT_UA
import viz.vplayer.util.UTF8
import viz.vplayer.bean.*
import java.net.URLDecoder


class MainVM : ViewModel() {
    val search = MutableLiveData<MutableList<SearchBean>>()
    val episodes = MutableLiveData<MutableList<String>>()
    val play = MutableLiveData<VideoInfoBean>()
    val errorInfo = MutableLiveData<String>()
    val rules = MutableLiveData<String>()
    val jsonBeanList = MutableLiveData<MutableList<JsonBean>>()

    fun getJson(rulesUrl: String) {
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            http.send(
                HttpRequest.HttpMethod.GET,
                rulesUrl,
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        if (responseInfo != null) {
                            if (responseInfo.result.isNullOrEmpty()) {
                                errorInfo.postValue("获取json规则数据为空")
                                return
                            }
                            rules.postValue(responseInfo.result)
                        } else {
                            errorInfo.postValue("获取json规则数据异常")
                        }
                    }

                    override fun onFailure(error: HttpException?, msg: String?) {
                        error?.printStackTrace()
                        l.e(msg)
                        errorInfo.postValue("获取json规则数据异常($msg)")
                    }

                })
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("抓取网页任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("抓取网页文件任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("抓取网页文件任务成功")
                }
            }
            return@continueWith null
        }
    }

    fun searchVideos(
        from: Int,
        paramsMap: MutableMap<String, String>,
        videoSearchUrl: String,
        searchHtmlResultBean: SearchHtmlResultBean
    ) {
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            val params = RequestParams("utf-8")
            paramsMap.forEach {
                params.addBodyParameter(it.key, it.value)
            }
            http.send(
                HttpRequest.HttpMethod.POST,
                videoSearchUrl,
                params,
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        try {
                            val searchList = mutableListOf<SearchBean>()
                            val doc = Jsoup.parse(responseInfo!!.result)
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
                            errorInfo.postValue("获取搜索数据异常(${e.localizedMessage})")
                        }
                    }

                    override fun onFailure(error: HttpException?, msg: String?) {
                        error?.printStackTrace()
                        l.e(msg)
                        errorInfo.postValue("获取搜索数据异常($msg)")
                    }

                })
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("抓取网页任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("抓取网页文件任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("抓取网页文件任务成功")
                }
            }
            return@continueWith null
        }
    }

    fun getVideoEpisodesInfo(singleVideoPageUrl: String, episodesBean: EpisodesBean) {
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            http.send(
                HttpRequest.HttpMethod.GET,
                singleVideoPageUrl,
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        try {
                            val doc = Jsoup.parse(responseInfo!!.result)
                            val episodeList = mutableListOf<String>()
                            val divs = doc.select(episodesBean.mainCss)
                            val div = divs[episodesBean.mainIndex]
                            if (episodesBean.isReg) {
                                val script = div.html()
                                val reg = Regex(episodesBean.regStr)
                                val result = reg.find(script)
                                if (result != null) {
                                    val urlList =
                                        result.groupValues[episodesBean.regIndex].split(episodesBean.regSplit)
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
                                        } else {
                                            errorInfo.postValue("获取剧集url数据异常(正则表达式可能错了)")
                                            break
                                        }
                                    }
                                } else {
                                    errorInfo.postValue("获取剧集数据异常(正则表达式可能错了)")
                                }
                            } else {
                                val lis = div.select(episodesBean.listCss)
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
                            errorInfo.postValue("获取搜索数据异常(${e.localizedMessage})")
                        }
                    }

                    override fun onFailure(error: HttpException?, msg: String?) {
                        error?.printStackTrace()
                        l.e(msg)
                        errorInfo.postValue("获取剧集数据异常($msg)")
                    }

                })
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("抓取网页任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("抓取网页文件任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("抓取网页文件任务成功")
                }
            }
            return@continueWith null
        }
    }

    fun getVideoInfo(
        singleVideoPageUrl: String,
        videoHtmlResultBean: VideoHtmlResultBean,
        img: String
    ) {
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            http.send(
                HttpRequest.HttpMethod.GET,
                singleVideoPageUrl,
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        try {
                            val doc = Jsoup.parse(responseInfo!!.result)
                            val title = doc.title()
                            if (videoHtmlResultBean.isFrame && !videoHtmlResultBean.isFrameProcess) {
                                val iframes = doc.select("iframe")
                                val iframe = iframes[videoHtmlResultBean.iframeIndex]
                                val src = iframe.attr(videoHtmlResultBean.iframeAttr)
                                videoHtmlResultBean.isFrameProcess = true
                                videoHtmlResultBean.title = title
                                getVideoInfo(src!!, videoHtmlResultBean, img)
                                return
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
                                            videoList
                                        )
                                    )
                                } else {
                                    errorInfo.postValue("获取视频数据异常(正则表达式可能错了)")
                                }
                            } else {
//                            val url = ""
//                            play.postValue(VideoInfoBean(url, title, 0, videoList))
                            }
//                        if (videoHtmlResultBean.isFrame && videoHtmlResultBean.isFrameProcess) {
//                            val url = script.attr(videoHtmlResultBean.itemAttr)
//                            play.postValue(VideoInfoBean(url, title, 0, videoList))
//                        }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            videoHtmlResultBean.isFrameProcess = false
                            errorInfo.postValue("获取视频数据异常(${e.message})")
                        }
                    }

                    override fun onFailure(error: HttpException?, msg: String?) {
                        error?.printStackTrace()
                        l.e(msg)
                        errorInfo.postValue("获取视频数据异常($msg)")
                    }

                })
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("抓取网页任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("抓取网页文件任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("抓取网页文件任务成功")
                }
            }
            return@continueWith null
        }
    }

    fun freeVip(videoUrl: String) {
//        http://vip.jlsprh.com/index.php?url=https://v.qq.com/x/cover/rjae621myqca41h/e003358h201.html
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            http.send(
                HttpRequest.HttpMethod.GET,
                "http://vip.jlsprh.com/index.php?url=$videoUrl",
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        val doc = Jsoup.parse(responseInfo!!.result)
                        val title = doc.title()
                    }

                    override fun onFailure(error: HttpException?, msg: String?) {
                        error?.printStackTrace()
                        l.e(msg)
                        errorInfo.postValue("获取视频数据异常($msg)")
                    }

                })
        }.continueWith { task ->
            when {
                task.isCancelled -> {
                    l.i("抓取网页任务取消")
                }
                task.isFaulted -> {
                    val error = task.error
                    l.e("抓取网页文件任务失败 $error")
                    error.printStackTrace()
                }
                else -> {
                    l.i("抓取网页文件任务成功")
                }
            }
            return@continueWith null
        }
    }
}
