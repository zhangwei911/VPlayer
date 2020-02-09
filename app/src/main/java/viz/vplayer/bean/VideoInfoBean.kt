package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoInfoBean(
    var url: String,
    var title: String,
    var duration: Long,
    var img: String = "",
    var videoList: MutableList<Pair<String, String>> = mutableListOf(),
    var isDownload: Boolean = false,
    var isLastDownload:Boolean = false
) : Parcelable