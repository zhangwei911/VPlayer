package viz.vplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Rule(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "rule_url") var ruleUrl: String = "",
    @ColumnInfo(name = "rule_enable") var ruleEnable: Boolean = true
)