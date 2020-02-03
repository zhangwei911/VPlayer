package viz.vplayer.room

import android.os.Parcelable
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import kotlinx.android.parcel.Parcelize

@Entity(
    foreignKeys = [ForeignKey(
        entity = VideoInfo::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("video_id"),
        onDelete = CASCADE
    )],
    indices = [Index(value = ["video_id"])]
)
@Parcelize
data class Episode(
    @PrimaryKey(autoGenerate = true)
    var id: Int=0,
    @ColumnInfo(name = "video_id")
    var videoId: Int=0,
    @ColumnInfo(name = "url")
    var url: String="",
    @ColumnInfo(name = "url_index")
    var urlIndex: Int=0
) : Parcelable