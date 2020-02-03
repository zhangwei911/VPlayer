package viz.vplayer.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VideoInfo::class, Episode::class, Rule::class, Download::class],
    version = 9
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun ruleDao(): RuleDao
    abstract fun downloadDao(): DownloadDao
}