package pe.net.libre.mixtapehaven.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationRequest(
    @SerialName("Username")
    val username: String,
    @SerialName("Pw")
    val password: String
)

@Serializable
data class AuthenticationResponse(
    @SerialName("User")
    val user: JellyfinUser,
    @SerialName("AccessToken")
    val accessToken: String,
    @SerialName("ServerId")
    val serverId: String
)

@Serializable
data class JellyfinUser(
    @SerialName("Name")
    val name: String,
    @SerialName("ServerId")
    val serverId: String,
    @SerialName("Id")
    val id: String,
    @SerialName("HasPassword")
    val hasPassword: Boolean = false,
    @SerialName("HasConfiguredPassword")
    val hasConfiguredPassword: Boolean = false,
    @SerialName("HasConfiguredEasyPassword")
    val hasConfiguredEasyPassword: Boolean = false
)

@Serializable
data class SystemInfo(
    @SerialName("ServerName")
    val serverName: String,
    @SerialName("Version")
    val version: String,
    @SerialName("Id")
    val id: String,
    @SerialName("OperatingSystem")
    val operatingSystem: String? = null
)

// Media item models
@Serializable
data class ItemsResponse(
    @SerialName("Items")
    val items: List<BaseItemDto> = emptyList(),
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int = 0,
    @SerialName("StartIndex")
    val startIndex: Int = 0
)

@Serializable
data class BaseItemDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Type")
    val type: String,
    @SerialName("AlbumArtist")
    val albumArtist: String? = null,
    @SerialName("AlbumArtists")
    val albumArtists: List<NameIdPair>? = null,
    @SerialName("Artists")
    val artists: List<String>? = null,
    @SerialName("ArtistItems")
    val artistItems: List<NameIdPair>? = null,
    @SerialName("Album")
    val album: String? = null,
    @SerialName("AlbumId")
    val albumId: String? = null,
    @SerialName("ProductionYear")
    val productionYear: Int? = null,
    @SerialName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    @SerialName("HasSubtitles")
    val hasSubtitles: Boolean? = null,
    @SerialName("ImageTags")
    val imageTags: Map<String, String>? = null,
    @SerialName("BackdropImageTags")
    val backdropImageTags: List<String>? = null,
    @SerialName("UserData")
    val userData: UserItemDataDto? = null,
    @SerialName("IndexNumber")
    val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber")
    val parentIndexNumber: Int? = null,
    @SerialName("ChildCount")
    val childCount: Int? = null,
    @SerialName("DateCreated")
    val dateCreated: String? = null,
    @SerialName("PremiereDate")
    val premiereDate: String? = null,
    @SerialName("MediaSources")
    val mediaSources: List<MediaSourceInfo>? = null
)

@Serializable
data class NameIdPair(
    @SerialName("Name")
    val name: String,
    @SerialName("Id")
    val id: String
)

@Serializable
data class UserItemDataDto(
    @SerialName("PlaybackPositionTicks")
    val playbackPositionTicks: Long? = null,
    @SerialName("PlayCount")
    val playCount: Int? = null,
    @SerialName("IsFavorite")
    val isFavorite: Boolean = false,
    @SerialName("Played")
    val played: Boolean = false,
    @SerialName("Key")
    val key: String? = null
)

@Serializable
data class MediaSourceInfo(
    @SerialName("Id")
    val id: String,
    @SerialName("Size")
    val size: Long? = null,
    @SerialName("Bitrate")
    val bitrate: Int? = null,
    @SerialName("Container")
    val container: String? = null
)

// Search hint models
@Serializable
data class SearchHintResult(
    @SerialName("SearchHints")
    val searchHints: List<SearchHint> = emptyList(),
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int = 0
)

@Serializable
data class SearchHint(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Type")
    val type: String,
    @SerialName("Album")
    val album: String? = null,
    @SerialName("AlbumArtist")
    val albumArtist: String? = null,
    @SerialName("Artists")
    val artists: List<String>? = null,
    @SerialName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    @SerialName("PrimaryImageTag")
    val primaryImageTag: String? = null
)
