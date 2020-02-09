package viz.commonlib.download.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class M3U8(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "status") var status: Int = 0,
    @ColumnInfo(name = "progress") var progress: Int = 0,
    @ColumnInfo(name = "path") var path: String = ""
) : Parcelable