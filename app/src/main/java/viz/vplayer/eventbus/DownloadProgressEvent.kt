package viz.vplayer.eventbus

data class DownloadProgressEvent(
    var progress:Int = 0,
    var videoUrl:String = ""
)