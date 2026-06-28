package pe.net.libre.mixtapehaven.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pe.net.libre.mixtapehaven.MixtapeApplication

/** Resolves the app's [AppContainer] from the current Android context. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as MixtapeApplication).container

/** Creates a [ViewModel] wired with the [AppContainer], scoped to the current nav entry. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
