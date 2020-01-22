package viz.vplayer.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ParamBean(
    @SerializedName("key")
    var key: String,
    @SerializedName("value")
    var value: String
) : Parcelable