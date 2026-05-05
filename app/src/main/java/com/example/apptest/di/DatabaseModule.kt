package com.example.apptest.di

import android.content.Context
import androidx.room.Room
import com.example.apptest.core.data.local.AppDatabase
import com.example.apptest.core.data.local.dao.FavoritesDao
import com.example.apptest.core.data.local.dao.MovieDao
import com.example.apptest.core.data.local.dao.MovieDetailsDao
import com.example.apptest.core.data.local.dao.VideosDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ¿Por qué Room necesita un @Provides manual?
 *      Room.databaseBuilder() necesita un Context para crear la base de datos.
 *      Hilt no puede saber esto automáticamente (no hay @Inject constructor).
 *      Por eso necesitamos un @Provides igual que con Retrofit
 *
 * @ApplicationContext -> Hilt provee el Context de la Application.
 *      NUNCA usar ActivityContext para Room - la base de datos debe vivir mientra viva la app,
 *      no solo mientras viva una Activity.
 *      Usar ActivityContext causaría memory leaks.
 *
 * @Singleton -> UNA sola instancia de AppDatabase en toda la app.
 *      Room mantiene un connection pool interno. Tener múltiples instancias
 *      causaría inconsistencias y problemas de concurrencia.
 *
 * Proveemos cada DAO por separado:
 *      Esto sigue el principio de segregación de interfaces.
 *      Los repositorios solo reciben el DAO que necesitan,
 *      no la base de datos completa. Más limpio y más fácil de testear.
 */


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // --- AppDatabase ---
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context  // Hilt inyecta el ApplicationContext
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "apptest_database"        //nombre del archivo .db en el dispositivo
        )
        // fallbackToDestructiveMigration() -> si no hay Migration para un cambio
        // de versión, borra y recrea la base de datos.
        // SOLO para desarrollo. En producción usar .addMigrations(...) para
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    // Cada @Provides de DAO recibe AppDatabase y llama al metodo abstracto.
    // Room ya generó la implementación concreta del DAO en compilación.
    // Hilt inyectará estos DAOs en los RepositoryImpl que los necesiten

    @Provides
    @Singleton
    fun provideMovieDao(db: AppDatabase): MovieDao = db.movieDao()

    @Provides
    @Singleton
    fun provideMovieDetailsDao(db: AppDatabase): MovieDetailsDao = db.movieDetailsDao()

    @Provides
    @Singleton
    fun provideVideosDao(db: AppDatabase): VideosDao = db.videosDao()

    @Provides
    @Singleton
    fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()
}