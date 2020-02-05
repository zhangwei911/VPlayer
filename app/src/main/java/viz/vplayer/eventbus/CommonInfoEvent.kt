package viz.vplayer.eventbus

import android.graphics.Color
import android.view.View

data class CommonInfoEvent(
    var show: Boolean = false,
    var text: String = "",
    var textColor: Int = Color.RED,
    var backgroundColor: Int = Color.LTGRAY,
    var type:InfoType = InfoType.NETWORK
)