package viz.vplayer.dagger2

import dagger.Subcomponent
import dagger.android.AndroidInjector
import viz.vplayer.ui.activity.RuleListActivity

@Subcomponent(modules = [
    MyObserverModule::class
])
interface RuleListActivitySubcomponent : AndroidInjector<RuleListActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Factory<RuleListActivity> {
        abstract fun myObserverModule(myObserverModule: MyObserverModule): Builder
    }
}