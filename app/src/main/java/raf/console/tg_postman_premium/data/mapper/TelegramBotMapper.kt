package raf.console.tg_postman_premium.data.mapper

import raf.console.tg_postman_premium.data.local.entity.TelegramBotEntity
import raf.console.tg_postman_premium.domain.model.TelegramBot

fun TelegramBotEntity.toDomain(): TelegramBot {
    return TelegramBot(
        id = id,
        botName = botName,
        token = token,
        selectedType = selectedType,
        chatIds = chatIds, // ✅ теперь это уже List<String>, конвертировать не нужно
        sendMode = sendMode,
        message = message,
        delayMs = delayMs,
        intervalMs = intervalMs,
        durationSubMode = durationSubMode,
        durationTotalTime = durationTotalTime,
        durationSendCount = durationSendCount,
        durationFixedInterval = durationFixedInterval
    )
}

fun TelegramBot.toEntity(): TelegramBotEntity {
    return TelegramBotEntity(
        id = id,
        botName = botName,
        token = token,
        selectedType = selectedType,
        chatIds = chatIds, // ✅ передаём напрямую список
        sendMode = sendMode,
        message = message,
        delayMs = delayMs,
        intervalMs = intervalMs,
        durationSubMode = durationSubMode,
        durationTotalTime = durationTotalTime,
        durationSendCount = durationSendCount,
        durationFixedInterval = durationFixedInterval
    )
}
