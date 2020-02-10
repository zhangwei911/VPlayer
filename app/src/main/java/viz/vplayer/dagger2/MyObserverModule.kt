package viz.vplayer.dagger2

import androidx.lifecycle.Lifecycle
import dagger.Module
import dagger.Provides
import viz.commonlib.util.MyObserver

@Module
class MyObserverModule(var lifecycle: Lifecycle, var className: String) {

    @Provides
    fun getMyObserver(): MyObserver {
        return MyObserver(lifecycle,className)
    }
}