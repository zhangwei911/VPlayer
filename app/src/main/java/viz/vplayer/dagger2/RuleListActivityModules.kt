package viz.vplayer.dagger2

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import viz.vplayer.ui.activity.RuleListActivity

@Module(subcomponents = [RuleListActivitySubcomponent::class])
abstract class RuleListActivityModules {
    @Binds
    @IntoMap
    @ClassKey(RuleListActivity::class)
    abstract fun bindRuleListActivityInjectorFactory(builder: RuleListActivitySubcomponent.Builder): AndroidInjector.Factory<*>
}