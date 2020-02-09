package viz.vplayer.util

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RoomUtil {
    fun migration(preVersion: Int, nowVersion: Int,vararg sqlList:String): Migration {
        return object : Migration(preVersion, nowVersion) {
            override fun migrate(database: SupportSQLiteDatabase) {
                sqlList.forEach { sql->
                    database.execSQL(sql)
                }
            }
        }
    }

    fun migrationOneVersion(preVersion: Int,vararg sqlList:String): Migration {
        return object : Migration(preVersion, preVersion + 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                sqlList.forEach { sql->
                    database.execSQL(sql)
                }
            }
        }
    }
}