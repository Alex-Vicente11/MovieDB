package com.example.apptest.core.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R

/**
 * EXTENSIONES COMPARTIDAS
 *
 * UBICACIÓN: core/util/
 *
 * Responsabilidad:
 * - Proveer extension functions reutilizables en toda la app
 * - Simplificar código repetitivo
 *
 * Decisión de diseño:
 * ¿Por qué en core/util/ y NO en un feature específico?
 * - Estas extensiones pueden ser usadas por múltiples features
 * - ImageView.loadUrl() es útil para cualquier pantalla que cargue imágenes
 * - String y Double extensions son de propósito general
 *
 * Cambios vs versión legacy:
 * Package actualizado: movies.util → core.util
 *  Documentación mejorada
 */

/**
 * Extension function para ImageView: Cargar imagen desde URL
 *
 * Simplifica el uso de Glide para cargar imágenes
 *
 * Uso:
 * ```kotlin
 * imageView.loadUrl("https://image.tmdb.org/t/p/w500/poster.jpg")
 * ```
 *
 * @param url URL de la imagen a cargar
 * @param placeholder Recurso drawable a mostrar mientras carga y si falla
 *
 * Características:
 * - Placeholder mientras carga
 * - Imagen de error si falla
 * - Animación crossfade suave
 * - Manejo automático de URLs vacías
 */
fun ImageView.loadUrl(
    url: String,
    placeholder: Int = R.drawable.ic_movie_placeholder
) {
    // Si la URL está vacía, solo mostrar placeholder
    if (url.isEmpty()) {
        setImageResource(placeholder)
        return
    }

    // Glide: Librería para cargar imágenes de forma eficiente
    Glide.with(context)
        .load(url)
        .placeholder(placeholder)  // Mientras carga
        .error(placeholder)        // Si falla
        .transition(DrawableTransitionOptions.withCrossFade())  // Animación suave
        .into(this)
}

/**
 * Extension para String: Validar si no está vacía ni es null
 *
 * Uso:
 * ```kotlin
 * if (movieTitle.isNotNullOrEmpty()) {
 *     // hacer algo
 * }
 * ```
 */
fun String?.isNotNullOrEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

/**
 * Extension para Double: Formatear como rating de película
 *
 * Uso:
 * ```kotlin
 * val rating = 7.8
 * textView.text = rating.toRatingString()  // "⭐ 7.8/10"
 * ```
 */
fun Double.toRatingString(): String {
    return String.format("⭐ %.1f/10", this)
}