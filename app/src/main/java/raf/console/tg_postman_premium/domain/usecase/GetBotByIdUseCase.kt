package raf.console.tg_postman_premium.domain.usecase

import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import javax.inject.Inject

class GetBotByIdUseCase @Inject constructor(
    private val repository: TelegramBotRepository
) {
    suspend operator fun invoke(id: Long): TelegramBot? {
        return repository.getBotById(id)
    }
}
