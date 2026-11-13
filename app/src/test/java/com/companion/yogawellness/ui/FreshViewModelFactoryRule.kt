package com.companion.yogawellness.ui

import androidx.lifecycle.ViewModelProvider
import org.junit.rules.ExternalResource

/**
 * Robolectric creates a new Application for every test but keeps static state for the whole
 * sandbox. `ViewModelProvider.AndroidViewModelFactory.getInstance()` caches the first Application
 * it is given in a private static field, so an AndroidViewModel created through an activity's
 * default factory (e.g. `viewModel()` → DashboardViewModel) could be wired to an *earlier* test's
 * Application and database. That made ProfileSwitchUiTest fail whenever a test rendering
 * `AppNavHost` ran before it in the same sandbox. Clearing the cache before and after each test
 * keeps every test on its own Application. (On a device there is one Application per process,
 * so the app itself is unaffected.)
 */
class FreshViewModelFactoryRule : ExternalResource() {
    override fun before() = clear()
    override fun after() = clear()

    private fun clear() {
        val field = ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
        field.isAccessible = true
        field.set(null, null)
    }
}
