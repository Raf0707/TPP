package raf.console.tg_postman_premium.ui.bots.details

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.browse.MediaBrowser
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import raf.console.tg_postman_premium.ui.components.RadioButtonWithLabel
import raf.console.tg_postman_premium.data.TelegramDataStore
import raf.console.tg_postman_premium.data.TelegramSettings
import raf.console.tg_postman_premium.service.TelegramBotService
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import raf.console.tg_postman_premium.data.ContactData
import androidx.media3.common.MediaItem
import raf.console.tg_postman_premium.domain.model.TelegramBot
import raf.console.tg_postman_premium.ui.components.TimePickerField
import raf.console.tg_postman_premium.presentation.screens.activity.GeoPickerActivity
import raf.console.tg_postman_premium.presentation.screens.activity.MapPickerActivity
import raf.console.tg_postman_premium.service.TelegramForegroundService
import raf.console.tg_postman_premium.utils.compressVideoStandard


enum class SendMode {
    ONCE, MULTIPLE, DURATION
}

enum class DurationSubMode {
    TIMES_PER_SECONDS, FIXED_INTERVAL
}

enum class TelegramMessageType(val label: String) {
    TEXT("Текст"),
    PHOTO("Фото"),
    DOCUMENT("Документ"),
    VIDEO("Видео"),
    AUDIO("Аудио"),
    CONTACT("Контакт"),
    LOCATION("Геолокация")
}

enum class MapProvider {
    GOOGLE,
    YANDEX
}

