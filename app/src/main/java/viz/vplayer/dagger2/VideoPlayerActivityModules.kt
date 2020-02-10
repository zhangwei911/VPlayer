package viz.vplayer.dagger2

import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import viz.vplayer.ui.activity.VideoPlayerActivity

@Module(subcomponents = [VideoPlayerActivitySubcomponent::class])
abstract class VideoPlayerActivityModules {
    @Binds
    @IntoMap
    @ClassKey(VideoPlayerActivity::class)
    abstract fun bindVideoPlayerActivityInjectorFactory(builder: VideoPlayerActivitySubcomponent.Builder): AndroidInjector.Factory<*>
}