package viz.vplayer.room

import androidx.room.Database
import androidx.room.RoomDatabase
import viz.commonlib.download.room.M3U8
import viz.commonlib.download.room.M3U8Dao
import viz.commonlib.download.room.TS
import viz.commonlib.download.room.TSDao

@Database(
    entities = [VideoInfo::class, Episode::class, Rule::class, Download::class, M3U8::class, TS::class],
    version = 14
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun ruleDao(): RuleDao
    abstract fun downloadDao(): DownloadDao
    abstract fun m3u8Dao(): M3U8Dao
    abstract fun tsDao(): TSDao
}