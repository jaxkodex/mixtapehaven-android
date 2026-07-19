package pe.net.libre.mixtapehaven.ui.navigation

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
