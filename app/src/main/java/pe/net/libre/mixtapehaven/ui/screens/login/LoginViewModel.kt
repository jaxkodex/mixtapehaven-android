package pe.net.libre.mixtapehaven.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository

class LoginViewModel(private val repository: JellyfinRepository) : ViewModel() {

    data class UiState(
        val server: String = "https://",
        val username: String = "",
        val password: String = "",
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onServerChange(value: String) = _state.update { it.copy(server = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun connect(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.server.isBlank() || current.username.isBlank()) {
            _state.update { it.copy(error = "Enter a server address and username") }
            return
        }
        if (current.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.authenticate(current.server, current.username, current.password).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.toUserMessage()) }
                },
            )
        }
    }

    private fun Throwable.toUserMessage(): String = when {
        this is InvalidStatusException && status == HTTP_UNAUTHORIZED -> "Incorrect username or password"
        else -> message ?: "Could not connect to the server"
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
