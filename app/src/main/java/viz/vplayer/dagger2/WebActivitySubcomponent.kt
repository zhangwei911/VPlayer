package viz.vplayer.dagger2

import dagger.Subcomponent
import dagger.android.AndroidInjector
import viz.vplayer.ui.activity.WebActivity

@Subcomponent(modules = [
    MyObserverModule::class
])
interface WebActivitySubcomponent : AndroidInjector<WebActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Factory<WebActivity> {
        abstract fun myObserverModule(myObserverModule: MyObserverModule): Builder
    }
}