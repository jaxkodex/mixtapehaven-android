package pe.net.libre.mixtapehaven.data.download

/**
 * Quality cap applied to video downloads. Videos are transcoded server-side to h264/aac mp4
 * within these limits, so a phone-sized copy is saved instead of the (often huge) original.
 */
enum class VideoDownloadQuality(
    val label: String,
    val maxHeight: Int,
    val videoBitRate: Int,
    val audioBitRate: Int,
) {
    HD_1080("1080p", 1080, 10_000_000, 384_000),
    HD_720("720p", 720, 4_000_000, 256_000),
    SD_480("480p", 480, 1_500_000, 128_000),
    ;

    companion object {
        val DEFAULT = HD_720

        /** The quality persisted under [name], falling back to [DEFAULT] for unknown/absent values. */
        fun fromName(name: String?): VideoDownloadQuality =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
