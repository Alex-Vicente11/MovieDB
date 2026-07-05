package com.alexvicente.moviedb.core.util

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.alexvicente.moviedb.R
import com.google.android.material.snackbar.Snackbar

// ── ImageView ────────────────────────────────────────────────────────────────
fun ImageView.loadUrl(
    url: String,
    placeholder: Int = R.drawable.ic_movie_placeholder
) {
    if (url.isEmpty()) { setImageResource(placeholder); return }
    Glide.with(context)
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}

// ── View ─────────────────────────────────────────────────────────────────────
fun View.showSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

fun View.showSnackbarWithAction(
    message: String,
    action: String,
    onClick: () -> Unit
) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .setAction(action) { onClick() }
        .show()
}

// ── Double ───────────────────────────────────────────────────────────────────
fun Double.toRatingString(): String = String.format("⭐ %.1f/10", this)