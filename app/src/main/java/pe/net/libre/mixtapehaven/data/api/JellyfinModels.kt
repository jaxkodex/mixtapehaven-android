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