@SuppressLint("Range")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotDetailsScreen(
    botId: Long? = null,
    viewModel: BotDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val existingBot by viewModel.bot.collectAsState()

    // Загружаем данные бота, если редактируем
    LaunchedEffect(botId) {
        if (botId != null) viewModel.loadBot(botId)
    }

    // Общие состояния
    var botName by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("channel") }
    val chatIds = remember { mutableStateListOf("") }
    var status by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var infoDialogText by remember { mutableStateOf<String?>(null) }

    // Режимы отправки
    var sendMode by rememberSaveable { mutableStateOf(SendMode.ONCE) }
    var sendWithDelay by rememberSaveable { mutableStateOf(false) }

    var sendCount by rememberSaveable { mutableStateOf("3") }
    var intervalMs by rememberSaveable { mutableStateOf(0L) }

    var durationSubMode by rememberSaveable { mutableStateOf(DurationSubMode.TIMES_PER_SECONDS) }
    var durationTotalTime by rememberSaveable { mutableStateOf("60") }
    var durationSendCount by rememberSaveable { mutableStateOf("3") }
    var durationFixedInterval by rememberSaveable { mutableStateOf("10") }

    var delayMs by rememberSaveable { mutableStateOf(0L) }
    var mediaUri by rememberSaveable { mutableStateOf<String?>(null) }
    var messageType by rememberSaveable { mutableStateOf(TelegramMessageType.TEXT) }
    val geoPoint = rememberSaveable { mutableStateOf<Pair<Double, Double>?>(null) }
    var selectedContact by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    val selectedDocs = remember { mutableStateListOf<Uri>() }
    val selectedAudios = remember { mutableStateListOf<Uri>() }
    val multiMediaUris = remember { mutableStateListOf<String>() }
    var selectedMapProvider by rememberSaveable { mutableStateOf(MapProvider.YANDEX) }

    var sentMessages by rememberSaveable { mutableStateOf(0) }
    var totalMessages by rememberSaveable { mutableStateOf(0) }

    // Лаунчеры
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> mediaUri = uri?.toString() }

    val multiImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> multiMediaUris.setAll(uris.map { it.toString() }) }

    val multiDocumentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> selectedDocs.setAll(uris) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted)
            Toast.makeText(context, "Нет разрешения на чтение контактов", Toast.LENGTH_SHORT).show()
    }

    val geoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("latitude", 0.0)
            val lon = result.data?.getDoubleExtra("longitude", 0.0)
            if (lat != null && lon != null) geoPoint.value = lat to lon
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val contactId = it.getString(idIndex)
                    val name = it.getString(nameIndex)

                    // Получаем номер телефона
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val phoneNumber = pc.getString(
                                pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            selectedContact = name to phoneNumber
                        }
                    }
                }
            }
        }
    }

    // Заполняем поля из существующего бота
    LaunchedEffect(existingBot) {
        existingBot?.let { bot ->
            botName = bot.botName
            token = bot.token
            selectedType = bot.selectedType
            chatIds.setAll(bot.chatIds)
            sendMode = SendMode.valueOf(bot.sendMode)
            message = bot.message
            delayMs = bot.delayMs
            intervalMs = bot.intervalMs
            durationSubMode = DurationSubMode.valueOf(bot.durationSubMode)
            durationTotalTime = bot.durationTotalTime.toString()
            durationSendCount = bot.durationSendCount.toString()
            durationFixedInterval = bot.durationFixedInterval.toString()
        }
    }

    // Сохраняем/обновляем бота
    fun saveBot() {
        val newBot = TelegramBot(
            id = existingBot?.id ?: 0L,
            botName = botName,
            token = token,
            selectedType = selectedType,
            chatIds = chatIds.toList(),
            sendMode = sendMode.name,
            message = message,
            delayMs = delayMs,
            intervalMs = intervalMs,
            durationSubMode = durationSubMode.name,
            durationTotalTime = durationTotalTime.toIntOrNull() ?: 60,
            durationSendCount = durationSendCount.toIntOrNull() ?: 3,
            durationFixedInterval = durationFixedInterval.toIntOrNull() ?: 10
        )

        if (existingBot == null) {
            viewModel.insertBot(newBot)
        } else {
            viewModel.updateBot(newBot)
        }
    }

    fun updateMyBot() {
        val newBot = TelegramBot(
            id = existingBot?.id ?: 0L,
            botName = botName,
            token = token,
            selectedType = selectedType,
            chatIds = chatIds.toList(),
            sendMode = sendMode.name,
            message = message,
            delayMs = delayMs,
            intervalMs = intervalMs,
            durationSubMode = durationSubMode.name,
            durationTotalTime = durationTotalTime.toIntOrNull() ?: 60,
            durationSendCount = durationSendCount.toIntOrNull() ?: 3,
            durationFixedInterval = durationFixedInterval.toIntOrNull() ?: 10
        )


        viewModel.updateBot(newBot)

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            }
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                updateMyBot()
                (context as? Activity)?.onBackPressed()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Telegram Postman",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(Modifier.height(16.dp))


        OutlinedTextField(
            value = botName,
            onValueChange = { botName = it; updateMyBot() },
            label = { Text("Название бота") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it; updateMyBot() },
            label = { Text("Токен бота") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Тип назначения:", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedType == "channel",
                onClick = { selectedType = "channel"; updateMyBot() }
            )
            Text("Канал")
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = selectedType == "group",
                onClick = { selectedType = "group"; updateMyBot() }
            )
            Text("Группа")
        }

        Spacer(Modifier.height(8.dp))
        Text("Chat ID", style = MaterialTheme.typography.titleMedium)

        chatIds.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        chatIds[index] = it
                        updateMyBot()
                    },
                    label = { Text("Chat ID ${index + 1}") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp)
                )
                if (chatIds.size > 1) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        chatIds.removeAt(index)
                        updateMyBot()
                    }) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalButton(onClick = {
                if (chatIds.size < 1000) {
                    chatIds.add("")
                    updateMyBot()
                }
            }) {
                Text("Добавить")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Режим отправки:", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // --- Отправить 1 раз ---
            RadioButtonWithLabel(
                selected = sendMode == SendMode.ONCE,
                onClick = { sendMode = SendMode.ONCE; updateMyBot() },
                label = "Отправить 1 раз"
            )

            /*if (sendMode == SendMode.ONCE) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendWithDelay,
                        onCheckedChange = { sendWithDelay = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Включить таймер")
                }

                if (sendWithDelay) {
                    Spacer(Modifier.height(8.dp))
                    Text("Таймер отправки")
                    Text("Задержка: ${delaySeconds.toInt()} секунд")
                    Slider(
                        value = delaySeconds,
                        onValueChange = { delaySeconds = it; saveAll() },
                        valueRange = 1f..180f,
                        steps = 20
                    )
                }
            }*/

            if (sendMode == SendMode.ONCE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sendWithDelay, onCheckedChange = { sendWithDelay = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Включить таймер")
                }
                if (sendWithDelay) {
                    Text("Таймер отправки")
                    TimePickerField(
                        label = "Задержка",
                        type = "delay"
                    ) { ms ->
                        delayMs = ms
                        //saveAll()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Отправить несколько раз ---
            RadioButtonWithLabel(
                selected = sendMode == SendMode.MULTIPLE,
                onClick = { sendMode = SendMode.MULTIPLE; updateMyBot() },
                label = "Отправить несколько раз"
            )

            if (sendMode == SendMode.MULTIPLE) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sendCount,
                    onValueChange = { sendCount = it },
                    label = { Text("Количество раз") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Частота отправки")
                Text("Интервал: ${(intervalMs / 1000).toInt()} секунд")
                /*Slider(
                    value = intervalSeconds,
                    onValueChange = { intervalSeconds = it },
                    valueRange = 1f..180f,
                    steps = 20
                )*/
                TimePickerField(
                    label = "Интервал",
                    type = "interval"
                ) { ms ->
                    intervalMs = ms
                    //saveAll()
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = sendWithDelay,
                        onCheckedChange = { sendWithDelay = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Включить таймер")
                }

                if (sendWithDelay) {
                    Spacer(Modifier.height(8.dp))
                    Text("Задержка: ${(delayMs / 1000).toInt()} секунд")
                    /*Slider(
                        value = delaySeconds,
                        onValueChange = { delaySeconds = it },
                        valueRange = 1f..180f,
                        steps = 20
                    )*/
                    TimePickerField(
                        label = "Задержка",
                        type = "delay",
                    ) { ms ->
                        delayMs = ms
                        //saveAll()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Отправлять в течение времени ---
            RadioButtonWithLabel(
                selected = sendMode == SendMode.DURATION,
                onClick = { sendMode = SendMode.DURATION; updateMyBot() },
                label = "Отправлять в течение времени"
            )

            if (sendMode == SendMode.DURATION) {
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(12.dp)) {
                        RadioButtonWithLabel(
                            selected = durationSubMode == DurationSubMode.TIMES_PER_SECONDS,
                            onClick = { durationSubMode = DurationSubMode.TIMES_PER_SECONDS },
                            label = "Отправить N раз за K секунд"
                        )
                        RadioButtonWithLabel(
                            selected = durationSubMode == DurationSubMode.FIXED_INTERVAL,
                            onClick = { durationSubMode = DurationSubMode.FIXED_INTERVAL },
                            label = "За K секунд с интервалом X"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (durationSubMode == DurationSubMode.TIMES_PER_SECONDS) {
                    Row {
                        OutlinedTextField(
                            value = durationSendCount,
                            onValueChange = { durationSendCount = it },
                            label = { Text("Количество (N)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = durationTotalTime,
                            onValueChange = { durationTotalTime = it },
                            label = { Text("Время (K)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val l = runCatching {
                        durationTotalTime.toInt() / durationSendCount.toInt()
                    }.getOrNull()

                    if (l != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Интервал: $l секунд")
                    }
                }

                if (durationSubMode == DurationSubMode.FIXED_INTERVAL) {
                    Row {
                        OutlinedTextField(
                            value = durationTotalTime,
                            onValueChange = { durationTotalTime = it },
                            label = { Text("Время (K)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = durationFixedInterval,
                            onValueChange = { durationFixedInterval = it },
                            label = { Text("Интервал (X)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val times = runCatching {
                        durationTotalTime.toInt() / durationFixedInterval.toInt()
                    }.getOrNull()

                    if (times != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Будет отправлено: $times сообщений")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        val messageFieldEnabled = messageType.supportsCaption()

        OutlinedTextField(
            value = message,
            onValueChange = {
                if (messageFieldEnabled) {
                    message = it
                    updateMyBot()
                }
            },
            label = { Text("Сообщение") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            enabled = messageFieldEnabled,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        )


        Spacer(Modifier.height(16.dp))

        Text("Тип сообщения:")

        Spacer(Modifier.height(12.dp))

        when (messageType) {
            TelegramMessageType.PHOTO -> {
                Text("Фото", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Text("Выбрать изображение")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (mediaUri != null) {
                            TextButton(onClick = { mediaUri = null }) {
                                Text("Удалить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { multiImagePickerLauncher.launch("image/*") }) {
                            Text("Выбрать несколько")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (multiMediaUris.isNotEmpty()) {
                            TextButton(onClick = { multiMediaUris.clear() }) {
                                Text("Удалить все")
                            }
                        }
                    }

                    // Превью выбранных изображений
                    if (multiMediaUris.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow {
                            items(multiMediaUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

            }

            TelegramMessageType.VIDEO -> {
                Text("Видео", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { imagePickerLauncher.launch("video/*") }) {
                            Text("Выбрать видео")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (mediaUri != null) {
                            TextButton(onClick = { mediaUri = null }) {
                                Text("Удалить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Выбор нескольких аудиофайлов
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { Toast.makeText(context, "Отправьте несколько видео как документы! Telegram API ограничивает скорость и объем потоковой загрузки", Toast.LENGTH_SHORT).show() }) {
                            Text("Выбрать несколько")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (selectedAudios.isNotEmpty()) {
                            TextButton(onClick = { selectedAudios.clear() }) {
                                Text("Удалить все")
                            }
                        }
                    }

//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        OutlinedButton(onClick = { multiVideoPickerLauncher.launch(("video/*")) }) {
//                            Text("Выбрать несколько")
//                        }
//                        Spacer(Modifier.width(16.dp))
//                        if (selectedVideos.isNotEmpty()) {
//                            TextButton(onClick = { selectedVideos.clear() }) {
//                                Text("Удалить все")
//                            }
//                        }
//                    }
//
//                    if (selectedVideos.isNotEmpty()) {
//                        Spacer(Modifier.height(8.dp))
//                        LazyRow {
//                            items(selectedVideos) { uri ->
//                                AndroidView(
//                                    factory = { ctx ->
//                                        PlayerView(ctx).apply {
//                                            player = ExoPlayer.Builder(ctx).build().also {
//                                                it.setMediaItem(MediaItem.fromUri(Uri.parse(uri.toString())))
//                                                it.prepare()
//                                                it.playWhenReady = false
//                                            }
//                                        }
//                                    },
//                                    modifier = Modifier
//                                        .width(120.dp)
//                                        .height(100.dp)
//                                        .padding(4.dp)
//                                )
//                            }
//                        }
//                    }
                }
            }



            TelegramMessageType.DOCUMENT -> {
                Text("Документ", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { imagePickerLauncher.launch("*/*") }) {
                            Text("Выбрать документ")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (mediaUri != null) {
                            TextButton(onClick = { mediaUri = null }) {
                                Text("Удалить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { multiDocumentPickerLauncher.launch(arrayOf("*/*")) }) {
                            Text("Выбрать несколько")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (selectedDocs.isNotEmpty()) {
                            TextButton(onClick = { selectedDocs.clear() }) {
                                Text("Удалить все")
                            }
                        }
                    }

                    if (selectedDocs.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp) // ограничиваем высоту, чтобы не было бесконечной
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                                .padding(4.dp)
                                .border(1.dp, Color.Gray)
                        ) {
                            selectedDocs.forEach { uri: Uri ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .border(1.dp, Color.LightGray)
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(uri.toString().substringAfterLast('/'))
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                }
            }


            TelegramMessageType.AUDIO -> {
                Text("Аудио", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column {
                    // Выбор одного аудиофайла
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { imagePickerLauncher.launch("audio/*") }) {
                            Text("Выбрать аудио")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (mediaUri != null) {
                            TextButton(onClick = { mediaUri = null }) {
                                Text("Удалить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Выбор нескольких аудиофайлов
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { Toast.makeText(context, "Отправьте несколько аудиофайлов как документы", Toast.LENGTH_SHORT).show() }) {
                            Text("Выбрать несколько")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (selectedAudios.isNotEmpty()) {
                            TextButton(onClick = { selectedAudios.clear() }) {
                                Text("Удалить все")
                            }
                        }
                    }

                    // Отображение выбранных аудиофайлов
                    if (selectedAudios.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(selectedAudios) { uri ->
                                val fileName = Uri.parse(uri.toString()).lastPathSegment ?: "Аудио"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(fileName)
                                }
                            }
                        }
                    }
                }
            }



            TelegramMessageType.CONTACT -> {
                Text("Контакт", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column {
                    // Выбор одного контакта
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                    contactPickerLauncher.launch(null)
                                }
                                else -> contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }) {
                            Text("Выбрать контакт")
                        }
                        Spacer(Modifier.width(16.dp))
                        if (selectedContact != null) {
                            TextButton(onClick = { selectedContact = null }) {
                                Text("Удалить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Мультивыбор (накопительный)
                    /*OutlinedButton(onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                contactPickerLauncher.launch(null) // повторный вызов → добавляем в multiContacts
                            }
                            else -> contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) {
                        Text("Выбрать несколько")
                    }

                    // Отображение выбранных контактов
                    if (multiContacts.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(multiContacts) { (name, phone) ->
                                Text("$name ($phone)")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { multiContacts.clear() }) {
                            Text("Удалить всех")
                        }
                    }*/
                }
            }


            TelegramMessageType.LOCATION -> {
                Column {
                    Text("Геопозиция", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Button(onClick = {
                        val intent = Intent(context, MapPickerActivity::class.java)
                        geoPickerLauncher.launch(intent)
                    }) {
                        Text("Выбрать геопозицию")
                    }

                    Spacer(Modifier.height(8.dp))

                    geoPoint.value?.let { (lat, lon) ->
                        Text("Геопозиция: $lat, $lon", modifier = Modifier.padding(bottom = 8.dp))

                        val staticMapUrl = when (selectedMapProvider) {
                            MapProvider.GOOGLE -> "https://maps.googleapis.com/maps/api/staticmap?center=$lat,$lon&zoom=15&size=600x300&markers=color:red%7C$lat,$lon&key=YOUR_GOOGLE_API_KEY"
                            MapProvider.YANDEX -> "https://static-maps.yandex.ru/1.x/?ll=$lon,$lat&z=15&size=600,300&l=map&pt=$lon,$lat,pm2rdm"
                        }

                        AsyncImage(
                            model = staticMapUrl,
                            contentDescription = "Превью геопозиции",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { geoPoint.value = null }) {
                            Text("Удалить геопозицию")
                        }
                    } ?: Text("Геопозиция не выбрана")
                }
            }


            else -> Unit
        }


        TelegramMessageType.values().forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = messageType == type,
                    onClick = { messageType = type }
                )
                Text(type.label)
            }
        }

        //Spacer(Modifier.height(16.dp))

        //Spacer(Modifier.height(24.dp))

        // === Отображение выбранного медиа ===
        //Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(16.dp))

        // === Кнопка (создание или отправка) ===
        Button(
            onClick = {
                if (token.isBlank() || message.isBlank()) {
                    status = "❗ Заполните токен и сообщение"
                    return@Button
                }

                val ids = chatIds.mapNotNull { raw ->
                    val id = raw.trim()
                    when (selectedType) {
                        "group" -> if (!id.startsWith("-")) "-$id" else id
                        "channel" -> if (!id.startsWith("@")) "@$id" else id
                        else -> id
                    }
                }

                if (ids.isEmpty()) {
                    status = "❗ Укажите хотя бы один Chat ID"
                    return@Button
                }

                val intent = Intent(context, TelegramForegroundService::class.java).apply {
                    putExtra("token", token)
                    putExtra("chatIds", ArrayList(ids))
                    putExtra("message", message)
                    putExtra("sendMode", sendMode.name)
                    putExtra("delayMs", if (sendWithDelay) delayMs else 0L)
                    putExtra("intervalMs", intervalMs)
                    putExtra("repeatCount", sendCount.toIntOrNull() ?: 1)
                    putExtra("messageType", messageType.name)
                }
                ContextCompat.startForegroundService(context, intent)

                status = "📤 Рассылка запущена в фоне..."
                isSending = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSending
        ) {
            Text(if (isSending) "Отправка..." else "Отправить")
        }


        Spacer(Modifier.height(12.dp))

        if (isSending) {
            val messagesPerChat = sendCount.toIntOrNull() ?: 1
            val chatCount = chatIds.size

            Text(
                text = "Отправка $messagesPerChat сообщений в $chatCount чатов",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LinearProgressIndicator(
                progress = if (totalMessages > 0) sentMessages.toFloat() / totalMessages.toFloat() else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (status.isNotBlank()) {
            Text(status, color = MaterialTheme.colorScheme.error)
        }


    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    "raf.console.tg_postman_premium.ACTION_SEND_COMPLETE" -> {
                        val success = intent.getBooleanExtra("success", false)
                        status = if (success) "✅ Сообщения успешно отправлены" else "⚠️ Ошибка отправки"
                        isSending = false
                        sentMessages = 0
                        totalMessages = 0
                    }
                    "raf.console.tg_postman_premium.ACTION_SEND_PROGRESS" -> {
                        sentMessages = intent.getIntExtra("sent", 0)
                        totalMessages = intent.getIntExtra("total", 0)
                        status = "📤 Отправлено $sentMessages / $totalMessages сообщений"
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("raf.console.tg_postman_premium.ACTION_SEND_COMPLETE")
            addAction("raf.console.tg_postman_premium.ACTION_SEND_PROGRESS")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
            updateMyBot()
        }
    }
}

private fun <T> SnapshotStateList<T>.setAll(list: List<T>) {
    clear()
    addAll(list)
}

fun TelegramMessageType.supportsCaption(): Boolean {
    return this == TelegramMessageType.TEXT ||
            this == TelegramMessageType.PHOTO ||
            this == TelegramMessageType.VIDEO ||
            this == TelegramMessageType.DOCUMENT ||
            this == TelegramMessageType.AUDIO
}