package com.example.tabidachi

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.example.tabidachi.auth.AuthManager
import com.example.tabidachi.data.AppDatabase
import com.example.tabidachi.data.PreferencesManager
import com.example.tabidachi.data.SecureStorage
import com.example.tabidachi.data.TripRepository
import com.example.tabidachi.network.TabidachiApi
import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
class TabidachiApp : Application(), SingletonImageLoader.Factory {

    @Volatile
    var isAuthenticated: Boolean = false

    @Volatile
    private var backgroundedAt: Long = 0L

    val secureStorage by lazy { SecureStorage(this) }
    val prefsManager by lazy { PreferencesManager(this) }
    val database by lazy { AppDatabase.create(this) }
    val api by lazy { TabidachiApi(secureStorage) }
    val tripRepository by lazy { TripRepository(api, database.tripDao()) }
    val authManager by lazy { AuthManager(secureStorage) }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val imageHttpClient = HttpClient(Android) {
            defaultRequest {
                val token = secureStorage.apiToken
                if (token.isNotBlank()) {
                    // Only attach Bearer auth to the user's own configured server, so that
                    // public /share/:token/uploads/* routes on foreign instances never
                    // receive the token.
                    val configuredHost = try {
                        Uri.parse(secureStorage.serverUrl).host ?: ""
                    } catch (_: Exception) { "" }
                    if (url.host == configuredHost) {
                        header("Authorization", "Bearer $token")
                    }
                }
            }
        }
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = imageHttpClient))
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundedAt = System.currentTimeMillis()
            }

            override fun onStart(owner: LifecycleOwner) {
                if (isAuthenticated && backgroundedAt > 0L) {
                    val timeout = prefsManager.autoLockTimeoutMs
                    if (System.currentTimeMillis() - backgroundedAt > timeout) {
                        isAuthenticated = false
                    }
                }
                backgroundedAt = 0L
            }
        })
    }
}
