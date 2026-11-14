package com.galla.app

import android.content.Context
import com.galla.app.core.state.CartStore
import com.galla.app.data.local.db.GallaDatabase
import com.galla.app.data.local.prefs.SettingsStore
import com.galla.app.data.repo.BackupRepository
import com.galla.app.data.repo.SampleData
import com.galla.app.data.repo.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app's parts list, built once when the app starts ("manual dependency injection").
 * Everything that needs something else gets it handed in here, in one readable place.
 *
 * Note what is NOT here: no network client and no login. Galla has no server.
 */
class AppContainer(context: Context) {

    /** The current time. Passed around as a function so tests and sample data can choose their own. */
    val now: () -> Long = { System.currentTimeMillis() }

    /** Work that should outlive any single screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = GallaDatabase.create(context)

    val settings = SettingsStore(context)
    val shop = ShopRepository(database, now)
    val backup = BackupRepository(database, settings, now)
    val sample = SampleData(shop, now)

    /** The bill being built at the counter. */
    val cart = CartStore()
}
