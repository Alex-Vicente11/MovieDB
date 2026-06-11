package com.example.apptest.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.apptest.core.data.local.AppDatabase
import com.example.apptest.core.data.local.entity.MovieEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Usando Room en memoria + Robolectric
 *
 * ¿Por qué Robolectric aquí?
 *  Room necesita un Context real de Android para crear la base de datos.
 *  Robolectric sumila ese Context en la JVM sin necesitar emulador.
 *  Room.inMemoryDatabaseBuilder() crea una DB que vive solo durante el test
 *  y se destruye al terminar - cada test parte de cero.
 *
 *  ¿Qué testeamos aquí?
 *    - Las queries SQL definidas con @Query en MovieDao
 *    - El comportamiento de OnConflictStrategy.REPLACE
 *    - La reactivdad del Flow (Room emite cuando cambian los datos)
 *    - El TTL: getLastCacheTime() retorna el timestamp correcto
 *
 * ¿Qué NO testeamos aquí?
 *   - Lógica de negocio (eso es responsabilidad del repositorio)
 *   - Mapeo Entity -> Domain (eso es responsabilidad del mapper)
 */

@RunWith(RobolectricTestRunner::class)
// Config: define el SDK de Android que Robolectric simula
// sdk = 34 es estable y compatible con minSDK = 24
@Config(sdk = [34])
class MovieDaoTest {

    // Base de datos real en memoria - se destruye después de cada test
    private lateinit var db: AppDatabase
    private lateinit var dao: MovieDao

    @Before
    fun setUp() {
        // ApplicationProvider.getApplicationContext() - Robolectric provee
        // un Context real simulado. Sin Robolectric esto lanzaría NullPointerException
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // allowMainThreadQueries() - permite queries en el hilo principal
            .allowMainThreadQueries()
            .build()

        dao = db.movieDao()
    }

    @After
    fun tearDown() {
        // Cierra la BD después de cada test para liberar recursos
        // y garantizar que el siguiente test parte de una BD limpia
        db.close()
    }

    // HELPERS

    /**
     * Convierte un Movie de dominio a MovieEntity lista para insertar en Room
     * cachedAt se fija manualmente para tests de TTL donde necesitamos
     * controlar el timestamp exacto
     */
    private fun createEntity(
        id: Int = 1,
        title: String = "Inception",
        isPopular: Boolean = true,
        cachedAt: Long = System.currentTimeMillis(),
        popularity: Double = 100.0
    ) = MovieEntity(
        id = id,
        title = title,
        overview = "Test overview",
        posterPath = "/poster.jpg",
        backdropPath = null,
        voteAverage = 8.8,
        voteCount = 30000,
        releaseDate = "2010-07-16",
        isPopular = isPopular,
        cachedAt = cachedAt,
        popularity = popularity
    )

    // insertMovies

    @Test
    fun whenMoviesInserted_canBeRetrievedFromDb() = runTest {
        // Given
        val entities = listOf(
            createEntity(id = 1, title = "Inception"),
            createEntity(id = 2, title = "Interstellar"),
            createEntity(id = 3, title = "The Dark Knight")
        )

        // When
        dao.insertMovies(entities)

        // Then - .first() obtiene la primera emisión del Flow de Room (snapshot actual)
        val result = dao.getPopularMovies().first()
        assertThat(result).hasSize(3)
    }

