package viz.vplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Rule(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "rule_url") var ruleUrl: String = "",
    @ColumnInfo(name = "rule_enable") var ruleEnable: Boolean = true,
    @ColumnInfo(name = "rule_status") var ruleStatus: Int = 1
){
    val ruleStatusMsg:String
        get() {
            return when(ruleStatus){
                1->{
                    "可用"
                }
                0->{
                    "无效"
                }
                -1->{
                    "为空"
                }
                else->{
                    ""
                }
            }
        }
}