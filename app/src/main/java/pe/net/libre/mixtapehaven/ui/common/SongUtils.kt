package pe.net.libre.mixtapehaven.ui.common

import pe.net.libre.mixtapehaven.ui.home.Song

fun calculateTotalDuration(songs: List<Song>): String {
    var totalSeconds = 0
    songs.forEach { song ->
        val parts = song.duration.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].toIntOrNull() ?: 0
            totalSeconds += (minutes * 60) + seconds
        }
    }

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        "$hours hr $minutes min"
    } else {
        "$minutes min"
    }
}
