package viz.vplayer.room

import androidx.room.*

@Dao
interface DownloadDao {
    @Query("select * from download")
    fun getAll(): MutableList<Download>

    @Query("select * from download where download_status=:status")
    fun getAllByStatus(status: Int): MutableList<Download>

    @Query("select * from download where video_url = :videoUrl LIMIT 1")
    fun getByUrl(videoUrl: String): Download

    @Insert
    fun insertAll(vararg download: Download)

    @Update
    fun updateALl(vararg download: Download)

    @Delete
    fun delete(download: Download)

    @Delete(entity = Download::class)
    fun deleteByNotificationId(notificationId: NotificationId)
}