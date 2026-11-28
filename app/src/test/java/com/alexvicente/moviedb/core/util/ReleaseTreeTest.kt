package com.alexvicente.moviedb.core.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

// isLoggable es protected (hereda la visibilidad de Timber.Tree) y la clase es final, así que se
// prueba por la API pública de Timber: v/d/i/w/e pasan por isLoggable y solo llegan a log() si procede.
// Crashlytics se simula con mockkStatic porque getInstance() lanza sin FirebaseApp inicializado.
class ReleaseTreeTest {

    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val tree = ReleaseTree()

    @Before
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
    }

    // Si el umbral baja a DEBUG, los mensajes de depuración (que pueden llevar datos sensibles) salen a Crashlytics.
    @Test
    fun whenPriorityIsBelowWarn_nothingReachesCrashlytics() {
        tree.v("verbose message")
        tree.d("debug message")
        tree.i("info message")

        verify(exactly = 0) { FirebaseCrashlytics.getInstance() }
        verify(exactly = 0) { crashlytics.log(any()) }
    }

    @Test
    fun whenPriorityIsWarn_messageIsLoggedWithoutRecordingAnException() {
        tree.w("careful")

        verify(exactly = 1) { crashlytics.log("careful") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun whenPriorityIsError_messageIsLogged() {
        tree.e("failed")

        verify(exactly = 1) { crashlytics.log("failed") }
    }

    @Test
    fun whenThrowableIsGiven_exceptionIsRecorded() {
        val exception = IllegalStateException("boom")

        tree.e(exception, "failed")

        verify(exactly = 1) { crashlytics.recordException(exception) }
        verify(exactly = 1) { crashlytics.log(any()) }
    }
}