package viz.vplayer.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView

var IMAGE_URL_PREFIX = ""
fun GlideRequest<Drawable>.into(imageView: ImageView, des: String, checkSame: Boolean = false) {
    if (checkSame && des == imageView.contentDescription && !des.isNullOrEmpty() && !imageView.contentDescription.isNullOrEmpty()) {
        return
    }
    imageView.contentDescription = des
    into(imageView)
}

fun GlideRequests.loadWithUrlPrefix(url: String): GlideRequest<Drawable> {
    return loadWithUrlPrefixAuto(url)
}

fun GlideRequests.loadWithUrlPrefixAuto(url: String): GlideRequest<Drawable> {
    return if (url.startsWith(IMAGE_URL_PREFIX) || url.startsWith("http://") || url.startsWith("https://")) {
        load(url)
    } else {
        load(IMAGE_URL_PREFIX + url)
    }
}