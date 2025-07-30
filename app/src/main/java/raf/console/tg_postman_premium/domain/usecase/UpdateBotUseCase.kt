package raf.console.tg_postman_premium.domain.usecase

import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import javax.inject.Inject

class UpdateBotUseCase @Inject constructor(
    private val repository: TelegramBotRepository
) {
    suspend operator fun invoke(bot: TelegramBot) {
        repository.update(bot)
    }
}
