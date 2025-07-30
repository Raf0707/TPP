package raf.console.tg_postman_premium.domain.usecase

import javax.inject.Inject

data class TelegramBotUseCases @Inject constructor(
    val getAllBots: GetAllBotsUseCase,
    val getBotById: GetBotByIdUseCase,
    val insertBot: InsertBotUseCase,
    val updateBot: UpdateBotUseCase,
    val deleteBot: DeleteBotUseCase
)
