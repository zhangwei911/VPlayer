package viz.vplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = VideoInfo::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("video_id"),
        onDelete = CASCADE
    )]
)
data class Episode(
    @PrimaryKey(autoGenerate = true)
    var id: Int=0,
    @ColumnInfo(name = "video_id")
    var videoId: Int=0,
    @ColumnInfo(name = "url")
    var url: String="",
    @ColumnInfo(name = "url_index")
    var urlIndex: Int=0
)