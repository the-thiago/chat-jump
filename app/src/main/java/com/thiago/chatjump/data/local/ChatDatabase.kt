package com.thiago.chatjump.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thiago.chatjump.data.local.dao.ConversationDao
import com.thiago.chatjump.data.local.dao.MessageDao
import com.thiago.chatjump.data.local.entity.ConversationEntity
import com.thiago.chatjump.data.local.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
} 