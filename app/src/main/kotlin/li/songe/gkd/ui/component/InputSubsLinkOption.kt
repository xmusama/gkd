package li.songe.gkd.ui.component

import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import li.songe.gkd.ui.WebViewRoute
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.subsItemsFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import kotlin.coroutines.resume


class InputSubsLinkOption {
    private val showFlow = MutableStateFlow(false)
    private val valueFlow = MutableStateFlow("")
    private val initValueFlow = MutableStateFlow("")
    private var continuation: CancellableContinuation<String?>? = null

    private fun resume(value: String?) {
        showFlow.value = false
        valueFlow.value = ""
        initValueFlow.value = ""
        if (continuation?.isActive == true) {
            continuation?.resume(value)
        }
        continuation = null
    }

    private fun submit() {
        val value = valueFlow.value
        if (!URLUtil.isNetworkUrl(value)) {
            toast(li.songe.gkd.i18n.t("k_e7e0ffcd50fb"))
            return
        }
        val initValue = initValueFlow.value
        if (initValue.isNotEmpty() && initValue == value) {
            toast(li.songe.gkd.i18n.t("k_fff8cc4d9427"))
            resume(null)
            return
        }
        if (subsItemsFlow.value.any { it.updateUrl == value }) {
            toast(li.songe.gkd.i18n.t("k_d41dda6f65c1"))
            return
        }
        resume(value)
    }

    private fun cancel() = resume(null)

    suspend fun getResult(initValue: String = ""): String? {
        initValueFlow.value = initValue
        valueFlow.value = initValue
        showFlow.value = true
        return suspendCancellableCoroutine {
            continuation = it
        }
    }

    @Composable
    fun ContentDialog() {
        val show by showFlow.collectAsState()
        if (show) {
            val mainVm = LocalMainViewModel.current
            val value by valueFlow.collectAsState()
            val initValue by initValueFlow.collectAsState()
            AlertDialog(
                properties = DialogProperties(dismissOnClickOutside = false),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = if (initValue.isNotEmpty()) li.songe.gkd.i18n.t("k_1508e32d3576") else li.songe.gkd.i18n.t("k_6debaa888532"))
                        PerfIconButton(
                            imageVector = PerfIcon.HelpOutline,
                            contentDescription = li.songe.gkd.i18n.t("k_761d7af04af4"),
                            onClick = throttle {
                                cancel()
                                mainVm.navigatePage(WebViewRoute(initUrl = ShortUrlSet.URL5))
                            })
                    }
                },
                text = {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            valueFlow.value = it.trim()
                        },
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .autoFocus(),
                        placeholder = {
                            Text(text = li.songe.gkd.i18n.t("k_a00626547a1b"))
                        },
                        isError = value.isNotEmpty() && !URLUtil.isNetworkUrl(value),
                    )
                },
                onDismissRequest = {
                    cancel()
                },
                confirmButton = {
                    TextButton(
                        enabled = value.isNotEmpty(),
                        onClick = throttle(fn = {
                            submit()
                        }),
                    ) {
                        Text(text = li.songe.gkd.i18n.t("k_f526c89937e1"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = ::cancel) {
                        Text(text = li.songe.gkd.i18n.t("k_4d0b4688c787"))
                    }
                },
            )
        }
    }
}
