package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EpisodesBean(
    var mainCss: String,
    var mainIndex: Int,
    var regStr: String,
    var listCss: String,
    var listItemCss: String,
    var listItemIndex: Int,
    var isListItemAttr: Boolean,
    var listItemAttr: String,
    var hasUrlPrefix: Boolean,
    var isReg: Boolean,
    var regIndex: Int,
    var regSplit: String,
    var regItemStr: String,
    var regItemIndex: Int,
    var isRegNeedDecoder: Boolean
    ) : Parcelable