    @Test
    fun whenMovieInsertedWithSameId_replacesExistingMovie() = runTest {
        // Given - OnConflictStrategy.REPLACE: si el ID ya existe, reemplaza la fila
        val original = createEntity(id = 1, title = "Inception")
        val updated = createEntity(id = 1, title = "Inception: Director's Cut")

        // When
        dao.insertMovies(listOf(original))
        dao.insertMovies(listOf(updated))  // mismo ID - debe reemplazar

        // Then - solo debe haber un registro con el título actualizado
        val result = dao.getPopularMovies().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().title).isEqualTo("Inception: Director's Cut")
    }

    @Test
    fun whenMultipleMoviesInserted_areOrderedByPopularityDesc() = runTest {
        // Given
        val entities = listOf(
            createEntity(id = 1, title = "Low", popularity = 30.0),
            createEntity(id = 2, title = "High", popularity = 90.0),
            createEntity(id = 3, title = "Medium", popularity = 60.0)
        )

        // When
        dao.insertMovies(entities)

        // Then - la query tiene ORDER BY popularity DESC
        val result = dao.getPopularMovies().first()
        assertThat(result[0].title).isEqualTo("High")
        assertThat(result[1].title).isEqualTo("Medium")
        assertThat(result[2].title).isEqualTo("Low")
    }

    // getPopularMovies - FILTRO is_popular
    @Test
    fun whenMixedMoviesInserted_getPopularMoviesReturnsOnlyPopular() = runTest {
        // Given - películas populares y de búsqueda en la misma tabla
        val popular = createEntity(id = 1, title = "Popular Movie", isPopular = true)
        val search = createEntity(id = 2, title = "Search Result", isPopular = false)

        // When
        dao.insertMovies(listOf(popular, search))

        // Then - getPopularMovies() filtra por is_popular = 1
        val result = dao.getPopularMovies().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().title).isEqualTo("Popular Movie")
    }

    @Test
    fun whenNoPopularMoviesExist_returnsEmptyList() = runTest {
        // Given - solo hay resultados de búsqueda, ninguna película popular
        val searchResult = createEntity(id = 1, isPopular = false)
        dao.insertMovies(listOf(searchResult))

        // When
        val result = dao.getPopularMovies().first()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun whenDatabaseIsEmpty_getPopularMoviesReturnsEmptyList() = runTest {
        // Given - BD recién creada, sin datos

        // When
        val result = dao.getPopularMovies().first()

        // Then
        assertThat(result).isEmpty()
    }

    // deletePopularMovies

    @Test
    fun whenDeletePopularMoviesCalled_removesOnlyPopularMovies() = runTest {
        // Given - hay populares y resultados de búsqueda
        val popular = createEntity(id = 1, title = "Popular", isPopular = true)
        val search = createEntity(id = 2, title = "Search", isPopular = false)
        dao.insertMovies(listOf(popular, search))

        // when - deletePopularMovies solo borra is_popular = 1
        dao.deletePopularMovies()

        // Then - la búsqueda sigue intacta
        val popularResult = dao.getPopularMovies().first()
        assertThat(popularResult).isEmpty()

        // Verificamos directamente con searchMovies que el resultado de búsqueda persiste
        val searchResult = dao.searchMovies("Search").first()
        assertThat(searchResult).hasSize(1)
    }

    @Test
    fun whenDeletePopularCalledOnEmptyDb_doesNotCrash() = runTest {
        // Verificamos que DELETE en tabla vacía no lanza exception
        dao.deletePopularMovies()  // No debe lanzar nada

        val result = dao.getPopularMovies().first()
        assertThat(result).isEmpty()
    }

    // getLastCacheTime - TTL
    @Test
    fun whenNoCachedMovies_getLastCacheTimeReturnsNull() = runTest {
        // Given - DB vacía

        // When
        val cacheTime = dao.getLastCacheTime()

        // Then - null indica que no hay caché, el repositorio debe llamar a la API
        assertThat(cacheTime).isNull()
    }

    @Test
    fun whenPopularMoviesInserted_getLastCacheTimeReturnsTimestamp() = runTest {
        // Given - timestamp fijo para verificar el valor exacto
        val fixedTimestamp = 1_700_000_000_000L // 2024-01-01T00:00:00Z
        val entity = createEntity(id = 1, cachedAt = fixedTimestamp)

        // When
        dao.insertMovies(listOf(entity))
        val cacheTime = dao.getLastCacheTime()

        // Then - debe retornar el mismo timestamp que se guardó
        assertThat(cacheTime).isEqualTo(fixedTimestamp)
    }

    @Test
    fun whenMultipleMoviesInserted_getLastCacheTimeReturnsAnyTimestamp() = runTest {
        // Given - la query usa LIMIT 1, retorna el cached_at de cualquier película popular
        // Lo importante es que no sea null - indica que hay caché
        val entities = listOf(
            createEntity(id = 1, cachedAt = 1_000L),
            createEntity(id = 2, cachedAt = 2_000L),
            createEntity(id = 3, cachedAt = 3_000L)
        )

        // When
        dao.insertMovies(entities)
        val cacheTime = dao.getLastCacheTime()

        // Then - retorna un valor no nulo (el repositorio puede calcular el TTL)
        assertThat(cacheTime).isNotNull()
    }

    @Test
    fun whenOnlySearchResultsExist_getLastCacheTimeReturnsNull() = runTest {
        // Given - solo hay resultados de búsqueda (is_popular = false)
        // getLastCacheTime filtra por is_popular = 1
        val searchEntity = createEntity(id = 1, isPopular = false)
        dao.insertMovies(listOf(searchEntity))

        // When
        val cacheTime = dao.getLastCacheTime()

        // Then - null porque no hay películas populares en caché
        assertThat(cacheTime).isNull()
    }

    // searchMovies

    @Test
    fun whenQueryMatchesTitle_returnsMatchingMovies() = runTest {
        // Given
        val entities = listOf(
            createEntity(id = 1, title = "Batman Begins", isPopular = false),
            createEntity(id = 2, title = "The Dark Knight", isPopular = false),
            createEntity(id = 3, title = "Batman vs Superman", isPopular = false)
        )
        dao.insertMovies(entities)

        // When - búsqueda parcial: LIKE '%batman%'
        val result = dao.searchMovies("batman").first()

        // Then - debe retornar las dos películas con "batman" en el título
        assertThat(result).hasSize(2)
        assertThat(result.map { it.title})
            .containsExactly("Batman Begins", "Batman vs Superman")
    }

    @Test
    fun whenQueryIsCaseInsensitive_returnsMatchingMovies() = runTest {
        // Given
        dao.insertMovies(listOf(
            createEntity(id = 1, title = "Inception", isPopular = false)
        ))

        // When - SQLite LIKE es case-insensitive para ASCII por defecto
        val result = dao.searchMovies("inception").first()

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun whenQueryDoesNotMatch_returnsEmptyList() = runTest {
        // Given
        dao.insertMovies(listOf(
            createEntity(id = 1, title = "Inception", isPopular = false)
        ))

        // When
        val result = dao.searchMovies("xvercnotfound").first()

        // Then
        assertThat(result).isEmpty()
    }

    // REACTIVIDAD DEL FLOW

    @Test
    fun whenMoviesInsertedAfterObserving_flowEmitsUpdatedList() = runTest {
        // Turbine maneja el timing internamente — no necesitamos advanceUntilIdle()
        dao.getPopularMovies().test {

            // Primera emisión: BD vacía al momento de suscribirse
            val firstEmission = awaitItem()
            assertThat(firstEmission).isEmpty()

            // Insertamos datos — Room detecta el cambio y emite automáticamente
            dao.insertMovies(listOf(createEntity(id = 1)))

            // Segunda emisión: Room emite la lista actualizada
            val secondEmission = awaitItem()
            assertThat(secondEmission).hasSize(1)
            assertThat(secondEmission.first().id).isEqualTo(1)

            cancelAndIgnoreRemainingEvents()
        }
    }
}