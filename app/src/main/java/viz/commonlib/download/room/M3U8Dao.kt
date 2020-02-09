package viz.commonlib.download.room

import androidx.room.*

@Dao
interface M3U8Dao {
    @Query("select * from m3u8")
    fun getAll(): MutableList<M3U8>

    @Query("select * from m3u8 where status=:status")
    fun getAllByStatus(status: Int): MutableList<M3U8>

    @Query("select * from m3u8 where url=:url LIMIT 1")
    fun getByUrl(url: String): M3U8

    @Insert
    fun insertAll(vararg m3U8: M3U8)

    @Update
    fun updateALl(vararg m3U8: M3U8)

    @Delete
    fun delete(m3U8: M3U8)
}