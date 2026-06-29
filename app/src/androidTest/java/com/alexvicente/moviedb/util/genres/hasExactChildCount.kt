package com.alexvicente.moviedb.util.genres

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

fun hasExactChildCount(expectedCount: Int): Matcher<View> =
    object : BoundedMatcher<View, ViewGroup>(ViewGroup::class.java) {

        override fun describeTo(description: Description) {
            description.appendText("has exactly $expectedCount children")
        }

        override fun matchesSafely(view: ViewGroup): Boolean {
            return view.childCount == expectedCount
        }
    }