package raf.console.tg_postman_premium.ui.bots.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import raf.console.tg_postman_premium.data.local.dao.TelegramBotDao
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.domain.usecase.TelegramBotUseCases
import javax.inject.Inject

@HiltViewModel
class BotDetailsViewModel @Inject constructor(
    private val useCases: TelegramBotUseCases
) : ViewModel() {

    private val _bot = MutableStateFlow<TelegramBot?>(null)
    val bot: StateFlow<TelegramBot?> = _bot

    fun loadBot(id: Long) {
        viewModelScope.launch {
            _bot.value = useCases.getBotById(id)
        }
    }

    fun insertBot(bot: TelegramBot) {
        viewModelScope.launch {
            val insertedId = useCases.insertBot(bot)
            _bot.value = bot.copy(id = insertedId)  // ✅ обновляем ID
        }
    }

    fun insertOrUpdateBot(bot: TelegramBot) {
        viewModelScope.launch {
            if (bot.id == 0L) {
                val id = useCases.insertBot(bot)
                _bot.value = bot.copy(id = id)
            } else {
                val updated = useCases.updateBot(bot)
                if (!updated) {
                    val id = useCases.insertBot(bot)
                    _bot.value = bot.copy(id = id)
                } else {
                    loadBot(bot.id)
                }
            }
        }
    }



    fun updateBot(bot: TelegramBot) {
        viewModelScope.launch {
            val success = useCases.updateBot(bot)
            if (!success) {
                // Если бот не найден → вставляем
                val id = useCases.insertBot(bot)
                _bot.value = bot.copy(id = id)
            } else {
                loadBot(bot.id)
            }
        }
    }


    fun deleteBot(bot: TelegramBot) {
        viewModelScope.launch {
            useCases.deleteBot(bot)
            _bot.value = null
        }
    }

    suspend fun insertBotAndGetId(bot: TelegramBot): Long {
        val insertedId = useCases.insertBot(bot)
        _bot.value = bot.copy(id = insertedId)
        return insertedId
    }

    suspend fun createEmptyBotIfNeeded(): Long {
        val bot = TelegramBot(
            id = 0L,
            botName = "",
            token = "",
            selectedType = "channel",
            chatIds = listOf(""),
            sendMode = SendMode.ONCE.name,
            message = "",
            delayMs = 0L,
            intervalMs = 0L,
            durationSubMode = DurationSubMode.TIMES_PER_SECONDS.name,
            durationTotalTime = 60,
            durationSendCount = 1,
            durationFixedInterval = 10
        )
        val id = useCases.insertBot(bot)
        _bot.value = bot.copy(id = id)
        return id
    }


}
