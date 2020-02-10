package viz.vplayer.dagger2

import dagger.Subcomponent
import dagger.android.AndroidInjector
import viz.vplayer.ui.activity.MainActivity

@Subcomponent(modules = [
    MyObserverModule::class
])
interface MainActivitySubcomponent : AndroidInjector<MainActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MainActivity>() {
        abstract fun myObserverModule(myObserverModule: MyObserverModule): Builder
    }
}