package viz.vplayer.dagger2

import dagger.Subcomponent
import dagger.android.AndroidInjector
import viz.vplayer.ui.activity.VideoPlayerActivity

@Subcomponent(modules = [
    MyObserverModule::class
])
interface VideoPlayerActivitySubcomponent : AndroidInjector<VideoPlayerActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Factory<VideoPlayerActivity> {
        abstract fun myObserverModule(myObserverModule: MyObserverModule): Builder
    }
}