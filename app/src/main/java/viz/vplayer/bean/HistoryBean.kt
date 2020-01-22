package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HistoryBean(
    var name: String,
    var leftTime: Long = -1,
    var img: String
) : Parcelable