package viz.vplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Download(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "video_url") var videoUrl: String = "",
    @ColumnInfo(name = "notification_id") var notificationId: Int = 0,
    @ColumnInfo(name = "video_title") var videoTitle: String = "",
    @ColumnInfo(name = "video_img_url") var videoImgUrl: String = "",
    @ColumnInfo(name = "download_status") var status: Int = 0,
    @ColumnInfo(name = "download_progress")  var progress:Int = 0,
    @ColumnInfo(name = "duration")  var duration:Long = 0,
    @ColumnInfo(name = "search_url")  var searchUrl:String = ""
)