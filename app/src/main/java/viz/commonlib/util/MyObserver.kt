package viz.commonlib.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle.State.*

class MyObserver(var lifecycle: Lifecycle,var className: String) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun ON_CREATE() {
        println("$className@@@@@@@@MyObserver:ON_CREATE")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun ON_START() {
        if (lifecycle.currentState.isAtLeast(STARTED)) {
            println("$className@@@@@@@@MyObserver:ON_START")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun ON_RESUME() {
        if (lifecycle.currentState.isAtLeast(RESUMED)) {
            println("$className@@@@@@@@MyObserver:ON_RESUME")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun ON_PAUSE() {
        if (lifecycle.currentState.isAtLeast(STARTED)) {
            println("$className@@@@@@@@MyObserver:ON_PAUSE")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun ON_STOP() {
        if (lifecycle.currentState.isAtLeast(CREATED)) {
            println("$className@@@@@@@@MyObserver:ON_STOP")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun ON_DESTROY() {
        if (lifecycle.currentState.isAtLeast(DESTROYED)) {
            println("$className@@@@@@@@MyObserver:ON_DESTROY")
        }
    }

    init {
        lifecycle.addObserver(this)
    }
}