package com.example.apptest.testutil.extensions

import app.cash.turbine.ReceiveTurbine
import com.example.apptest.core.data.util.Resource
import com.google.common.truth.Truth.assertWithMessage

/**
 * Responsabilidades: Extensiones de Turbine para reducir boilerplate en tests de Flow
 *
 * Sin estas extensiones, cada test repite el mismo patrón:
 *
 *   val item = awaitItem()
 *   assertThat(item).isInstanceOf(Resource.Loading::class.java)
 *
 *   val item = awaitItem()
 *   assertThat(item).isInstanceOf(Resource.Success::class.java)
 *   assertThat((item as Resource.Success).data).isNotEmpty()
 *
 * Con las extensiones, el test queda más declarativo y legible:
 *
 *   awaitLoading()
 *   awaitSuccess() { assertThat(it).isNotEmpty() }
 *   awaitError { assertThat(it).contains("conexión") }
 *
 * Patrón aplicado: Extension Functions como DSL ligero
 * Extienden ReceiveTurbine<Resource<T>> para agregar semántica de dominio.
 */

/**
 * Espera la siguiente emisión y verifica que es Resource.Loading
 * Falla el test si la emisión es de otro tipo
 *
 * Uso:
 *   repository.getPopularMovies().test {
 *      awaitLoading()
 *      ...
 *  }
 */
suspend fun <T> ReceiveTurbine<Resource<T>>.awaitLoading() {
    val item = awaitItem()
    assertWithMessage("Se esperaba Resource.Loading pero fue ${item::class.simpleName}")
        .that(item)
        .isInstanceOf(Resource.Loading::class.java)
}

/**
 * Espera la siguiente emisión, verifica que es Resource.Success
 * y ejecuta el bloque de validación con los datos.
 *
 * Uso: awaitSuccess { movies ->
 *         assertThat(movies).hasSize(3)
 *         assertThat(movies.first().title).isEqualTo("Inception")
 *      }
 */
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

/**
 * Espera la siguiente emisión, verifica que es Resource.Error
 * y ejecuta el bloque de validación con el mensaje de error.
 *
 * Uso: awaitError { message ->
 *         assertThat(message).isEqualTo("Error de conexión. Verifica tu internet.")
 *      }
 */
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