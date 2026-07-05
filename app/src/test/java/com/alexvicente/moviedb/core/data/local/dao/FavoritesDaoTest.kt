package com.alexvicente.moviedb.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.local.AppDatabase
import com.alexvicente.moviedb.core.data.local.entity.FavoriteEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests del DAO de favoritos con Room real en memoria + Robolectric.
 *
 * A diferencia de MovieDaoTest, FavoritesDao usa Flow<Boolean> para
 * isFavorite() — testeamos que el Flow emite true/false correctamente
 * y que se actualiza reactivamente al agregar/eliminar favoritos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritesDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavoritesDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoritesDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun createFavoriteEntity(
        movieId: Int = 1,
        title: String = "Inception",
        addedAt: Long = movieId.toLong() * 1000L
    ) = FavoriteEntity(
        movieId = movieId,
        title = title,
        posterPath = "/poster.jpg",
        voteAverage = 8.8,
        releaseDate = "2010-07-16",
        overview = "A thief who steals corporate secrets",
        addedAt = addedAt
    )

    // ── Tests de getAllFavorites ───────────────────────────────────────────────

    @Test
    fun whenDatabaseIsEmpty_getAllFavoritesReturnsEmptyList() = runTest {
        val result = dao.getAllFavorites().first()
        assertThat(result).isEmpty()
    }

    @Test
    fun whenFavoriteAdded_appearsInGetAllFavorites() = runTest {
        // Given
        val entity = createFavoriteEntity(movieId = 1, title = "Inception")

        // When
        dao.addFavorite(entity)

        // Then
        val result = dao.getAllFavorites().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().title).isEqualTo("Inception")
    }

    @Test
    fun whenMultipleFavoritesAdded_orderedByAddedAtDesc() = runTest {
        // Given — addedAt distintos para verificar el ORDER BY
        dao.addFavorite(createFavoriteEntity(movieId = 1, title = "First",  addedAt = 1000L))
        dao.addFavorite(createFavoriteEntity(movieId = 2, title = "Second", addedAt = 3000L))
        dao.addFavorite(createFavoriteEntity(movieId = 3, title = "Third",  addedAt = 2000L))

        // When
        val result = dao.getAllFavorites().first()

        // Then — más reciente primero (ORDER BY added_at DESC)
        assertThat(result.map { it.title })
            .containsExactly("Second", "Third", "First").inOrder()
    }

    @Test
    fun whenSameFavoriteAddedTwice_replacesExistingEntry() = runTest {
        // Given — OnConflictStrategy.REPLACE
        val original = createFavoriteEntity(movieId = 1, title = "Inception")
        val updated  = createFavoriteEntity(movieId = 1, title = "Inception Remastered")

        // When
        dao.addFavorite(original)
        dao.addFavorite(updated)

        // Then — solo existe una entrada con el título actualizado
        val result = dao.getAllFavorites().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().title).isEqualTo("Inception Remastered")
    }

    // ── Tests de removeFavorite ───────────────────────────────────────────────

    @Test
    fun whenFavoriteRemoved_disappearsFromList() = runTest {
        // Given
        dao.addFavorite(createFavoriteEntity(movieId = 1))
        dao.addFavorite(createFavoriteEntity(movieId = 2, title = "Interstellar"))

        // When
        dao.removeFavorite(movieId = 1)

        // Then — solo queda el favorito con id = 2
        val result = dao.getAllFavorites().first()
        assertThat(result).hasSize(1)
        assertThat(result.first().title).isEqualTo("Interstellar")
    }

    @Test
    fun whenRemovingNonExistentFavorite_doesNotCrash() = runTest {
        // Eliminar un ID que no existe no debe lanzar excepción
        dao.removeFavorite(movieId = 999)

        val result = dao.getAllFavorites().first()
        assertThat(result).isEmpty()
    }

    // ── Tests de isFavorite ───────────────────────────────────────────────────

    @Test
    fun whenMovieIsNotFavorite_isFavoriteReturnsFalse() = runTest {
        // Given — BD vacía
        val result = dao.isFavorite(movieId = 1).first()
        assertThat(result).isFalse()
    }

    @Test
    fun whenMovieIsAdded_isFavoriteReturnsTrue() = runTest {
        // Given
        dao.addFavorite(createFavoriteEntity(movieId = 42))

        // When
        val result = dao.isFavorite(movieId = 42).first()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun whenMovieIsRemovedAfterBeingFavorite_isFavoriteReturnsFalse() = runTest {
        // Given
        dao.addFavorite(createFavoriteEntity(movieId = 42))
        assertThat(dao.isFavorite(42).first()).isTrue() // precondición

        // When
        dao.removeFavorite(movieId = 42)

        // Then
        assertThat(dao.isFavorite(42).first()).isFalse()
    }

    @Test
    fun whenCheckingDifferentMovieId_returnsFalseEvenIfOtherExists() = runTest {
        // Given — movieId 1 es favorito
        dao.addFavorite(createFavoriteEntity(movieId = 1))

        // When — verificamos movieId 2
        val result = dao.isFavorite(movieId = 2).first()

        // Then — no debe confundir IDs
        assertThat(result).isFalse()
    }

    // ── Test de reactividad ───────────────────────────────────────────────────

    @Test
    fun whenFavoriteAdded_flowEmitsUpdatedList() = runTest {
        dao.getAllFavorites().test {
            // Primera emisión — BD vacía al suscribirse
            assertThat(awaitItem()).isEmpty()

            // Escritura en BD — Room re-emite automáticamente
            dao.addFavorite(createFavoriteEntity(movieId = 1))

            // Segunda emisión — con el nuevo favorito
            val updated = awaitItem()
            assertThat(updated).hasSize(1)
            assertThat(updated.first().movieId).isEqualTo(1)
            assertThat(updated.first().title).isEqualTo("Inception")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenFavoriteAdded_isFavoriteFlowEmitsTrue() = runTest {
        dao.isFavorite(movieId = 42).test {
            // Primera emisión — no es favorita
            assertThat(awaitItem()).isFalse()

            // Agregar favorito
            dao.addFavorite(createFavoriteEntity(movieId = 42))

            // Segunda emisión — ahora sí es favorita
            assertThat(awaitItem()).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenFavoriteRemoved_isFavoriteFlowEmitsFalse() = runTest {
        // Arrancar con favorito ya existente
        dao.addFavorite(createFavoriteEntity(movieId = 42))

        dao.isFavorite(movieId = 42).test {
            // Primera emisión — es favorita
            assertThat(awaitItem()).isTrue()

            // Remover favorito
            dao.removeFavorite(movieId = 42)

            // Segunda emisión — ya no es favorita
            assertThat(awaitItem()).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }
}