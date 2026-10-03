package com.fitpaisa.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * M0 Room stub. Holds the placeholder [PendingSyncEntity]; M1 adds the real
 * step-cache entities. Never stores balances — those live server-side.
 */
@Database(entities = [PendingSyncEntity::class], version = 1, exportSchema = false)
abstract class FitPaisaDatabase : RoomDatabase() {
    abstract fun pendingSyncDao(): PendingSyncDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitPaisaDatabase =
        Room.databaseBuilder(context, FitPaisaDatabase::class.java, "fitpaisa.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePendingSyncDao(db: FitPaisaDatabase): PendingSyncDao = db.pendingSyncDao()
}
