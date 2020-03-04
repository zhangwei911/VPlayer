package viz.commonlib.download.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class TS(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "m3u8_id") var m3u8_id: Int = 0,
    @ColumnInfo(name = "status") var status: Int = 0,
    @ColumnInfo(name = "progress") var progress: Int = 0,
    @ColumnInfo(name = "index") var index: Int = 0,
    @ColumnInfo(name = "duration") var duration: Float = 0f,
    @ColumnInfo(name = "path") var path: String = ""
) : Parcelable