package com.example.apptest

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun contextShouldNotBeNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(context).isNotNull()
    }
}