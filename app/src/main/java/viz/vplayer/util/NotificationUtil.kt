package viz.vplayer.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import viz.vplayer.R
import viz.vplayer.ui.activity.MainActivity

class NotificationUtil(private val context: Context) {
    fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "下载视频渠道名"
            val descriptionText = "下载视频"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_DOWNLOAD, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotificationBuilder(
        channelId: String,
        groupKey: String,
        title: String,
        text: String,
        smallIcon: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        onlyAlertOnce: Boolean = true,
        isBigText: Boolean = false,
        bigText: String = "",
        clickable: Boolean = false,
        intent: Intent? = null,
        requestCode: Int = 0,
        flags: Int = 0,
        isGroupSummary: Boolean = false
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId).apply {
            setContentText(text)
            setContentTitle(title)
            setSmallIcon(smallIcon)
            this.priority = priority
            if(onlyAlertOnce) {
                setOnlyAlertOnce(onlyAlertOnce)
            }
            setGroup(groupKey)
            if(isGroupSummary) {
                setGroupSummary(isGroupSummary)
            }
            if (isBigText) {
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bigText)
                )
            }
            if (clickable) {
                val pendingIntent: PendingIntent =
                    PendingIntent.getActivity(context, requestCode, intent, flags)
                setContentIntent(pendingIntent)
            }
        }
    }
}