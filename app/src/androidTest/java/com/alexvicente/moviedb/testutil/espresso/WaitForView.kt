package com.alexvicente.moviedb.testutil.espresso

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 10000) {
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
        try {
            Espresso.onView(matcher).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            return
        } catch (e: Throwable) {
            Thread.sleep(100) // intervalo de polling entre intento
        }
    }
    Espresso.onView(matcher).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    // Si llegamos aquí, el timeout se agotó sin encontrar la vista
    throw AssertionError("waitForView: vista no encontrada después de ${timeoutMs}ms — $matcher")
}