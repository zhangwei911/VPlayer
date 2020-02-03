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
    @ColumnInfo(name = "download_status") var status: Int = 0
)