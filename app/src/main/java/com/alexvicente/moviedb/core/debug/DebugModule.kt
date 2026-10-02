package com.alexvicente.moviedb.core.debug

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugModule {
    @Binds
    abstract fun bindDebugActions(impl: DebugActionsImpl): DebugActions
}