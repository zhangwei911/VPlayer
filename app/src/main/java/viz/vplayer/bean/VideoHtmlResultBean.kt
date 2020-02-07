package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoHtmlResultBean(
    var title: String,
    var mainCss: String,
    var isFrameProcess: Boolean,
    var isFrame: Boolean,
    var iframeIndex: Int,
    var iframeAttr: String,
    var mainIndex: Int,
    var itemCss: String,
    var itemIndex: Int,
    var isItemAttr: Boolean,
    var itemAttr: String,
    var isReg: Boolean,
    var regStr: String,
    var regIndex: Int,
    var hasEpisodes: Boolean,
    var episodesMainCss: String,
    var episodesMainIndex: Int,
    var episodesListCss: String,
    var urlCss: String,
    var urlIndex: Int,
    var isUrlAttr: Boolean,
    var urlAttr: String,
    var nameCss: String,
    var nameIndex: Int,
    var isNameAttr: Boolean,
    var nameAttr: String,
    var hasUrlPrefix: Boolean,
    var videoCss: String,
    var videoIndex: Int,
    var isVideoUrlAttr: Boolean,
    var videoUrlAttr: String
) : Parcelable
