package viz.vplayer.room

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class VideoInfo(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "video_url") var videoUrl: String = "",
    @ColumnInfo(name = "video_title") @NonNull var videoTitle: String = "",
    @ColumnInfo(name = "current_position") var currentPosition: Int = 0,
    @ColumnInfo(name = "duration") var duration: Int = 0,
    @ColumnInfo(name = "video_img_url") var videoImgUrl: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = createdAt,
    @Ignore var episodeList: MutableList<Episode> = mutableListOf(),
    @Ignore var index: Int = 0,
    @ColumnInfo(name = "search_url") var searchUrl: String = ""
)