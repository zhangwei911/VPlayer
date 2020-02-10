package viz.vplayer.dagger2

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import viz.vplayer.ui.activity.HistoryActivity

@Module(subcomponents = [HistoryActivitySubcomponent::class])
abstract class HistoryActivityModules {
    @Binds
    @IntoMap
    @ClassKey(HistoryActivity::class)
    abstract fun bindMainActivityInjectorFactory(builder: HistoryActivitySubcomponent.Builder): AndroidInjector.Factory<*>
}