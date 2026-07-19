package pe.net.libre.mixtapehaven.di

import android.content.Context
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import pe.net.libre.mixtapehaven.data.download.DownloadDatabase
import pe.net.libre.mixtapehaven.data.download.DownloadManager
import pe.net.libre.mixtapehaven.data.download.DownloadSettingsStore
import pe.net.libre.mixtapehaven.data.download.VideoDownloadManager
import pe.net.libre.mixtapehaven.data.download.resolveLocalFirst
import pe.net.libre.mixtapehaven.data.diagnostics.DiagnosticsLog
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.data.playback.VideoProgressStore
import pe.net.libre.mixtapehaven.data.session.SessionStore

/** Manual dependency container holding app-scoped singletons. Created once in MixtapeApplication. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val jellyfin: Jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "Mixtape Haven", version = "1.0")
        this.context = appContext
    }

    val sessionStore: SessionStore = SessionStore(appContext)

    /** Ring buffer of recent diagnostic events, exportable from Settings > Share diagnostics. */
    val diagnosticsLog: DiagnosticsLog = DiagnosticsLog()

    val repository: JellyfinRepository = JellyfinRepository(jellyfin, sessionStore)

    val downloadSettingsStore: DownloadSettingsStore = DownloadSettingsStore(appContext)

    private val downloadDatabase: DownloadDatabase by lazy { DownloadDatabase.build(appContext) }

    val playerController: PlayerController by lazy { PlayerController(appContext, repository, diagnosticsLog) }

    /**
     * Resolves ordered playable source URLs for a video item id. Defaults to remote streaming
     * (direct play, then HLS transcode); swap to serve local video downloads first, mirroring
     * [PlayerController.streamUrlResolver] for audio. Suspending because building the transcode
     * URL negotiates the media source id with the server.
     */
    var videoSourceResolver: suspend (String) -> List<String> = repository::videoStreamCandidates

    /** Local + server watch positions for video, backing offline resume and Continue watching. */
    val videoProgressStore: VideoProgressStore by lazy {
        VideoProgressStore(repository, downloadDatabase.videoProgressDao())
    }

    val downloadManager: DownloadManager by lazy {
        DownloadManager(
            context = appContext,
            repository = repository,
            dao = downloadDatabase.downloadDao(),
            settingsStore = downloadSettingsStore,
        )
    }

    val videoDownloadManager: VideoDownloadManager by lazy {
        VideoDownloadManager(
            context = appContext,
            repository = repository,
            dao = downloadDatabase.videoDownloadDao(),
            settingsStore = downloadSettingsStore,
        )
    }

    init {
        // Wire the two app-scoped singletons after both are built, so neither depends on the other
        // at construction time. Eager so auto-download runs before any download UI is opened.
        val manager = downloadManager
        val controller = playerController
        // Local-first: serve saved bytes when present, else fall back to the remote stream.
        controller.streamUrlResolver = { track ->
            resolveLocalFirst(track, manager::localUriFor, repository::audioStreamUrl)
        }
        manager.start(controller.nowPlaying)
        // Same local-first idea for video: a saved copy is the only candidate (no network fallback
        // mid-list — if the file plays, remote candidates are never needed; if it's gone the
        // resolver won't have listed it since localUriFor checks existence).
        val videoManager = videoDownloadManager
        videoSourceResolver = { itemId ->
            videoManager.localUriFor(itemId)?.let { listOf(it) } ?: repository.videoStreamCandidates(itemId)
        }
    }
}
