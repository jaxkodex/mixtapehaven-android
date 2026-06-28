package pe.net.libre.mixtapehaven.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.net.libre.mixtapehaven.data.jellyfin.JellyfinRepository

/** Decides the navigation start destination by restoring any persisted session on cold start. */
class AppStateViewModel(private val repository: JellyfinRepository) : ViewModel() {

    /** Null while the session is being restored; afterwards the route to start on. */
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restore()
            val session = repository.session.first()
            _startDestination.value = if (session != null) Routes.HOME else Routes.LOGIN
        }
    }
}
