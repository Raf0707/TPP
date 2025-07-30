package raf.console.tg_postman_premium.domain.model

data class TelegramBot(
    val id: Long = 0,
    val botName: String,
    val token: String,
    val selectedType: String,
    val chatIds: List<String>,
    val sendMode: String,
    val message: String,
    val delayMs: Long,
    val intervalMs: Long,
    val durationSubMode: String,
    val durationTotalTime: Int,
    val durationSendCount: Int,
    val durationFixedInterval: Int
)
