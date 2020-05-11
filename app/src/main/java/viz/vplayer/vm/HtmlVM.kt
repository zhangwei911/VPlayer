package viz.vplayer.vm

import androidx.lifecycle.ViewModel
import com.viz.tools.l
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import viz.commonlib.http.VCallback
import viz.commonlib.util.UrlUtil
import viz.vplayer.http.HttpApi

class HtmlVM : ViewModel() {
    fun autoGet(url: String) {
        HttpApi.createHttp().anyUrl(url)
            .enqueue(VCallback<ResponseBody>(onResult = { call, response, resultResponseBody ->
                try {
                    val result = resultResponseBody.string()
                    val doc = Jsoup.parse(result)
                    val uls = doc.select("ul")
                    l.d(uls)
                    val allLis = mutableListOf<String>()
                    uls.forEach { ul ->
                        val lis = ul.select("li")
                        lis?.forEach { li ->
                            val a = li.select("a")
                            val href = if (a != null && a.size > 0) {
                                a[0].attr("href").let {
                                    UrlUtil.getUrl(url, it)
                                }
                            } else {
                                "[null]"
                            }
                            val img = li.select("img")
                            val imgUrl = if (img != null && img.size > 0) {
                                img[0].attr("src").let {
                                    UrlUtil.getUrl(url, it)
                                }
                            } else {
                                val imgUrlTemp = if (a.size > 0) {
                                    a[0].attr("data-original")
                                } else {
                                    ""
                                }
                                if (imgUrlTemp.isNullOrEmpty()) {
                                    "[null]"
                                } else {
                                    imgUrlTemp.let {
                                        UrlUtil.getUrl(url, it)
                                    }
                                }
                            }
                            if (imgUrl != "[null]") {
                                val text = li.text() ?: "[null]"
                                allLis.add("href=$href imgUrl=$imgUrl text=$text")
                            }
                        }
                    }
                    l.d(allLis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }))
    }
}