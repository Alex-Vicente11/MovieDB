package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.ApolloMovieMapper.toDomain
import com.alexvicente.moviedb.graphql.PopularMoviesQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApolloMovieMapperTest {

    // Constructor verificado con PopularMoviesRepositoryImplTest y schema.graphqls:
    // posterPath es String! (no nulo) y backdropPath es String? (nulo).
    private fun createApolloMovie(
        id: Int = 1,
        title: String = "Inception",
        overview: String = "A thief who steals corporate secrets",
        posterPath: String = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
        releaseDate: String = "2010-07-16",
        voteAverage: Double = 8.8,
        voteCount: Int = 30000,
        popularity: Double = 100.0
    ) = PopularMoviesQuery.PopularMovie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        popularity = popularity,
        genres = emptyList()
    )

    // -- PopularMovie.toDomain() ---------------------------------------------------

    @Test
    fun whenAllFieldsArePresent_mapsEveryField() {
        // Given
        val apollo = createApolloMovie(
            id = 7, title = "Avengers", overview = "Earth's mightiest heroes",
            posterPath = "/a.jpg", backdropPath = "/b.jpg", releaseDate = "2012-05-04",
            voteAverage = 8.0, voteCount = 10000, popularity = 99.5
        )

        // When
        val movie = apollo.toDomain()

        // Then
        assertThat(movie.id).isEqualTo(7)
        assertThat(movie.title).isEqualTo("Avengers")
        assertThat(movie.overview).isEqualTo("Earth's mightiest heroes")
        assertThat(movie.posterPath).isEqualTo("/a.jpg")
        assertThat(movie.backdropPath).isEqualTo("/b.jpg")
        assertThat(movie.releaseDate).isEqualTo("2012-05-04")
        assertThat(movie.voteAverage).isEqualTo(8.0)
        assertThat(movie.voteCount).isEqualTo(10000)
        assertThat(movie.popularity).isEqualTo(99.5)
    }

    @Test
    fun whenTitleHasSurroundingSpaces_titleIsTrimmed() {
        assertThat(createApolloMovie(title = "  Batman  ").toDomain().title).isEqualTo("Batman")
    }

    @Test
    fun whenOverviewHasSurroundingSpaces_overviewIsTrimmed() {
        assertThat(createApolloMovie(overview = "  Great movie  ").toDomain().overview)
            .isEqualTo("Great movie")
    }

    @Test
    fun whenOverviewIsEmpty_usesDefaultMessage() {
        assertThat(createApolloMovie(overview = "").toDomain().overview)
            .isEqualTo("Sin descripción disponible")
    }

    @Test
    fun whenOverviewIsOnlySpaces_usesDefaultMessage() {
        assertThat(createApolloMovie(overview = "   ").toDomain().overview)
            .isEqualTo("Sin descripción disponible")
    }

    @Test
    fun whenReleaseDateHasSurroundingSpaces_releaseDateIsTrimmed() {
        assertThat(createApolloMovie(releaseDate = "  2024-01-01  ").toDomain().releaseDate)
            .isEqualTo("2024-01-01")
    }

    @Test
    fun whenBackdropPathIsNull_backdropPathIsNull() {
        assertThat(createApolloMovie(backdropPath = null).toDomain().backdropPath).isNull()
    }

    // Caracterización: a diferencia de MovieMapper (REST), Apollo NO recorta las rutas de imagen.
    // Si se decide unificar el comportamiento, estos dos tests son los que cambian.
    @Test
    fun whenPosterPathHasSpaces_posterPathIsNotTrimmed() {
        assertThat(createApolloMovie(posterPath = "  /poster.jpg  ").toDomain().posterPath)
            .isEqualTo("  /poster.jpg  ")
    }

    @Test
    fun whenBackdropPathHasSpaces_backdropPathIsNotTrimmed() {
        assertThat(createApolloMovie(backdropPath = "  /backdrop.jpg  ").toDomain().backdropPath)
            .isEqualTo("  /backdrop.jpg  ")
    }

    // Caracterización: el schema declara posterPath como String!, así que "sin póster" no puede
    // llegar como null; si el proxy manda "", el dominio conserva "" (no null).
    @Test
    fun whenPosterPathIsEmpty_posterPathStaysEmptyNotNull() {
        assertThat(createApolloMovie(posterPath = "").toDomain().posterPath).isEmpty()
    }

    // -- List<PopularMovie>.toDomain() -----------------------------------------------

    @Test
    fun whenListHasSeveralMovies_preservesSizeAndOrder() {
        val list = listOf(
            createApolloMovie(id = 3, title = "C"),
            createApolloMovie(id = 1, title = "A"),
            createApolloMovie(id = 2, title = "B")
        )

        val movies = list.toDomain()

        assertThat(movies.map { it.id }).containsExactly(3, 1, 2).inOrder()
        assertThat(movies.map { it.title }).containsExactly("C", "A", "B").inOrder()
    }

    @Test
    fun whenListIsEmpty_returnsEmptyList() {
        assertThat(emptyList<PopularMoviesQuery.PopularMovie>().toDomain()).isEmpty()
    }
}
