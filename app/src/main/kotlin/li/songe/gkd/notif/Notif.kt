package li.songe.gkd.notif

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import kotlinx.atomicfu.atomic
import li.songe.gkd.META
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.app
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.service.ActivityService
import li.songe.gkd.service.ButtonService
import li.songe.gkd.service.EventService
import li.songe.gkd.service.HttpService
import li.songe.gkd.service.ScreenshotService
import li.songe.gkd.service.TrackService
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.componentName
import kotlin.reflect.KClass

// 相同的 request code 会导致后续 PendingIntent 失效
private val pendingIntentReqId = atomic(0)

data class Notif(
    val channel: NotifChannel = NotifChannel.Default,
    val id: Int,
    val smallIcon: Int = R.drawable.ic_status,
    val title: String,
    val text: String? = null,
    val ongoing: Boolean = true,
    val autoCancel: Boolean = false,
    val uri: String? = null,
    val stopService: KClass<out Service>? = null,
) {
    private fun toNotification(): Notification {
        val contextIntent = PendingIntent.getActivity(
            app,
            pendingIntentReqId.incrementAndGet(),
            Intent().apply {
                component = MainActivity::class.componentName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data = uri?.toUri()
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(app, channel.id)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contextIntent)
            .setOngoing(ongoing)
            .setAutoCancel(autoCancel)
        if (stopService != null) {
            val deleteIntent = PendingIntent.getBroadcast(
                app,
                pendingIntentReqId.incrementAndGet(),
                StopServiceReceiver.getIntent(stopService),
                PendingIntent.FLAG_IMMUTABLE
            )
            notification
                .setDeleteIntent(deleteIntent)
                .addAction(0, li.songe.gkd.i18n.t("k_a17f70a8d3d6"), deleteIntent)
        }
        return notification.build()
    }

    fun notifySelf() {
        if (!notificationState.updateAndGet()) return
        if (!foregroundServiceSpecialUseState.updateAndGet()) return
        @SuppressLint("MissingPermission")
        NotificationManagerCompat.from(app).notify(id, toNotification())
    }

    context(service: Service)
    fun notifyService() {
        if (!notificationState.updateAndGet()) return
        if (!foregroundServiceSpecialUseState.updateAndGet()) return
        ServiceCompat.startForeground(
            service,
            id,
            toNotification(),
            if (AndroidTarget.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST else -1
        )
    }
}

val abNotif by lazy {
    Notif(
        id = 100,
        title = META.appName,
        text = li.songe.gkd.i18n.t("k_f106cde067c2"),
    )
}

val screenshotNotif = Notif(
    id = 101,
    title = li.songe.gkd.i18n.t("k_57e70da3fd3f"),
    text = li.songe.gkd.i18n.t("k_5d6266ee33c0"),
    uri = "gkd://page/1",
    stopService = ScreenshotService::class,
)

val buttonNotif = Notif(
    id = 102,
    title = li.songe.gkd.i18n.t("k_beb79c5e2c6e"),
    text = li.songe.gkd.i18n.t("k_34a7b235f70e"),
    uri = "gkd://page/1",
    stopService = ButtonService::class,
)

val httpNotif = Notif(
    id = 103,
    title = li.songe.gkd.i18n.t("k_9662efa45c3c"),
    uri = "gkd://page/1",
    stopService = HttpService::class,
)

val exposeNotif = Notif(
    id = 104,
    title = li.songe.gkd.i18n.t("k_3ab5c7597f89"),
    text = li.songe.gkd.i18n.t("k_013126c11539"),
)

val snapshotNotif = Notif(
    channel = NotifChannel.Snapshot,
    id = 105,
    title = li.songe.gkd.i18n.t("k_502b37c69925"),
    ongoing = false,
    autoCancel = true,
    uri = "gkd://page/2",
)

val recordNotif = Notif(
    id = 106,
    title = li.songe.gkd.i18n.t("k_120cd9d42a5b"),
    uri = "gkd://page/1",
    stopService = ActivityService::class,
)

val eventNotif = Notif(
    id = 107,
    title = li.songe.gkd.i18n.t("k_3067d10f92ff"),
    uri = "gkd://page/1",
    stopService = EventService::class,
)

val trackNotif = Notif(
    id = 108,
    title = li.songe.gkd.i18n.t("k_41ae4345a1ff"),
    uri = "gkd://page?tab=3",
    stopService = TrackService::class,
)
