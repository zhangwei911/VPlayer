package viz.vplayer.dagger2

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import viz.vplayer.ui.activity.WebActivity

@Module(subcomponents = [WebActivitySubcomponent::class])
abstract class WebActivityModules {
    @Binds
    @IntoMap
    @ClassKey(WebActivity::class)
    abstract fun bindWebActivityInjectorFactory(builder: WebActivitySubcomponent.Builder): AndroidInjector.Factory<*>
}