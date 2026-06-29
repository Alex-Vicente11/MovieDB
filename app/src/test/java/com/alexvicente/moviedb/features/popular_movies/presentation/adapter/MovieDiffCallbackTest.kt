package com.alexvicente.moviedb.features.popular_movies.presentation.adapter

import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * ¿Por qué testear DiffCallback?
 *   DiffUtil decide qué items se redibujan, animan o actualizan en el RecyclerView.
 *   Un bug aquí causa:
 *     - Flickering: items que se redibujan innecesariamente
 *     - Animaciones incorrectas: ítems que no se animan cuando deberían
 *     - Payload incorrecto: campos que no se actualizan en bind parcial
 *
 * No necesita Android framework - es lógica pura kotlin
 * Corre en JVM con JUnit + Truth sin emulador
 */

class MovieDiffCallbackTest {

    // Película base reutilizada en múltiples tests
    private lateinit var baseMovie: Movie

    @Before
    fun setUp() {
        baseMovie = MovieFactory.createMovie(
            id = 1,
            title = "Inception",
            voteAverage = 8.8,
            overview = "A thief who steals corporate secrets",
            posterPath = "/poster.jpg"
        )
    }

    // HELPERS
    /**
     * Crea un DiffCallback con listas de un solo elemento cada una.
     * Simplifica los tests que solo necesitan comparar dos películas
     */

    private fun callbackOf(
        old: Movie,
        new: Movie
    ) = MovieDiffCallback(listOf(old), listOf(new))

    // areItemsTheShame - IDENTIDAD POR ID

    @Test
    fun whenBothMoviesHaveSameId_itemsAreTheSame() {
        // Mismo ID aunque todos los demás campos difieran
        val old = MovieFactory.createMovie(id = 1, title = "Inception")
        val new = MovieFactory.createMovie(id = 1, title = "Inception Remastered")

        val callback = callbackOf(old, new)


        // areItemsTheSame determina si es el mismo item lógico (mismo ID = mismo item)
        assertThat(callback.areItemsTheSame(0,0)).isTrue()
    }

    @Test
    fun whenMoviesHaveDifferentIds_itemsAreNotTheSame() {
        // IDs distintos = items distintos, DiffUtil los trata como inserción + eliminación
        val old = MovieFactory.createMovie(id = 1, title = "Inception")
        val new = MovieFactory.createMovie(id = 2, title = "Inception")

        val callback = callbackOf(old, new)

        assertThat(callback.areItemsTheSame(0,0)).isFalse()
    }

    // areContentsTheSame - IGUALDAD COMPLETA

    @Test
    fun whenMoviesAreIdentical_contentsAreTheSame() {
        // Misma instancia lógica - no debe generar ninguna actualización
        val callback = callbackOf(baseMovie, baseMovie.copy())

        assertThat(callback.areContentsTheSame(0,0)).isTrue()
    }

    @Test
    fun whenTitleChanges_contentsAreNotTheSame() {
        val updated = baseMovie.copy(title = "Inception: Director's Cut")
        val callback = callbackOf(baseMovie, updated)

        // Cambio de título debe disparar rebind del item
        assertThat(callback.areContentsTheSame(0,0)).isFalse()
    }

    @Test
    fun whenVoteAverageChanges_contentsAreNotTheSame() {
        val updated = baseMovie.copy(voteAverage = 9.5)
        val callback = callbackOf(baseMovie, updated)

        assertThat(callback.areContentsTheSame(0,0)).isFalse()
    }

    @Test
    fun whenOverviewChanges_contentsAreNotTheSame() {
        val updated = baseMovie.copy(overview = "Updated overview")
        val callback = callbackOf(baseMovie, updated)

        assertThat(callback.areContentsTheSame(0,0)).isFalse()
    }

    @Test
    fun whenPosterPathChanges_contentsAreNotTheSame() {
        val updated = baseMovie.copy(posterPath = "/new_poster.jpg")
        val callback = callbackOf(baseMovie, updated)

        assertThat(callback.areContentsTheSame(0,0)).isFalse()
    }

    @Test
    fun whenPosterPathChangesFromNullToValue_contentsAreNotTheSame() {
        // Caso específico: película sin poster que ahora tiene uno
        val withoutPoster = baseMovie.copy(posterPath = null)
        val withPoster = baseMovie.copy(posterPath = "/poster.jpg")
        val callback = callbackOf(withoutPoster, withPoster)

        assertThat(callback.areContentsTheSame(0,0)).isFalse()
    }

    // getChangePayload - DETECCIÓN DE CAMPOS ESPECÍFICOS

    @Test
    fun whenNothingChanges_allPayloadFieldsAreFalse() {
        // Sin cambios no debe haber payload activo - evita rebind innecesario
        val callback = callbackOf(baseMovie, baseMovie.copy())
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        assertThat(payload!!.titleChanged).isFalse()
        assertThat(payload.ratingChanged).isFalse()
        assertThat(payload.overviewChanged).isFalse()
        assertThat(payload.posterChanged).isFalse()
    }

