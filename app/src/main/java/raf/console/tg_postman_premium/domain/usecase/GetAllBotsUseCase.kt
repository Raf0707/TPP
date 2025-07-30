package raf.console.tg_postman_premium.domain.usecase

import kotlinx.coroutines.flow.Flow
import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import javax.inject.Inject

class GetAllBotsUseCase @Inject constructor(
    private val repository: TelegramBotRepository
) {
    operator fun invoke(): Flow<List<TelegramBot>> {
        return repository.getAllBots()
    }
}
