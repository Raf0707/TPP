package raf.console.tg_postman_premium.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import raf.console.tg_postman_premium.domain.model.TelegramBot
import javax.inject.Inject

@HiltViewModel
class TelegramBotViewModel @Inject constructor(
    private val repository: TelegramBotRepository
) : ViewModel() {

    private val _bots = MutableStateFlow<List<TelegramBot>>(emptyList())
    val bots: StateFlow<List<TelegramBot>> = _bots.asStateFlow()

    init {
        loadBots()
    }

    private fun loadBots() {
        viewModelScope.launch {
            repository.getAllBots().collectLatest { botsList ->
                _bots.value = botsList
            }
        }
    }

    fun deleteBot(bot: TelegramBot) {
        viewModelScope.launch {
            repository.delete(bot)
        }
    }

    fun createAndOpenBot(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newBot = TelegramBot(
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
            val id = repository.insert(newBot)
            onCreated(id)
        }
    }
}
