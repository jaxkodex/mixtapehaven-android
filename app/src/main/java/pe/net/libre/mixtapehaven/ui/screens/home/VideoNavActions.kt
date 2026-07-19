package pe.net.libre.mixtapehaven.ui.screens.home

/**
 * Where the video surfaces on Home navigate to: a title's detail, a resume straight into the
 * player, and the full library. Grouped so they travel together through Home's call chain.
 */
data class VideoNavActions(
    val onOpenVideo: (String) -> Unit,
    val onResumeVideo: (String) -> Unit,
    val onOpenLibrary: () -> Unit,
)
