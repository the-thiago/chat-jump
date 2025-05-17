package com.thiago.chatjump.di

import com.thiago.chatjump.domain.repository.AIChatRepository
import com.thiago.chatjump.domain.usecase.GetAIResponseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetAIResponseUseCase(
        repository: AIChatRepository
    ): GetAIResponseUseCase {
        return GetAIResponseUseCase(repository)
    }
} 