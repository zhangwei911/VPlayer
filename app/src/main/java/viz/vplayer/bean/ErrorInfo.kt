package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ErrorInfo(
    var errMsg: String = "",
    var errCode: Int = -1,
    var url:String = ""
) : Parcelable