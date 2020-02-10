package viz.vplayer.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EpisodesBean(
    var mainCss: String,
    var mainIndex: Int,
    var regStrList: MutableList<String>,
    var listCss: String,
    var isMultiList:Boolean,
    var listCssMulti: String,
    var listItemCss: String,
    var listItemIndex: Int,
    var isListItemAttr: Boolean,
    var listItemAttr: String,
    var hasUrlPrefix: Boolean,
    var isReg: Boolean,
    var regIndexList: MutableList<Int>,
    var regSplitList: MutableList<String>,
    var regItemStr: String,
    var regItemIndex: Int,
    var isRegNeedDecoder: Boolean
    ) : Parcelable
