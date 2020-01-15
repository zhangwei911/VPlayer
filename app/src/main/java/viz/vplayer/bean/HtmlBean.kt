package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HtmlBean(var searchHtmlResultBean:SearchHtmlResultBean, var episodesBean: EpisodesBean,var videoHtmlResultBean:VideoHtmlResultBean) :
    Parcelable