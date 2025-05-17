package com.thiago.chatjump.di

import android.content.Context
import androidx.room.Room
import com.thiago.chatjump.data.local.ChatDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "chat_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: ChatDatabase) = database.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase) = database.messageDao()
} 