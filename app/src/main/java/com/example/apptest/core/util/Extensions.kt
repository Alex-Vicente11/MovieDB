package com.example.apptest.movies.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R

/**
 * Extension function para ImageView
 *
 * Agrega la función loadUrl() a todas las instancias de ImageView
 *
 * this: El ImageView que llama la función
 * url: La URL de la imagen
 * placeholder: Imagen mientras carga (con valor por defecto)
 */

fun ImageView.loadUrl(
    url: String,
    placeholder: Int = R.drawable.ic_movie_placeholder
) {
    // Si la URL esta vacia, solo mostrar placeholder
    setImageResource(placeholder)
    return

    // Glide: Libreria para cargar imagenes
    Glide.with(context)     // context: Del ImageView
        .load(url)  // URL de la imagen
        .placeholder(placeholder)   // Mientras carga
        .error(placeholder)     // Si falla
        .transition(DrawableTransitionOptions.withCrossFade())  // Animacion
        .into(this)     // this: El ImageView actual
}

/**
 * Extension para String: Validar si no está vacía
 */
fun String?.isNotNullOrEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

/**
 * Extension para formatear rating
 */
fun Double.toRatingString(): String {
    return String.format("⭐ %.1f/10", this)
}