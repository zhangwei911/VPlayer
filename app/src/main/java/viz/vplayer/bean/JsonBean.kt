package viz.vplayer.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class JsonBean(
    @SerializedName("search_url")
    var searchUrl: String,
    @SerializedName("web_name")
    var webName: String,
    @SerializedName("kw")
    var kwKey: String,
    @SerializedName("params")
    var params: MutableList<ParamBean>,
    @SerializedName("html")
    var html: HtmlBean
) : Parcelable