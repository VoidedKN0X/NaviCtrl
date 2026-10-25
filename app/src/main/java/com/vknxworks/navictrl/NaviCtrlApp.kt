package com.vknxworks.navictrl

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vknxworks.navictrl.ui.lock.AppLock
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

class NaviCtrlApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppStrings.context = this
        container = AppContainer(this)
        // The app lock: lock when the app leaves the screen, or after the chosen time away.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppLock.onForeground(container.lockEnabled, container.settings.settings.value.lockTimeout.millis)
            }

            override fun onStop(owner: LifecycleOwner) {
                AppLock.onBackground(container.lockEnabled, container.settings.settings.value.lockTimeout.millis)
                // Play queue sync: the server gets the queue and position as the app goes to the background.
                runCatching { container.music.saveNow() }
            }
        })
    }

    /**
     * Artwork goes through the app's own HTTP client, so the same rules apply as everywhere else:
     * plain HTTP only to a server the user chose, and no session headers on redirects to other servers.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.http })) }
            .build()
}
