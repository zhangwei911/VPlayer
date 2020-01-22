package viz.vplayer.room

import androidx.room.*

@Dao
interface EpisodeDao {
    @Query("select * from episode")
    fun getAll(): List<Episode>

    @Query("select * from episode where video_id = :videoId")
    fun getByVid(videoId: Int): MutableList<Episode>

    @Insert
    fun insertAll(vararg episode: Episode)

    @Insert
    fun insertAll(episodeList: MutableList<Episode>)

    @Update
    fun updateALl(vararg episode: Episode)

    @Delete
    fun delete(episode: Episode)

    @Delete(entity = Episode::class)
    fun deleteByVid(vararg videoId: VideoId)
}