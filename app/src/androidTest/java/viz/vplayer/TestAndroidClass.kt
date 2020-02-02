import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.idling.net.UriIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import viz.vplayer.R
import viz.vplayer.adapter.SearchAdapter
import viz.vplayer.ui.activity.MainActivity

class TestAndroidClass {
    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    private var uriIdlingResource: UriIdlingResource? = null
    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_MEDIA_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.GET_TASKS",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"
        )

    @Before
    fun registerIdlingResource() {
        val activityScenario: ActivityScenario<MainActivity> = ActivityScenario.launch(
            MainActivity::class.java
        )
        activityScenario.onActivity { activity ->
            uriIdlingResource = activity.getIdlingResource()
            // To prove that the test fails, omit this call:
            IdlingRegistry.getInstance().register(uriIdlingResource)
        }
    }

    @Test
    fun mainActivityMethod_ReturnsTrue() {
        onView(withId(R.id.textInputEditText_search)).perform(replaceText("锦衣之下"))
        onView(withId(R.id.spinner_website)).perform(click())
        onData(
            allOf(
                `is`(instanceOf(String::class.java)),
                `is`("所有")
            )
        ).perform(click())
        onView(withId(R.id.materialButton_search)).perform(click())
        onView(withId(R.id.recyclerView_search)).perform(
            RecyclerViewActions.actionOnItemAtPosition<SearchAdapter.ViewHolder>(
                0,
                click()
            )
        )
    }

    @After
    fun unregisterIdlingResource() {
        if (uriIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(uriIdlingResource)
        }
    }
}