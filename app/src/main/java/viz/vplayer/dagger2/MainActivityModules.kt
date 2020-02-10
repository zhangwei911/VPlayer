package viz.vplayer.dagger2

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import viz.vplayer.ui.activity.MainActivity

@Module(subcomponents = [MainActivitySubcomponent::class])
abstract class MainActivityModules {
    @Binds
    @IntoMap
    @ClassKey(MainActivity::class)
    abstract fun bindMainActivityInjectorFactory(builder: MainActivitySubcomponent.Builder): AndroidInjector.Factory<*>
}