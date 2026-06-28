package pe.net.libre.mixtapehaven

import android.app.Application
import pe.net.libre.mixtapehaven.di.AppContainer

class MixtapeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
