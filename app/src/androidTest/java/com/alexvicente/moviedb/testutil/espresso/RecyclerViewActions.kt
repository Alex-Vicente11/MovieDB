package com.alexvicente.moviedb.testutil.espresso

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

fun clickChildViewWithId(id: Int): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> =
            ViewMatchers.isEnabled()

        override fun getDescription() = "Click on child view with id $id"

        override fun perform(uiController: UiController, view: View) {
            val child = view.findViewById<View>(id)
                ?: throw IllegalStateException(
                    "View with id $id not found in ViewHolder"
                )
            child.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }
}