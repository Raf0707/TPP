package raf.console.tg_postman_premium.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import raf.console.tg_postman_premium.data.local.dao.TelegramBotDao
import raf.console.tg_postman_premium.data.local.database.AppDatabase
import raf.console.tg_postman_premium.data.repository.TelegramBotRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "telegram_postman_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTelegramBotDao(database: AppDatabase): TelegramBotDao {
        return database.telegramBotDao()
    }

    @Provides
    @Singleton
    fun provideTelegramBotRepository(dao: TelegramBotDao): TelegramBotRepository {
        return TelegramBotRepository(dao)
    }
}
