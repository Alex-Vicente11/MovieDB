package com.example.apptest.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.apptest.core.data.local.dao.FavoritesDao
import com.example.apptest.core.data.local.dao.MovieDao
import com.example.apptest.core.data.local.dao.MovieDetailsDao
import com.example.apptest.core.data.local.dao.VideosDao
import com.example.apptest.core.data.local.entity.FavoriteEntity
import com.example.apptest.core.data.local.entity.MovieDetailsEntity
import com.example.apptest.core.data.local.entity.MovieEntity
import com.example.apptest.core.data.local.entity.VideoEntity

/**
 * @Database -> marca esta clase como la base de datos de la app
 *  entities -> lista de TODAS las tablas que Room debe crear.
 *              Cada @Entity aquí = una tabla en SQLite.
 *  version  -> número de versión del esquema. DEBE incrementarse cuando
 *              cambias la estructura de una tabla (agregar/eliminar columnas).
 *              Si cambias el esquema sin incrementar la versión -> crash.
 *  exportSchema -> guarda el esquema en un archivo JSON para historial.
 *                  false en desarrollo para no generar archivos extra.
 *                  true en producción para mantener historial de migraciones.
 *
 * @TypeConverters -> registra los convertidores de tipos.
 *  Room los busca automáticamente cuando necesita convertir List<> o tipos complejos.
 *
 * abstract class -> Room genera la implementación concreta en compilación.
 *  Tú defines los @Dao abstractos; Room genera el código que llama a SQLite.
 *
 *  SINGLETON via Hilt:
 *      AppDatabase NO es un object singleton manual. Hilt lo gestiona como
 *      @Singleton en DatabaseModule.kt - una sola instancia en toda la app.
 *
 * VERSIONES Y MIGRACIONES:
 *  Cuando agregues una columna o tabla nueva en el futuro:
 *  1. Incrementa version = 2
 *  2. Crea una Migración(1, 2) { database.execSQL("ALTER TABLE...") }
 *  3. Agrégala en DataBaseModule: .addMigrations(MIGRATION_1_2)
 *  Sin Migracion, Room lanza IllegalStateException al detectar cambio de esquema.
 */

@Database(
    entities = [
        MovieEntity::class,     // tabla: movies (populares + búsqueda)
        MovieDetailsEntity::class, // tabla: movie_details
        VideoEntity::class,         // tabla: videos
        FavoriteEntity::class,      // tablas: favorites
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Room genera la implementación de cada DAO automáticamente.
    // Hilt inyecta AppDatabase donde sea necesario y llama a estas funciones
    // para obtener los DAOs específicos.

    abstract fun movieDao(): MovieDao
    abstract fun movieDetailsDao(): MovieDetailsDao
    abstract fun videosDao(): VideosDao
    abstract fun favoritesDao(): FavoritesDao
}