package viz.vplayer.dagger2

import dagger.Subcomponent
import dagger.android.AndroidInjector
import viz.vplayer.ui.activity.HistoryActivity

@Subcomponent(modules = [
    MyObserverModule::class
])
interface HistoryActivitySubcomponent : AndroidInjector<HistoryActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Factory<HistoryActivity> {
        abstract fun myObserverModule(myObserverModule: MyObserverModule): Builder
    }
}