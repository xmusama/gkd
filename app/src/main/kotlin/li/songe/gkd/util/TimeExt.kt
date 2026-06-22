package li.songe.gkd.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatTimeAgo(timestamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val timeDifference = currentTime - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference)
    val hours = TimeUnit.MILLISECONDS.toHours(timeDifference)
    val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
    val weeks = days / 7
    val months = (days / 30)
    val years = (days / 365)
    return when {
        years > 0 -> li.songe.gkd.i18n.t("k_a397d4cc04b9", years)
        months > 0 -> li.songe.gkd.i18n.t("k_a26c600d24a3", months)
        weeks > 0 -> li.songe.gkd.i18n.t("k_1e925fb09f50", weeks)
        days > 0 -> li.songe.gkd.i18n.t("k_34a8dcab96f4", days)
        hours > 0 -> li.songe.gkd.i18n.t("k_d0e50059e5e7", hours)
        minutes > 0 -> li.songe.gkd.i18n.t("k_edd9dccd3d8a", minutes)
        else -> li.songe.gkd.i18n.t("k_9e636642d6d4")
    }
}

private val formatDateMap by lazy { hashMapOf<String, SimpleDateFormat>() }

fun Long.format(formatStr: String): String {
    var df = formatDateMap[formatStr]
    if (df == null) {
        df = SimpleDateFormat(formatStr, Locale.getDefault())
        formatDateMap[formatStr] = df
    }
    return df.format(this)
}

data class ThrottleTimer(
    private val interval: Long = 500L,
) {
    private var lastAccessTime: Long = 0L
    fun expired(): Boolean {
        val t = System.currentTimeMillis()
        if (t - lastAccessTime > interval) {
            lastAccessTime = t
            return true
        }
        return false
    }
}

@Composable
fun throttle(
    fn: (() -> Unit),
): (() -> Unit) {
    val timer = remember { ThrottleTimer() }
    return remember(fn) {
        {
            if (timer.expired()) {
                fn.invoke()
            }
        }
    }
}

@Composable
fun <T> throttle(
    fn: ((T) -> Unit),
): ((T) -> Unit) {
    val timer = remember { ThrottleTimer() }
    return remember(fn) {
        {
            if (timer.expired()) {
                fn.invoke(it)
            }
        }
    }
}
