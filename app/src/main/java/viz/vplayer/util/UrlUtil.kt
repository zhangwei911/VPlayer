package viz.vplayer.util

import android.content.Context
import android.net.Uri
import com.viz.tools.MD5Util
import com.viz.tools.l
import java.net.URLDecoder

object UrlUtil {
    fun format(url: String): String {
        var oldReplaceWords = arrayListOf("+", " ", "*")
        var newReplaceWords = arrayListOf("%2B", "%20", "%2A")
        var urlUTF8 = URLDecoder.decode(url, "UTF-8")
        for (i in oldReplaceWords.indices) {
            urlUTF8 = urlUTF8.replace(oldReplaceWords[i], newReplaceWords[i])
        }
        l.i(urlUTF8)
        return urlUTF8
    }

    fun generatLocalFileNameAndPath(
        context: Context,
        url: String,
        format: Boolean = false
    ): Pair<String, String> {
        var uri = Uri.parse(
            if (format) {
                format(url)
            } else {
                url
            }
        )
        val isM3u8 = url.endsWith(".m3u8")
        val fileName = MD5Util.MD5(url) + if (isM3u8) {
            ".m3u8"
        } else {
            ""
        }
        val target = FileUtil.getPath(context) + "/VDownloader/" + fileName + "/" + fileName + ".mp4"
        l.d(target)
        return Pair(fileName, target)
    }
}