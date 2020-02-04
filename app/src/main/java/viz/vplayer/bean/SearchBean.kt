package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchBean(
    var name: String,
    var desc: String,
    var img: String,
    var url: String,
    var from: Int
) : Parcelable