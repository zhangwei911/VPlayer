package viz.vplayer.dagger2

import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import viz.vplayer.util.App
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        MainActivityModules::class,
        HistoryActivityModules::class,
        RuleListActivityModules::class,
        VideoPlayerActivityModules::class,
        WebActivityModules::class
    ]
)
interface AppComponent : AndroidInjector<App> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<App>()

    fun mainActivitySubcomponentBuilder(): MainActivitySubcomponent.Builder
    fun historyActivitySubcomponentBuilder(): HistoryActivitySubcomponent.Builder
    fun ruleListActivitySubcomponentBuilder(): RuleListActivitySubcomponent.Builder
    fun videoPlayerActivitySubcomponentBuilder(): VideoPlayerActivitySubcomponent.Builder
    fun webActivitySubcomponentBuilder(): WebActivitySubcomponent.Builder
}