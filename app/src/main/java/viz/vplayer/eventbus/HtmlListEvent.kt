package viz.vplayer.eventbus

import viz.vplayer.bean.JsonBean

data class HtmlListEvent(
    var htmlList:MutableList<JsonBean> = mutableListOf()
)