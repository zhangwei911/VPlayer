package viz.vplayer.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(VideoInfo::class, Episode::class, Rule::class, Download::class),
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun ruleDao(): RuleDao
    abstract fun downloadDao(): DownloadDao
}