package raf.console.tg_postman_premium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_bots")
data class TelegramBotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val botName: String,
    val token: String,
    val selectedType: String,
    val chatIds: List<String>,  // ✅ теперь список (TypeConverter)
    val sendMode: String,
    val message: String,
    val delayMs: Long,
    val intervalMs: Long,
    val durationSubMode: String,
    val durationTotalTime: Int,
    val durationSendCount: Int,
    val durationFixedInterval: Int
)
