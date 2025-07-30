package raf.console.tg_postman_premium.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import raf.console.tg_postman_premium.data.local.converter.Converters
import raf.console.tg_postman_premium.data.local.dao.TelegramBotDao
import raf.console.tg_postman_premium.data.local.entity.TelegramBotEntity

@Database(
    entities = [TelegramBotEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telegramBotDao(): TelegramBotDao
}
