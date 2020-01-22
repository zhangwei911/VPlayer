package viz.vplayer.bean

import android.graphics.Color
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MenuBean(
    var name: String,
    var resId: Int = -1,
    var menuColor:Int = Color.BLACK
) : Parcelable