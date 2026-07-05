package com.alexvicente.moviedb.util

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import debug.java.com.alexvicente.apptest.HiltTestActivity

inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    navHostController: TestNavHostController? = null,
    fragmentFactory: FragmentFactory? = null,
    crossinline action: T.() -> Unit = {}
) {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    val scenario = ActivityScenario.launch<HiltTestActivity>(startActivityIntent)

    scenario.onActivity { activity ->

        fragmentFactory?.let {
            activity.supportFragmentManager.fragmentFactory = it
        }

        val fragment = activity.supportFragmentManager.fragmentFactory
            .instantiate(
                checkNotNull(T::class.java.classLoader),
                T::class.java.name
            )
        fragment.arguments = fragmentArgs

        navHostController?.let { navController ->
            lateinit var observer: Observer<LifecycleOwner?>
            observer = Observer { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    Navigation.setViewNavController(fragment.requireView(), navController)
                    fragment.viewLifecycleOwnerLiveData.removeObserver(observer)
                }
            }
            fragment.viewLifecycleOwnerLiveData.observeForever(observer)
        }

        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

        (fragment as T).action()
    }
}
