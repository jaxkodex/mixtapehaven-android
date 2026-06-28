package pe.net.libre.mixtapehaven.di

import android.content.Context
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.data.session.SessionStore

/** Manual dependency container holding app-scoped singletons. Created once in MixtapeApplication. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val jellyfin: Jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "Mixtape Haven", version = "1.0")
        this.context = appContext
    }

    val sessionStore: SessionStore = SessionStore(appContext)

    val repository: JellyfinRepository = JellyfinRepository(jellyfin, sessionStore)

    val playerController: PlayerController by lazy { PlayerController(appContext, repository) }
}
