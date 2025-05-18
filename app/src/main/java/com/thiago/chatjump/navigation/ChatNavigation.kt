package com.thiago.chatjump.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thiago.chatjump.ui.chat.ChatScreen
import com.thiago.chatjump.ui.conversations.ConversationHistoryScreen
import com.thiago.chatjump.ui.voicechat.VoiceChatScreen

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
                navArgument("conversationId") {
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
                    navController.navigate(Screen.VoiceChat.route)
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

        composable(Screen.VoiceChat.route) {
            VoiceChatScreen()
        }
    }
} 