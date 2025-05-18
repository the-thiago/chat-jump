package com.thiago.chatjump.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Int = -1) = "chat/$conversationId"
    }
    object ConversationHistory : Screen("conversation_history")
    object VoiceChat : Screen("voice_chat")
}