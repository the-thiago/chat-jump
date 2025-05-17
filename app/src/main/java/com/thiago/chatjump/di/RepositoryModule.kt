package com.thiago.chatjump.di

import com.thiago.chatjump.data.repository.AIChatRepositoryImpl
import com.thiago.chatjump.domain.repository.AIChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAIChatRepository(
        aiChatRepositoryImpl: AIChatRepositoryImpl
    ): AIChatRepository
} 