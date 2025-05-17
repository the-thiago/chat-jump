package com.thiago.chatjump.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thiago.chatjump.ui.chat.ChatScreen
import com.thiago.chatjump.ui.conversations.ConversationHistoryScreen

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object ConversationHistory : Screen("conversation_history")
    object ChatWithId : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Int) = "chat/$conversationId"
    }
}

@Composable
fun ChatNavigation(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onConversationHistoryClick = {
                    navController.navigate(Screen.ConversationHistory.route)
                },
                conversationId = 0 // Default ID for new conversation
            )
        }

        composable(Screen.ConversationHistory.route) {
            ConversationHistoryScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.ChatWithId.createRoute(conversationId))
                },
                onNewConversationClick = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(
            route = Screen.ChatWithId.route,
            arguments = listOf(
                androidx.navigation.navArgument("conversationId") {
                    type = androidx.navigation.NavType.IntType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: 0
            ChatScreen(
                onConversationHistoryClick = {
                    navController.navigate(Screen.ConversationHistory.route)
                },
                conversationId = conversationId
            )
        }
    }
} 