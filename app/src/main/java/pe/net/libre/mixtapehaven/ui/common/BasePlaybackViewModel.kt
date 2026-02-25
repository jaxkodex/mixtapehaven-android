package pe.net.libre.mixtapehaven.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.playback.PlaybackManager
import pe.net.libre.mixtapehaven.ui.home.Song

abstract class BasePlaybackViewModel(
    protected val playbackManager: PlaybackManager
) : ViewModel() {

    protected abstract fun getSongs(): List<Song>

    fun onPlayAllClick() {
        val songs = getSongs()
        if (songs.isNotEmpty()) {
            playbackManager.setQueue(songs, startIndex = 0)
        }
    }

    fun onShuffleClick() {
        val songs = getSongs()
        if (songs.isNotEmpty()) {
            val shuffledSongs = songs.shuffled()
            playbackManager.setQueue(shuffledSongs, startIndex = 0)
        }
    }

    fun onSongClick(song: Song) {
        val songs = getSongs()
        val index = songs.indexOf(song)
        if (index != -1) {
            // Set queue starting from the clicked song
            playbackManager.setQueue(songs, startIndex = index)
        } else {
            // Fallback to just playing the song
            playbackManager.playSong(song)
        }
    }

    fun onPlayPauseClick() {
        playbackManager.togglePlayPause()
    }

    protected fun performInstantMix(
        fetchMix: suspend () -> Result<List<Song>>,
        onStartLoading: () -> Unit,
        onError: (Throwable) -> Unit,
        onStopLoading: () -> Unit
    ) {
        viewModelScope.launch {
            onStartLoading()
            try {
                fetchMix()
                    .onSuccess { songs ->
                        if (songs.isNotEmpty()) {
                            playbackManager.setQueue(songs, 0)
                        }
                    }
                    .onFailure { error ->
                        onError(error)
                    }
            } finally {
                onStopLoading()
            }
        }
    }
}
