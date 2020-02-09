package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EpisodeListBean(
    var index: Int = 0,
    var url: String = "",
    var isSelect: Boolean = false
) : Parcelable