package raf.console.tg_postman_premium.ui.bots.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
            _bot.value = bot.copy(id = insertedId)
        }
    }

    fun updateBot(bot: TelegramBot) {
        viewModelScope.launch {
            useCases.updateBot(bot)
            _bot.value = bot
        }
    }

    fun deleteBot(bot: TelegramBot) {
        viewModelScope.launch {
            useCases.deleteBot(bot)
            _bot.value = null
        }
    }


}
