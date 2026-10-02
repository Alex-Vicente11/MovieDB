package com.alexvicente.moviedb.core.data.util

/**
 * Se lanza cuando una respuesta GraphQL no trae errores de red/transporte,
 * pero el servidor reportó errores en el nivel de resolución (response.errors)
 * y no hay datos utilizables (response.data == null).
 */

class GraphQLDataException(message: String): Exception(message)