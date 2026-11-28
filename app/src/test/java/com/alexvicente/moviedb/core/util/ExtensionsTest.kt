package com.alexvicente.moviedb.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

// JUnit plano: toRatingString solo ejecuta String.format. Si cargar ExtensionsKt falla
// (Stub! / NoClassDefFoundError por Glide o Snackbar), este test pasa a Robolectric.
class ExtensionsTest {

    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun whenRatingHasManyDecimals_toRatingStringRoundsToOneDecimal() {
        assertThat(8.76.toRatingString()).isEqualTo("⭐ 8.8/10")
    }

    @Test
    fun whenRatingIsZero_toRatingStringShowsZero() {
        assertThat(0.0.toRatingString()).isEqualTo("⭐ 0.0/10")
    }

    // ROJO con el código actual: con Locale alemán String.format produce "7,5".
    @Test
    fun whenDeviceUsesCommaDecimal_toRatingStringStillUsesDot() {
        Locale.setDefault(Locale.GERMANY)

        assertThat(7.5.toRatingString()).isEqualTo("⭐ 7.5/10")
    }
}