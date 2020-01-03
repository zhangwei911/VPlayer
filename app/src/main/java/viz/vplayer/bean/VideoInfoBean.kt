package viz.vplayer.bean

data class VideoInfoBean(var url: String, var title: String, var duration: Long,var videoList:MutableList<Pair<String,String>> = mutableListOf())