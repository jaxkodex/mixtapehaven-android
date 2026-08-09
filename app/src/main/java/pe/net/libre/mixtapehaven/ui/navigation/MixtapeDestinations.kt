package pe.net.libre.mixtapehaven.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A route request that never arrives — the default where no deep link is possible (previews,
 * tests). A shared instance, so composables defaulting to it do not allocate a new flow per
 * recomposition and reset the state collected from it.
 */
val NoPendingRoute: StateFlow<String?> = MutableStateFlow(null)

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
    const val DOWNLOADS = "downloads"
    const val VIDEO_LIBRARY = "video_library"
    const val VIDEO_DETAIL = "video_detail/{itemId}"
    const val VIDEO_PLAYER = "video_player/{itemId}"

    fun videoDetail(itemId: String) = "video_detail/$itemId"
    fun videoPlayer(itemId: String) = "video_player/$itemId"
}

/**
 * The route to navigate to for an externally [requested] one (e.g. a tap on the playback
 * notification), or null to ignore it. Requests are dropped while signed out — Now Playing behind
 * the login wall has nothing to show — and when the user is already on the target route.
 */
internal fun resolveDeepLink(requested: String, currentRoute: String): String? {
    if (currentRoute == Routes.LOGIN) return null
    return requested.takeIf { it != currentRoute }
}
