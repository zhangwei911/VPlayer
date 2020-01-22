package viz.vplayer.room

import androidx.room.*

@Dao
interface RuleDao {
    @Query("select * from rule")
    fun getAll(): MutableList<Rule>

    @Query("select * from rule where rule_url = :ruleUrl LIMIT 1")
    fun getByUrl(ruleUrl: String): Rule

    @Insert
    fun insertAll(vararg rule: Rule)

    @Update
    fun updateALl(vararg rule: Rule)

    @Delete
    fun delete(rule: Rule)

    @Delete(entity = Rule::class)
    fun deleteByUrl(ruleUrl: RuleUrl)
}