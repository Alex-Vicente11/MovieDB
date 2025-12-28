package com.example.apptest.data.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R

class MovieDetailsActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_MOVIE_ID = "extra_movie_id"
        const val EXTRA_MOVIE_TITLE = "extra_movie_title"
        const val EXTRA_MOVIE_OVERVIEW = "extra_movie_overview"
        const val EXTRA_MOVIE_RATING = "extra_movie_rating"
        const val EXTRA_MOVIE_VOTES = "extra_movie_votes"
        const val EXTRA_MOVIE_RELEASE = "extra_movie_release"
        const val EXTRA_MOVIE_POSTER = "extra_movie_poster"
        const val EXTRA_MOVIE_BACKDROP = "extra_movie_backdrop"
        const val EXTRA_MOVIE_POPULARITY = "extra_movie_popularity"
    }

    private lateinit var ivBackdrop: ImageView
    private lateinit var ivPoster: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvVotes: TextView
    private lateinit var tvRelease: TextView
    private lateinit var tvPopularity: TextView
    private lateinit var tvOverview: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        // Inicializar vistas
        initViews()

        // Obtener datos del intent
        loadMovieData()
    }

    private fun initViews() {
        // findViewById: Buscar las vistas por ID
        ivBackdrop = findViewById(R.id.ivBackdrop)
        ivPoster = findViewById(R.id.ivPosterDetail)
        tvTitle = findViewById(R.id.tvTitleDetail)
        tvRating = findViewById(R.id.tvRatingDetail)
        tvVotes = findViewById(R.id.tvVotesDetail)
        tvRelease = findViewById(R.id.tvReleaseDetail)
        tvPopularity = findViewById(R.id.tvPopularityDetail)
        tvOverview = findViewById(R.id.tvOverviewDetail)
        btnBack = findViewById(R.id.btnBack)

        // Click listener del boton regresar
        btnBack.setOnClickListener {
            // finish(): Cierra esta activity y regresa a la anterior
            finish()
        }
    }

    private fun loadMovieData() {
        // intent: El intent que inicio esta activity
        // getStringExtra(): Obtiene un String del intent
        val title = intent.getStringExtra(EXTRA_MOVIE_TITLE) ?: "Sin título"
        val overview = intent.getStringExtra(EXTRA_MOVIE_OVERVIEW) ?: "Sin descripción"

        // .getDoubleExtra(): Obtiene un Double del intent
        // El segundo parametro es el valor por defecto
        val rating = intent.getDoubleExtra(EXTRA_MOVIE_RATING, 0.0)
        val popularity = intent.getDoubleExtra(EXTRA_MOVIE_POPULARITY, 0.0)

        // .getIntExtra(): Obtiene un Int del intent
        val votes = intent.getIntExtra(EXTRA_MOVIE_VOTES, 0)

        val release = intent.getStringExtra(EXTRA_MOVIE_RELEASE) ?: "N/A"
        val posterUrl = intent.getStringExtra(EXTRA_MOVIE_POSTER) ?: ""
        val backdropUrl = intent.getStringExtra(EXTRA_MOVIE_BACKDROP) ?: ""

        // Establecer textos
        tvTitle.text = title
        tvOverview.text = overview

        // String.format: Formatea un string con parametros
        // $.1f: float con 1 decimal
        tvRating.text = String.format(" ⭐ %.1f/10", rating)
        tvVotes.text = "$votes votos"
        tvRelease.text = "$release"
        tvPopularity.text = String.format("🔥 %.1f", popularity)

        // Cargar backdrop (imagen horizontal grande)
        if (backdropUrl.isNotEmpty()) {
            Glide.with(this)
                .load(backdropUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBackdrop)
        }

        // Cargar poster (imagen vertical)
        if (posterUrl.isNotEmpty()) {
            Glide.with(this)
                .load(posterUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivPoster)
        }
    }
}