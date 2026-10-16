package com.vknxworks.navictrl.ui.nav

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/** One place that knows where things open. */
class Navigator(val nav: NavHostController) {

    fun navigate(route: Any) = nav.navigate(route) { launchSingleTop = true }

    fun back() {
        nav.popBackStack()
    }

    fun album(id: String?) {
        if (!id.isNullOrBlank()) navigate(AlbumRoute(id))
    }

    fun artist(id: String?) {
        if (!id.isNullOrBlank()) navigate(ArtistRoute(id))
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator") }
