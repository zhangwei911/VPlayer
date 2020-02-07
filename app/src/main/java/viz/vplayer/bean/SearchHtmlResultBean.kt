package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchHtmlResultBean(
    var mainCss: String,
    var mainListIndex: Int,
    var searchListCss: String,
    var nameCss: String,
    var nameIndex: Int,
    var isNameAttr: Boolean,
    var nameAttr: String,
    var descCss: String,
    var descIndex: Int,
    var isDescAttr: Boolean,
    var descAttr: String,
    var imgCss: String,
    var imgIndex: Int,
    var isImgAttr: Boolean,
    var imgAttr: String,
    var urlCss: String,
    var urlIndex: Int,
    var isUrlAttr: Boolean,
    var urlAttr: String,
    var hasUrlPrefix: Boolean,
    var isWebPlay: Boolean
) : Parcelable