package com.alexvicente.moviedb.util.genres

import android.view.View
import com.google.android.material.chip.Chip
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun isChipChecked(): Matcher<View> =
    object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description) {
            description.appendText("is a checked Chip")
        }

        override fun matchesSafely(view: View): Boolean {
            return (view as? Chip)?.isChecked == true
        }
    }