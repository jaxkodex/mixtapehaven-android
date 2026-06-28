package pe.net.libre.mixtapehaven.model

import androidx.compose.ui.graphics.Color

/** A single track. [artColor] drives the vinyl artwork tint since there are no real images yet. */
data class Track(
    val title: String,
    val artist: String,
    val durationLabel: String,
    val artColor: Color,
    val downloaded: Boolean = false,
    val sizeLabel: String = "",
)

/** An album/mixtape tile on the Home grid. */
data class Album(
    val title: String,
    val artist: String,
    val artColor: Color,
    val downloaded: Boolean = true,
)

private val Coral = Color(0xFFC65B4E)
private val Olive = Color(0xFF9A8A3C)
private val Mauve = Color(0xFFA86B7E)
private val Rust = Color(0xFFB5633B)
private val Sand = Color(0xFFB59A4E)

object SampleData {

    const val USER_NAME = "Alex"
    const val GREETING = "Good evening"
    const val SERVER_HOST = "jellyfin.home.lan"

    /** Toggle to preview the first-run (empty downloads) Home variant. */
    const val FIRST_RUN = false

    val onDevice = listOf(
        Album("Midnight Tape", "Lo-Fi Beats", Coral),
        Album("Slow Static", "Wovenhand", Olive),
        Album("Paper Moon", "The Reverie", Mauve),
        Album("Harvest Moon Pt. II", "Junip Sands", Rust),
    )

    val searchResults = listOf(
        Track("Moonlit Drive", "Hana Verdura", "4:12", Rust, downloaded = true),
        Track("Bad Moon Rising", "The Togan Hultavs", "3:47", Coral),
        Track("Moonage", "Sails", "5:21", Olive, downloaded = true),
        Track("Harvest Moon Pt. II", "Junip Sands", "6:18", Sand, downloaded = true),
        Track("Blue Moon Motel", "The Reverie", "3:55", Mauve),
    )

    val topResult = Album("Paper Moon", "The Reverie", Mauve)

    val nowPlaying = Track("Harvest Moon Pt. II", "Junip Sands", "5:40", Accent_, downloaded = true)
    val upNext = Track("Blue Moon Motel", "The Reverie", "3:55", Mauve)

    val downloads = listOf(
        Track("Moonlit Drive", "Hana Verdura", "4:12", Rust, downloaded = true, sizeLabel = "9.8 MB"),
        Track("Paper Moon", "The Reverie", "3:30", Mauve, downloaded = true, sizeLabel = "8.1 MB"),
        Track("Harvest Moon Pt. II", "Junip Sands", "6:18", Sand, downloaded = true, sizeLabel = "14 MB"),
        Track("Blue Moon Motel", "The Reverie", "3:55", Mauve, downloaded = true, sizeLabel = "9.2 MB"),
    )

    val downloading = Track("Moonage", "Sails", "5:21", Olive, sizeLabel = "68%")
}

private val Accent_ = Color(0xFFD8A93E)
