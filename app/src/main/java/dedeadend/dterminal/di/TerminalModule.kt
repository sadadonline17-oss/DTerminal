package dedeadend.dterminal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dedeadend.dterminal.data.CommandExecutor
import dedeadend.dterminal.data.ShellCommandExecutor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TerminalModule {

    @Provides
    @Singleton
    fun provideCommandExecutor(): CommandExecutor {
        return ShellCommandExecutor();
    }
}