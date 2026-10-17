package com.vknxworks.navictrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object SongsRoute
@Serializable data object LibraryRoute
@Serializable data object SearchRoute
@Serializable data object YouRoute

@Serializable data class AlbumRoute(val id: String)
@Serializable data class ArtistRoute(val id: String)
@Serializable data class PlaylistRoute(val id: String)
@Serializable data class GenreRoute(val name: String)


@Serializable data object FavouritesRoute
@Serializable data object NowPlayingRoute
@Serializable data object DownloadsRoute
@Serializable data object SettingsRoute
@Serializable data object AccountsRoute
@Serializable data object AddAccountRoute
