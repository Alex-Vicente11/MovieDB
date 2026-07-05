package com.alexvicente.moviedb.testutil.extensions

import app.cash.turbine.ReceiveTurbine
import com.alexvicente.moviedb.core.data.util.Resource
import com.google.common.truth.Truth.assertWithMessage

suspend fun <T> ReceiveTurbine<Resource<T>>.awaitLoading() {
    val item = awaitItem()
    assertWithMessage("Se esperaba Resource.Loading pero fue ${item::class.simpleName}")
        .that(item)
        .isInstanceOf(Resource.Loading::class.java)
}

suspend fun <T> ReceiveTurbine<Resource<T>>.awaitSuccess(
    validate: ((T) -> Unit)? = null
) {
    val item = awaitItem()
    assertWithMessage("Se esperaba Resource.Success pero fue ${item::class.simpleName}")
        .that(item)
        .isInstanceOf(Resource.Success::class.java)

    // Ejecuta validaciones adicionales sobre los datos si se proveyeron
    val data = (item as Resource.Success).data
    validate?.invoke(data)
}

suspend fun <T> ReceiveTurbine<Resource<T>>.awaitError(
    validate: ((String) -> Unit)? = null
) {
    val item = awaitItem()
    assertWithMessage("Se esperaba Resource.Error pero fue ${item::class.simpleName}")
        .that(item)
        .isInstanceOf(Resource.Error::class.java)

    val message = (item as Resource.Error).message
    validate?.invoke(message)
}