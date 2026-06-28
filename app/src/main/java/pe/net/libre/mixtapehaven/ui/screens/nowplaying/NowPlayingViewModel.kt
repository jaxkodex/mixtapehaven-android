package pe.net.libre.mixtapehaven.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import pe.net.libre.mixtapehaven.data.playback.PlayerController
import pe.net.libre.mixtapehaven.model.Track
import kotlinx.coroutines.flow.StateFlow

class NowPlayingViewModel(
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<Track?> = playerController.nowPlaying
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val positionMs: StateFlow<Long> = playerController.positionMs
    val durationMs: StateFlow<Long> = playerController.durationMs

    fun playPause() = playerController.playPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekToFraction(fraction: Float) = playerController.seekToFraction(fraction)
}
