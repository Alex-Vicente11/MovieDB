package com.alexvicente.moviedb.core.data.local.mapper

import com.alexvicente.moviedb.core.data.local.entity.FavoriteEntity
import com.alexvicente.moviedb.core.data.local.entity.MovieDetailsEntity
import com.alexvicente.moviedb.core.data.local.entity.MovieEntity
import com.alexvicente.moviedb.core.data.local.entity.VideoEntity
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.testutil.factories.MovieDetailsFactory
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.testutil.factories.VideoFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovieEntityMapperTest {

    // ═══ Movie <-> MovieEntity ═══════════════════════════════════════════════════════

    @Test
    fun whenMovieEntityIsMapped_everyDomainFieldIsKept() {
        val entity = MovieEntity(
            id = 5, title = "Heat", overview = "Cops and robbers", posterPath = "/p.jpg",
            backdropPath = null, voteAverage = 8.3, voteCount = 7000, releaseDate = "1995-12-15",
            popularity = 70.5, isPopular = true, cachedAt = 123L
        )

        val movie = entity.toDomain()

        assertThat(movie.id).isEqualTo(5)
        assertThat(movie.title).isEqualTo("Heat")
        assertThat(movie.overview).isEqualTo("Cops and robbers")
        assertThat(movie.posterPath).isEqualTo("/p.jpg")
        assertThat(movie.backdropPath).isNull()
        assertThat(movie.voteAverage).isEqualTo(8.3)
        assertThat(movie.voteCount).isEqualTo(7000)
        assertThat(movie.releaseDate).isEqualTo("1995-12-15")
        assertThat(movie.popularity).isEqualTo(70.5)
    }

    @Test
    fun whenMovieIsMappedToEntity_everyFieldIsKept() {
        val movie = MovieFactory.createMovie(
            id = 9, title = "Alien", overview = "In space", posterPath = "/a.jpg",
            backdropPath = "/b.jpg", voteAverage = 8.1, voteCount = 12000,
            releaseDate = "1979-05-25", popularity = 55.0
        )

        val entity = movie.toEntity()

        assertThat(entity.id).isEqualTo(9)
        assertThat(entity.title).isEqualTo("Alien")
        assertThat(entity.overview).isEqualTo("In space")
        assertThat(entity.posterPath).isEqualTo("/a.jpg")
        assertThat(entity.backdropPath).isEqualTo("/b.jpg")
        assertThat(entity.voteAverage).isEqualTo(8.1)
        assertThat(entity.voteCount).isEqualTo(12000)
        assertThat(entity.releaseDate).isEqualTo("1979-05-25")
        assertThat(entity.popularity).isEqualTo(55.0)
    }

    @Test
    fun whenMovieIsMappedWithoutFlag_isPopularIsFalse() {
        assertThat(MovieFactory.createMovie().toEntity().isPopular).isFalse()
    }

    @Test
    fun whenMovieIsMappedAsPopular_isPopularIsTrue() {
        assertThat(MovieFactory.createMovie().toEntity(isPopular = true).isPopular).isTrue()
    }

    @Test
    fun whenMovieIsMappedToEntity_cachedAtIsTheCurrentTime() {
        val before = System.currentTimeMillis()

        val entity = MovieFactory.createMovie().toEntity()

        assertThat(entity.cachedAt).isAtLeast(before)
        assertThat(entity.cachedAt).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun whenMovieListIsMapped_flagAndOrderAreKept() {
        val movies = MovieFactory.createMovieList(3)

        val entities = movies.toEntity(isPopular = true)

        assertThat(entities.map { it.id }).containsExactly(1, 2, 3).inOrder()
        assertThat(entities.all { it.isPopular }).isTrue()
    }

    @Test
    fun whenMovieListIsEmpty_returnsEmptyEntityList() {
        assertThat(emptyList<Movie>().toEntity()).isEmpty()
    }

    @Test
    fun whenMovieWithNullImagesIsRoundTripped_domainIsEqual() {
        val movie = MovieFactory.createMovie(posterPath = null, backdropPath = null)

        assertThat(movie.toEntity().toDomain()).isEqualTo(movie)
    }

    // ═══ MovieDetails <-> MovieDetailsEntity ═════════════════════════════════════════

    private fun detailsEntity(genresJson: String) = MovieDetailsEntity(
        id = 1, title = "Inception", overview = "Dreams", posterPath = "/p.jpg",
        backdropPath = "/b.jpg", voteAverage = 8.8, voteCount = 30000, releaseDate = "2010-07-16",
        popularity = 100.0, runtime = 148, budget = 160_000_000L, revenue = 829_895_144L,
        genresJson = genresJson, tagline = "Your mind is the scene of the crime", cachedAt = 1L
    )

    // Contrato con lo que ya hay guardado en Room: si cambia el formato del JSON, esto avisa.
    @Test
    fun whenGenresJsonIsStoredFormat_genresAreParsed() {
        val details = detailsEntity("""[{"id":28,"name":"Action"},{"id":878,"name":"Science Fiction"}]""").toDomain()

        assertThat(details.genres).containsExactly(Genre(28, "Action"), Genre(878, "Science Fiction")).inOrder()
    }

    @Test
    fun whenGenresJsonIsEmptyArray_genresAreEmpty() {
        assertThat(detailsEntity("[]").toDomain().genres).isEmpty()
    }

    @Test
    fun whenGenresJsonIsNullLiteral_genresAreEmpty() {
        assertThat(detailsEntity("null").toDomain().genres).isEmpty()
    }

    @Test
    fun whenDetailsEntityIsMapped_scalarFieldsAreKept() {
        val details = detailsEntity("[]").toDomain()

        assertThat(details.id).isEqualTo(1)
        assertThat(details.title).isEqualTo("Inception")
        assertThat(details.overview).isEqualTo("Dreams")
        assertThat(details.posterPath).isEqualTo("/p.jpg")
        assertThat(details.backdropPath).isEqualTo("/b.jpg")
        assertThat(details.voteAverage).isEqualTo(8.8)
        assertThat(details.voteCount).isEqualTo(30000)
        assertThat(details.releaseDate).isEqualTo("2010-07-16")
        assertThat(details.popularity).isEqualTo(100.0)
        assertThat(details.runtime).isEqualTo(148)
        assertThat(details.budget).isEqualTo(160_000_000L)
        assertThat(details.revenue).isEqualTo(829_895_144L)
        assertThat(details.tagline).isEqualTo("Your mind is the scene of the crime")
    }

    @Test
    fun whenDetailsAreMappedToEntity_scalarFieldsAreKept() {
        val entity = MovieDetailsFactory.createMovieDetails(
            id = 3, title = "Dune", runtime = null, budget = null, revenue = null, tagline = null
        ).toEntity()

        assertThat(entity.id).isEqualTo(3)
        assertThat(entity.title).isEqualTo("Dune")
        assertThat(entity.runtime).isNull()
        assertThat(entity.budget).isNull()
        assertThat(entity.revenue).isNull()
        assertThat(entity.tagline).isNull()
    }

    @Test
    fun whenDetailsHaveNoGenres_genresJsonIsEmptyArray() {
        assertThat(MovieDetailsFactory.createMovieDetails(genres = emptyList()).toEntity().genresJson)
            .isEqualTo("[]")
    }

    @Test
    fun whenDetailsAreMappedToEntity_cachedAtIsTheCurrentTime() {
        val before = System.currentTimeMillis()

        val entity = MovieDetailsFactory.createMovieDetails().toEntity()

        assertThat(entity.cachedAt).isAtLeast(before)
        assertThat(entity.cachedAt).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun whenDetailsAreRoundTripped_domainIsEqual() {
        val details = MovieDetailsFactory.createMovieDetails()

        assertThat(details.toEntity().toDomain()).isEqualTo(details)
    }

    // ═══ Video <-> VideoEntity ═══════════════════════════════════════════════════════

    @Test
    fun whenVideoEntityIsMapped_languageAndCountryComeFromIsoColumns() {
        val entity = VideoEntity(
            id = "v1", movieId = 10, name = "Trailer", key = "k", site = "YouTube", type = "Trailer",
            official = true, publishedAt = "2025-02-14T16:00:07.000Z", size = 1080,
            iso6391 = "es", iso31661 = "MX", cachedAt = 1L
        )

        val video = entity.toDomain()

        assertThat(video.language).isEqualTo("es")
        assertThat(video.country).isEqualTo("MX")
        assertThat(video.id).isEqualTo("v1")
        assertThat(video.key).isEqualTo("k")
        assertThat(video.size).isEqualTo(1080)
        assertThat(video.official).isTrue()
    }

    // Si se intercambian iso6391 e iso31661 en toEntity, este test falla.
    @Test
    fun whenVideoIsMappedToEntity_isoColumnsComeFromLanguageAndCountry() {
        val entity = VideoFactory.createVideo(language = "es", country = "MX").toEntity(movieId = 10)

        assertThat(entity.iso6391).isEqualTo("es")
        assertThat(entity.iso31661).isEqualTo("MX")
    }

    @Test
    fun whenVideoIsMappedToEntity_movieIdIsAssigned() {
        assertThat(VideoFactory.createVideo().toEntity(movieId = 77).movieId).isEqualTo(77)
    }

    @Test
    fun whenVideoIsMappedToEntity_cachedAtIsTheCurrentTime() {
        val before = System.currentTimeMillis()

        val entity = VideoFactory.createVideo().toEntity(movieId = 1)

        assertThat(entity.cachedAt).isAtLeast(before)
        assertThat(entity.cachedAt).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun whenVideoListIsMapped_everyEntityGetsTheMovieId() {
        val entities = VideoFactory.createVideoList(3).toEntity(movieId = 42)

        assertThat(entities.map { it.id }).containsExactly("video-1", "video-2", "video-3").inOrder()
        assertThat(entities.all { it.movieId == 42 }).isTrue()
    }

    @Test
    fun whenVideoIsRoundTripped_domainIsEqual() {
        val video = VideoFactory.createVideo(language = "es", country = "MX")

        assertThat(video.toEntity(movieId = 1).toDomain()).isEqualTo(video)
    }

    // ═══ Favorite <-> FavoriteEntity ═════════════════════════════════════════════════

    @Test
    fun whenFavoriteEntityIsMapped_movieIdBecomesId() {
        val entity = FavoriteEntity(
            movieId = 8, title = "Se7en", posterPath = null, voteAverage = 8.4,
            releaseDate = "1995-09-22", overview = "Seven sins", addedAt = 999L
        )

        val favorite = entity.toDomain()

        assertThat(favorite.id).isEqualTo(8)
        assertThat(favorite.title).isEqualTo("Se7en")
        assertThat(favorite.posterPath).isNull()
        assertThat(favorite.voteAverage).isEqualTo(8.4)
        assertThat(favorite.releaseDate).isEqualTo("1995-09-22")
        assertThat(favorite.overview).isEqualTo("Seven sins")
        assertThat(favorite.addedAt).isEqualTo(999L)
    }

    @Test
    fun whenMovieIsMappedToFavoriteEntity_idBecomesMovieIdAndFavoriteFieldsAreKept() {
        val movie = MovieFactory.createMovie(
            id = 12, title = "Heat", posterPath = "/h.jpg", voteAverage = 8.3,
            releaseDate = "1995-12-15", overview = "Cops and robbers"
        )

        val entity = movie.toFavoriteEntity()

        assertThat(entity.movieId).isEqualTo(12)
        assertThat(entity.title).isEqualTo("Heat")
        assertThat(entity.posterPath).isEqualTo("/h.jpg")
        assertThat(entity.voteAverage).isEqualTo(8.3)
        assertThat(entity.releaseDate).isEqualTo("1995-12-15")
        assertThat(entity.overview).isEqualTo("Cops and robbers")
    }

    @Test
    fun whenMovieIsMappedToFavoriteEntity_addedAtIsTheCurrentTime() {
        val before = System.currentTimeMillis()

        val entity = MovieFactory.createMovie().toFavoriteEntity()

        assertThat(entity.addedAt).isAtLeast(before)
        assertThat(entity.addedAt).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun whenMovieBecomesFavoriteAndBack_favoriteKeepsTheSharedFields() {
        val movie = MovieFactory.createMovie(id = 4, title = "Ronin", posterPath = null)

        val favorite = movie.toFavoriteEntity().toDomain()

        assertThat(favorite.id).isEqualTo(movie.id)
        assertThat(favorite.title).isEqualTo(movie.title)
        assertThat(favorite.posterPath).isNull()
        assertThat(favorite.voteAverage).isEqualTo(movie.voteAverage)
        assertThat(favorite.releaseDate).isEqualTo(movie.releaseDate)
        assertThat(favorite.overview).isEqualTo(movie.overview)
    }
}