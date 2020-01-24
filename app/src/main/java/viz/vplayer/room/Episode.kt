package viz.vplayer.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(
    foreignKeys = [ForeignKey(
        entity = VideoInfo::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("video_id"),
        onDelete = CASCADE
    )]
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