    @Test
    fun whenOnlyTitleChanges_onlyTitleChangedIsTrue() {
        val updated = baseMovie.copy(title = "Inception: Director's Cut")
        val callback = callbackOf(baseMovie, updated)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        // Solo titleChanged debe ser true - los demás campos no cambiaron
        assertThat(payload!!.titleChanged).isTrue()
        assertThat(payload.ratingChanged).isFalse()
        assertThat(payload.overviewChanged).isFalse()
        assertThat(payload.posterChanged).isFalse()
    }

    @Test
    fun whenOnlyRatingChanges_onlyRatingChangedIsTrue() {
        val updated = baseMovie.copy(voteAverage = 9.5)
        val callback = callbackOf(baseMovie, updated)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        assertThat(payload!!.titleChanged).isFalse()
        assertThat(payload.ratingChanged).isTrue()
        assertThat(payload.overviewChanged).isFalse()
        assertThat(payload.posterChanged).isFalse()
    }

    @Test
    fun whenOnlyOverviewChanges_onlyOverviewChangedIsTrue() {
        val updated = baseMovie.copy(overview = "New overview")
        val callback = callbackOf(baseMovie, updated)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        assertThat(payload!!.titleChanged).isFalse()
        assertThat(payload.ratingChanged).isFalse()
        assertThat(payload.overviewChanged).isTrue()
        assertThat(payload.posterChanged).isFalse()
    }

    @Test
    fun whenOnlyPosterChanges_onlyPosterChangedIsTrue() {
        val updated = baseMovie.copy(posterPath = "/new_poster.jpg")
        val callback = callbackOf(baseMovie, updated)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        assertThat(payload!!.titleChanged).isFalse()
        assertThat(payload.ratingChanged).isFalse()
        assertThat(payload.overviewChanged).isFalse()
        assertThat(payload.posterChanged).isTrue()
    }

    @Test
    fun whenMultipleFieldsChange_allChangedFieldsAreTrue() {
        // Escenario realista: actualización de datos desde la API
        // cambia rating, overview y poster simultáneamente
        val updated = baseMovie.copy(
            voteAverage = 9.0,
            overview = "Updated overview",
            posterPath = "/updated_poster.jpg"
        )

        val callback = callbackOf(baseMovie, updated)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload).isNotNull()
        assertThat(payload!!.titleChanged).isFalse() // título no cambió
        assertThat(payload.ratingChanged).isTrue()
        assertThat(payload.overviewChanged).isTrue()
        assertThat(payload.posterChanged).isTrue()
    }

    // TAMAÑOS DE LISTA
    @Test
    fun oldListSize_matchesConstructorInput() {
        val oldList = MovieFactory.createMovieList(5)
        val newList = MovieFactory.createMovieList(3)
        val callback = MovieDiffCallback(oldList, newList)

        assertThat(callback.getOldListSize()).isEqualTo(5)
    }

    @Test
    fun newListSize_matchesConstructorInput() {
        val oldList = MovieFactory.createMovieList(5)
        val newList = MovieFactory.createMovieList(3)
        val callback = MovieDiffCallback(oldList, newList)

        assertThat(callback.getNewListSize()).isEqualTo(3)
    }

    @Test
    fun whenBothListsAreEmpty_sizesAreZero() {
        val callback = MovieDiffCallback(emptyList(), emptyList())

        assertThat(callback.getOldListSize()).isEqualTo(0)
        assertThat(callback.getNewListSize()).isEqualTo(0)
    }

    // EDGE CASES

    @Test
    fun whenMovieHasNullPosterAndNewHasNull_posterNotChanged() {
        // Ambos null - no debe marcar poster como cambiado
        val old = baseMovie.copy(posterPath = null)
        val new = baseMovie.copy(posterPath = null)
        val callback = callbackOf(old, new)
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload

        assertThat(payload!!.posterChanged).isFalse()
    }

    @Test
    fun whenSameIdButAllFieldsChange_itemsAreTheSameButContentAreNot() {
        // Escenario: misma película con todos los datos actualizados
        // DiffUtil debe reconocerla como el mismo item (mismo ID)
        // pero disparar un rebind completo (contenido diferente)
        val fullyUpdated = MovieFactory.createMovie(
            id = baseMovie.id,
            title = "Nuevo título",
            voteAverage = 1.0,
            overview = "Nuevo overview",
            posterPath = "/nuevo.jpg"
        )

        val callback = callbackOf(baseMovie, fullyUpdated)

        // Mismo item lógico
        assertThat(callback.areItemsTheSame(0,0)).isTrue()
        // Pero contenido completamente diferente
        assertThat(callback.areContentsTheSame(0,0)).isFalse()

        // Y el payload refleja todos los cambios
        val payload = callback.getChangePayload(0,0) as? MovieChangePayload
        assertThat(payload!!.titleChanged).isTrue()
        assertThat(payload.ratingChanged).isTrue()
        assertThat(payload.overviewChanged).isTrue()
        assertThat(payload.posterChanged).isTrue()
    }
}