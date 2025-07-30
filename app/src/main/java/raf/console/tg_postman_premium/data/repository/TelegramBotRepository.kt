package raf.console.tg_postman_premium.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import raf.console.tg_postman_premium.data.local.dao.TelegramBotDao
import raf.console.tg_postman_premium.data.mapper.toDomain
import raf.console.tg_postman_premium.data.mapper.toEntity
import raf.console.tg_postman_premium.domain.model.TelegramBot
import javax.inject.Inject

class TelegramBotRepository @Inject constructor(
    private val dao: TelegramBotDao
) {
    fun getAllBots(): Flow<List<TelegramBot>> {
        return dao.getAllBots().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getBotById(id: Long): TelegramBot? {
        return dao.getBotById(id)?.toDomain()
    }

    suspend fun insert(bot: TelegramBot): Long {
        return dao.insert(bot.toEntity())
    }

    suspend fun update(bot: TelegramBot) {
        dao.update(bot.toEntity())
    }

    suspend fun delete(bot: TelegramBot) {
        dao.delete(bot.toEntity())
    }

    suspend fun createEmptyBot(): Long {
        val emptyBot = TelegramBot(
            id = 0L,
            botName = "",
            token = "",
            selectedType = "channel",
            chatIds = emptyList(),
            sendMode = "ONCE",
            message = "",
            delayMs = 0L,
            intervalMs = 0L,
            durationSubMode = "TIMES_PER_SECONDS",
            durationTotalTime = 60,
            durationSendCount = 1,
            durationFixedInterval = 10
        )
        return dao.insert(emptyBot.toEntity())
    }

}
