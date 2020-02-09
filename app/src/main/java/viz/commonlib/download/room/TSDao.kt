package viz.commonlib.download.room

import androidx.room.*

@Dao
interface TSDao {
    @Query("select * from ts")
    fun getAll(): MutableList<TS>

    @Query("select * from ts where status=:status and m3u8_id=:m3u8_id order by `index`")
    fun getAllByStatusAndM3U8Id(status: Int, m3u8_id: Int): MutableList<TS>

    @Query("select * from ts where m3u8_id=:m3u8_id order by `index`")
    fun getAllByM3U8Id(m3u8_id: Int): MutableList<TS>

    @Query("select * from ts where url=:url limit 1")
    fun getByUrl(url: String): TS

    @Insert
    fun insertAll(vararg ts: TS)

    @Update
    fun updateAll(vararg ts: TS)

    @Delete
    fun delete(TS: TS)
}