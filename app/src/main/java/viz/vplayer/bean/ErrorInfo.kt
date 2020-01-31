package viz.vplayer.bean

data class ErrorInfo(
    var errMsg: String = "",
    var errCode: Int = -1,
    var url:String = ""
)