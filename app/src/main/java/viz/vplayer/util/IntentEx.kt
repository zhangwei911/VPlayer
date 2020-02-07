package viz.vplayer.util

import android.content.Intent

fun Intent.getStringExtra(name: String, defaultValue: String): String {
    return getStringExtra(name) ?: defaultValue
}