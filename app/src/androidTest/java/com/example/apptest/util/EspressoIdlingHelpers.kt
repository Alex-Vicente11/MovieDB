package com.example.apptest.util

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher

fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 10000) {
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
        try {
            onView(matcher).check(matches(isDisplayed()))
            return
        } catch (e: Throwable) {
            Thread.sleep(100) // intervalo de polling entre intento
        }
    }
    onView(matcher).check(matches(isDisplayed()))
    // Si llegamos aquí, el timeout se agotó sin encontrar la vista
    throw AssertionError("waitForView: vista no encontrada después de ${timeoutMs}ms — $matcher")
}