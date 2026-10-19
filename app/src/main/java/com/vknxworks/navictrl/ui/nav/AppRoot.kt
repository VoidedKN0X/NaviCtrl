package com.vknxworks.navictrl.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.music.MiniPlayer
import com.vknxworks.navictrl.music.NowPlayingScreen
import com.vknxworks.navictrl.ui.auth.SignInScreen
import com.vknxworks.navictrl.ui.components.LoadingView
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.LocalHazeState
import com.vknxworks.navictrl.ui.detail.AlbumScreen
import com.vknxworks.navictrl.ui.detail.ArtistScreen
import com.vknxworks.navictrl.ui.detail.FavouritesScreen
import com.vknxworks.navictrl.ui.detail.GenreScreen
import com.vknxworks.navictrl.ui.detail.PlaylistScreen
import com.vknxworks.navictrl.ui.playlists.PlaylistsScreen
import com.vknxworks.navictrl.ui.search.SearchScreen
import com.vknxworks.navictrl.ui.settings.AccountsScreen
import com.vknxworks.navictrl.ui.settings.DownloadsScreen
import com.vknxworks.navictrl.ui.settings.SettingsScreen
import com.vknxworks.navictrl.ui.settings.UsageStatsPrompt
import com.vknxworks.navictrl.ui.settings.YouScreen
import com.vknxworks.navictrl.ui.songs.SongsScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun AppRoot() {
    val c = LocalContext.current.container
    val ready by c.sessions.ready.collectAsStateWithLifecycle()
    val active by c.sessions.active.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !ready -> LoadingView()
            active == null -> SignInScreen(onDone = {}, onCancel = null)
            else -> key(active!!.account.key) {
                // Favourite overrides belong to the previous account.
                LaunchedEffect(Unit) { c.stars.clear() }
                MainScaffold()
            }
        }
    }
}

private data class TopDest(val route: Any, val item: TabItem)

private val topDestinations = listOf(
    TopDest(SongsRoute, TabItem("Songs", Icons.Outlined.MusicNote, Icons.Filled.MusicNote)),
    TopDest(FavouritesRoute, TabItem("Favourites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)),
    TopDest(PlaylistsRoute, TabItem("Playlists", Icons.AutoMirrored.Outlined.QueueMusic, Icons.AutoMirrored.Filled.QueueMusic)),
    TopDest(SearchRoute, TabItem("Search", Icons.Outlined.Search, Icons.Filled.Search)),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navigator = remember(navController) { Navigator(navController) }
    val backStack by navController.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val selectedIndex = topDestinations.indexOfFirst { top -> dest?.hierarchy?.any { it.hasRoute(top.route::class) } == true }
    val onNowPlaying = dest?.hasRoute(NowPlayingRoute::class) == true

    // Usage stats (only if the user opted in): the kind of screen, never its contents or the album, artist or playlist it shows.
    val context = LocalContext.current
    val entryId = backStack?.id
    val screen = dest?.route?.let(::screenName)
    LaunchedEffect(entryId) {
        if (screen != null) context.container.telemetry.track("screen_viewed", mapOf("screen" to screen))
    }
    UsageStatsPrompt()

    fun selectTab(i: Int) {
        navController.navigate(topDestinations[i].route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (maxWidth >= 600.dp) {
                // Tablets and unfolded foldables: a side rail, with the mini player along the bottom.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        topDestinations.forEachIndexed { i, top ->
                            val selected = i == selectedIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectTab(i) },
                                icon = { Icon(if (selected) top.item.selectedIcon else top.item.icon, contentDescription = top.item.label) },
                                label = { Text(top.item.label) },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { AppNavHost(navController) }
                        if (!onNowPlaying) {
                            Box(Modifier.navigationBarsPadding()) { MiniPlayer(onOpen = { navigator.navigate(NowPlayingRoute) }) }
                        }
                    }
                }
            } else {
                PhoneLayout(
                    navController = navController,
                    showTabs = selectedIndex >= 0,
                    showMiniPlayer = !onNowPlaying,
                    selectedIndex = selectedIndex,
                    onSelect = ::selectTab,
                    onOpenNowPlaying = { navigator.navigate(NowPlayingRoute) },
                )
            }
        }
    }
}

/**
 * "com.vknxworks.navictrl.ui.nav.AlbumRoute/{id}" -> "Album". Only the route's class name: arguments (album, artist
 * and playlist IDs, genre names) are dropped.
 */
private fun screenName(route: String): String =
    route.substringBefore('/').substringBefore('?').substringAfterLast('.').removeSuffix("Route")

/**
 * Phones: content runs edge to edge and scrolls under a floating glass island.
 * The island shows on the main tabs; pushed screens get the full height.
 */
@Composable
private fun PhoneLayout(
    navController: NavHostController,
    showTabs: Boolean,
    showMiniPlayer: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var overlayHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalBottomClearance provides if (showTabs) overlayHeight else 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .then(if (showTabs) Modifier else Modifier.padding(bottom = overlayHeight)),
            ) {
                AppNavHost(navController)
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { overlayHeight = with(density) { it.height.toDp() } }
                    .navigationBarsPadding()
                    .padding(bottom = if (showTabs) 12.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showMiniPlayer) MiniPlayer(onOpen = onOpenNowPlaying)
                AnimatedVisibility(
                    visible = showTabs,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    GlassTabBar(
                        items = topDestinations.map { it.item },
                        selectedIndex = selectedIndex,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = SongsRoute) {
        composable<SongsRoute> { SongsScreen() }
        composable<PlaylistsRoute> { PlaylistsScreen() }
        composable<SearchRoute> { SearchScreen() }
        composable<YouRoute> { YouScreen() }

        composable<AlbumRoute> { AlbumScreen(it.toRoute<AlbumRoute>().id) }
        composable<ArtistRoute> { ArtistScreen(it.toRoute<ArtistRoute>().id) }
        composable<PlaylistRoute> { PlaylistScreen(it.toRoute<PlaylistRoute>().id) }
        composable<GenreRoute> { GenreScreen(it.toRoute<GenreRoute>().name) }
        composable<FavouritesRoute> { FavouritesScreen() }
        composable<NowPlayingRoute> { NowPlayingScreen() }
        composable<DownloadsRoute> { DownloadsScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<AccountsRoute> { AccountsScreen() }
        composable<AddAccountRoute> {
            val navigator = LocalNavigator.current
            SignInScreen(onDone = { navController.popBackStack(SongsRoute, false) }, onCancel = navigator::back)
        }
    }
}
