package viz.commonlib.util

import android.net.Uri

object UrlUtil {
    fun getUrl(urlConsult: String, urlTarget: String): String {
        val uri = Uri.parse(urlConsult)
        return urlTarget.let {
            if (it.startsWith("//")) {
                "http:$it"
            } else if (it.startsWith("/")) {
                "${uri.scheme}://${uri.authority}" + it
            } else if (it.startsWith("http://") || it.startsWith("https://")) {
                it
            } else {
                "${uri.scheme}://${uri.authority}${uri.path!!.substringBeforeLast("/")}/" + it
            }
        }
    }
}