package raf.console.tg_postman_premium.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.presentation.viewmodel.TelegramBotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotListScreen(
    onBotClick: (TelegramBot) -> Unit,
    onAddBotClick: () -> Unit,
    viewModel: TelegramBotViewModel = hiltViewModel()
) {
    val bots by viewModel.bots.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.createAndOpenBot { newBotId ->
                    onBotClick(TelegramBot(
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
                    )) // или навигация
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить бота")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Telegram Bots") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (bots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет ботов. Нажмите + чтобы добавить.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bots) { bot ->
                        BotItem(
                            bot = bot,
                            onClick = { onBotClick(bot) },
                            onDelete = { viewModel.deleteBot(bot) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotItem(
    bot: TelegramBot,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(bot.botName, style = MaterialTheme.typography.titleMedium)
                Text("Чатов: ${bot.chatIds.size}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}