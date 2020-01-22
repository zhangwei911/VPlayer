package viz.vplayer.room

import androidx.room.*

@Dao
interface VideoInfoDao {
    @Query("select * from videoinfo")
    fun getAll(): MutableList<VideoInfo>

    @Query("select * from videoinfo where video_url = :videoUrl LIMIT 1")
    fun getByUrl(videoUrl: String): VideoInfo

    @Insert
    fun insertAll(vararg videoinfo: VideoInfo)

    @Update
    fun updateALl(vararg videoinfo: VideoInfo)

    @Delete
    fun delete(videoinfo: VideoInfo)
}