package com.thiago.chatjump.ui.voicechat

import android.content.Context

sealed interface VoiceChatEvent {
    data class StartConversation(val context: Context) : VoiceChatEvent
}