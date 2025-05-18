package com.thiago.chatjump.ui.voicechat

sealed interface VoiceChatUiEvent {
    object UnexpectedError: VoiceChatUiEvent
    object BackOnline: VoiceChatUiEvent
}