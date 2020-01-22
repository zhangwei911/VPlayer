package viz.vplayer.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HtmlBean(
    @SerializedName("search")
    var searchHtmlResultBean: SearchHtmlResultBean,
    @SerializedName("episodes")
    var episodesBean: EpisodesBean,
    @SerializedName("video")
    var videoHtmlResultBean: VideoHtmlResultBean
) : Parcelable