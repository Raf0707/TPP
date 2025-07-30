package raf.console.tg_postman_premium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import raf.console.tg_postman_premium.presentation.screens.BotListScreen
import raf.console.tg_postman_premium.ui.bots.details.BotDetailsScreen
import raf.console.tg_postman_premium.ui.theme.TelegramPostmanTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelegramPostmanTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "bot_list") {
        composable("bot_list") {
            BotListScreen(
                onBotClick = { bot ->
                    navController.navigate("bot_detail/${bot.id}")
                },
                onAddBotClick = {
                    navController.navigate("bot_detail/0") // 0 = новый бот
                }
            )
        }

        composable(
            route = "bot_detail/{botId}",
            arguments = listOf(navArgument("botId") { type = NavType.LongType })
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getLong("botId") ?: 0
            BotDetailsScreen()
        }
    }
}
