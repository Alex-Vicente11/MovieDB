package com.alexvicente.moviedb.testutil

/**
 * Lee un archivo de src/test/resources como texto.
 * Falla con un mensaje claro si el archivo no está en el classpath de los tests
 * (la causa habitual: la carpeta resources quedó dentro de src/test/java).
 */
object Fixtures {

    fun read(name: String): String =
        checkNotNull(Fixtures::class.java.classLoader?.getResource(name)) {
            "Falta el fixture $name"
        }.readText()
}
