package raf.console.tg_postman_premium.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import raf.console.tg_postman_premium.data.local.entity.TelegramBotEntity

@Dao
interface TelegramBotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bot: TelegramBotEntity): Long

    @Update
    suspend fun update(bot: TelegramBotEntity)

    @Delete
    suspend fun delete(bot: TelegramBotEntity)

    @Query("SELECT * FROM telegram_bots ORDER BY id DESC")
    fun getAllBots(): Flow<List<TelegramBotEntity>>

    @Query("SELECT * FROM telegram_bots WHERE id = :id LIMIT 1")
    suspend fun getBotById(id: Long): TelegramBotEntity?
}
