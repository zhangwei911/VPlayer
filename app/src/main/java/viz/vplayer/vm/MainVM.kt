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
import org.jsoup.Jsoup
import viz.vplayer.DEFAULT_UA
import viz.vplayer.UTF8
import viz.vplayer.bean.SearchBean
import viz.vplayer.bean.VideoInfoBean

class MainVM : ViewModel() {
    val search = MutableLiveData<MutableList<SearchBean>>()
    val episodes = MutableLiveData<MutableList<String>>()
    val play = MutableLiveData<VideoInfoBean>()
    val errorInfo = MutableLiveData<String>()

    fun searchVideos(kw: String, videoSearchUrl: String) {
        Task.callInBackground {
            val http = HttpUtils()
            http.configUserAgent(DEFAULT_UA)
            http.configResponseTextCharset(UTF8)
            http.configCurrentHttpCacheExpiry(1000 * 60.toLong()) //设置超时时间   60s
            val params = RequestParams("utf-8")
            params.addBodyParameter("wd", kw)
            http.send(
                HttpRequest.HttpMethod.POST,
                videoSearchUrl,
                params,
                object : RequestCallBack<String>() {
                    override fun onSuccess(responseInfo: ResponseInfo<String>?) {
                        val searchList = mutableListOf<SearchBean>()
                        val doc = Jsoup.parse(responseInfo!!.result)
                        val uls = doc.getElementsByClass("stui-vodlist__media")
                        val ul = uls[0]
                        val lis = ul.getElementsByTag("li")
                        lis.forEachIndexed { index, element ->
                            val aArr = element.getElementsByTag("a")
                            val name = aArr[0].attr("title")
                            val img = aArr[0].attr("data-original")
                            val uri = Uri.parse(videoSearchUrl)
                            searchList.add(
                                SearchBean(
                                    name,
                                    element.getElementsByClass("detail")[0].html(),
                                    img,
                                    uri.scheme + "://" + uri.host + aArr[0].attr("href")
                                )
                            )
                        }
                        search.postValue(searchList)
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

    fun getVideoEpisodesInfo(singleVideoPageUrl: String) {
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
                        val doc = Jsoup.parse(responseInfo!!.result)
                        val episodeList = mutableListOf<String>()
                        val div = doc.getElementsByClass("num-tab-main")
                        val lis = div[0].getElementsByTag("li")
                        val uri = Uri.parse(singleVideoPageUrl)
                        lis.forEachIndexed { index, element ->
                            val a = element.getElementsByTag("a")
                            val href = uri.scheme + "://" + uri.host + a.attr("href")
                            episodeList.add(href)
                        }
                        episodes.postValue(episodeList)
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

    fun getVideoInfo(singleVideoPageUrl: String) {
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
                        val doc = Jsoup.parse(responseInfo!!.result)
                        val title = doc.title()
                        val cms_player = doc.getElementById("cms_player")
                        val scripts = cms_player.getElementsByTag("script")
                        val script = scripts[0]
                        val scriptHtml = script.html()
                        val url = scriptHtml.substring(
                            "var cms_player = {\"url\":\"".length,
                            scriptHtml.indexOf("\",")
                        ).replace("\\/", "/")
                        val videoList = mutableListOf<Pair<String, String>>()
                        val listParent = doc.getElementsByClass("num-tab-main")
                        val list = listParent[0].getElementsByTag("li")
                        list.forEachIndexed { index, element ->
                            val href = element.attr("abs:href")
                            val name = element.html()
                            videoList.add(Pair(href, name))
                        }
                        play.postValue(VideoInfoBean(url, title, 0, videoList))
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
