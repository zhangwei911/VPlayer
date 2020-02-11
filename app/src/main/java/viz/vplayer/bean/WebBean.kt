package viz.vplayer.bean

import android.graphics.Color
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.nio.ByteOrder

@Parcelize
data class WebBean(
    var name: String = "",
    var resId: Int = -1,
    var menuColor: Int = Color.BLACK,
    var imgUrl: String = "",
    var searchUrl: String = "",
    var isSelected: Boolean = false
) : Parcelable