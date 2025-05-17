package com.thiago.chatjump.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thiago.chatjump.ui.chat.ChatScreen
import com.thiago.chatjump.ui.conversations.ConversationHistoryScreen
import com.thiago.chatjump.ui.realtime.RealTimeScreen

sealed class Screen(val route: String) {
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Int = -1) = "chat/$conversationId"
    }
    object ConversationHistory : Screen("conversation_history")
    object RealTime : Screen("realtime")
}

@Composable
fun ChatNavigation(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.createRoute(),
    ) {
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                androidx.navigation.navArgument("conversationId") {
                    type = androidx.navigation.NavType.IntType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: -1
            ChatScreen(
                onConversationHistoryClick = {
                    navController.navigate(Screen.ConversationHistory.route)
                },
                onRealTimeClick = {
                    navController.navigate(Screen.RealTime.route)
                },
                conversationId = conversationId
            )
        }

        composable(Screen.ConversationHistory.route) {
            ConversationHistoryScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversationClick = {
                    navController.navigate(Screen.Chat.createRoute()) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.RealTime.route) {
            RealTimeScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
    }
} 