package li.songe.gkd.ui

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.dylanc.activityresult.launcher.launchForResult
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.permission.shizukuGrantedState
import li.songe.gkd.service.ActivityService
import li.songe.gkd.service.ButtonService
import li.songe.gkd.service.EventService
import li.songe.gkd.service.HttpService
import li.songe.gkd.service.ScreenshotService
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.shizuku.updateBinderMutex
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.CustomOutlinedTextField
import li.songe.gkd.ui.component.PerfCustomIconButton
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.autoFocus
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.iconTextSize
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.appInfoMapFlow
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import li.songe.selector.Selector

@Serializable
data object AdvancedPageRoute : NavKey

@Composable
fun AdvancedPage() {
    val context = LocalActivity.current as MainActivity
    val mainVm = LocalMainViewModel.current
    val vm = viewModel<AdvancedVm>()
    val store by storeFlow.collectAsState()

    var showEditPortDlg by vm.showEditPortDlgFlow.asMutableState()
    if (showEditPortDlg) {
        val portRange = remember { 1000 to 65535 }
        val placeholderText = remember { li.songe.gkd.i18n.t("k_69bab6a24108", portRange.first, portRange.second) }
        var value by remember {
            mutableStateOf(store.httpServerPort.toString())
        }
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(text = li.songe.gkd.i18n.t("k_6f77ee7c5c99")) },
            text = {
                OutlinedTextField(
                    value = value,
                    placeholder = {
                        Text(text = placeholderText)
                    },
                    onValueChange = {
                        value = it.filter { c -> c.isDigit() }.take(5)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoFocus(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "${value.length} / 5",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                )
            },
            onDismissRequest = {
                showEditPortDlg = false
            },
            confirmButton = {
                TextButton(
                    enabled = value.isNotEmpty(),
                    onClick = {
                        val newPort = value.toIntOrNull()
                        if (newPort == null || !(portRange.first <= newPort && newPort <= portRange.second)) {
                            toast(placeholderText)
                            return@TextButton
                        }
                        showEditPortDlg = false
                        if (newPort != store.httpServerPort) {
                            storeFlow.value = store.copy(
                                httpServerPort = newPort
                            )
                            toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                        }
                    }
                ) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_b56d9ac6c5a0"), modifier = Modifier
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPortDlg = false }) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_4d0b4688c787")
                    )
                }
            }
        )
    }

    var showShizukuState by vm.showShizukuStateFlow.asMutableState()
    if (showShizukuState) {
        val onDismissRequest = { showShizukuState = false }
        AlertDialog(
            title = { Text(text = li.songe.gkd.i18n.t("k_ac3cc79f9199")) },
            text = {
                val states = shizukuContextFlow.collectAsState().value.states
                Column {
                    states.forEach { (name, value) ->
                        Text(
                            text = name,
                            textDecoration = if (value != null) null else TextDecoration.LineThrough,
                        )
                    }
                }
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = li.songe.gkd.i18n.t("k_dd3760c80abe"))
                }
            },
        )
    }

    var showCaptureScreenshotDlg by vm.showCaptureScreenshotDlgFlow.asMutableState()
    if (showCaptureScreenshotDlg) {
        var appIdValue by remember { mutableStateOf(store.screenshotTargetAppId) }
        var eventSelectorValue by remember { mutableStateOf(store.screenshotEventSelector) }
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = li.songe.gkd.i18n.t("k_ee5db675e1f0"))
                    PerfIconButton(
                        imageVector = PerfIcon.HelpOutline,
                        onClick = throttle {
                            showCaptureScreenshotDlg = false
                            mainVm.navigateWebPage(ShortUrlSet.URL15)
                        },
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CustomOutlinedTextField(
                        label = { Text(li.songe.gkd.i18n.t("k_be8af550f337")) },
                        value = appIdValue,
                        placeholder = { Text(text = li.songe.gkd.i18n.t("k_9fccef00274d")) },
                        onValueChange = {
                            appIdValue = it
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomOutlinedTextField(
                        label = { Text(li.songe.gkd.i18n.t("k_a08049752bcb")) },
                        value = eventSelectorValue,
                        placeholder = { Text(text = li.songe.gkd.i18n.t("k_ea72227d80d9")) },
                        onValueChange = {
                            eventSelectorValue = it
                        },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .autoFocus(),
                    )
                }
            },
            onDismissRequest = {
                showCaptureScreenshotDlg = false
            },
            confirmButton = {
                TextButton(onClick = throttle {
                    if (appIdValue == store.screenshotTargetAppId && eventSelectorValue == store.screenshotEventSelector) {
                        showCaptureScreenshotDlg = false
                        return@throttle
                    }
                    if (appIdValue.isNotEmpty() && !appInfoMapFlow.value.contains(appIdValue)) {
                        toast(li.songe.gkd.i18n.t("k_34e21ea99c13"))
                        return@throttle
                    }
                    if (eventSelectorValue.isNotEmpty()) {
                        val s = Selector.parseOrNull(eventSelectorValue)
                        if (s == null) {
                            toast(li.songe.gkd.i18n.t("k_8c9fbc6ef927"))
                            return@throttle
                        }
                    }
                    storeFlow.update {
                        it.copy(
                            screenshotTargetAppId = appIdValue,
                            screenshotEventSelector = eventSelectorValue,
                        )
                    }
                    toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                    showCaptureScreenshotDlg = false
                }) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_b56d9ac6c5a0"),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureScreenshotDlg = false }) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_4d0b4688c787"),
                    )
                }
            })
    }
    var showHttpSettingDlg by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    PerfIconButton(imageVector = PerfIcon.ArrowBack, onClick = {
                        mainVm.popPage()
                    })
                },
                title = { Text(text = li.songe.gkd.i18n.t("k_dd07e641ca66")) },
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .titleItemPadding(showTop = false),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier,
                    text = "Shizuku",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                PerfIcon(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClickLabel = li.songe.gkd.i18n.t("k_3b8328c91c81"), onClick = throttle {
                            showShizukuState = true
                        })
                        .iconTextSize(textStyle = MaterialTheme.typography.titleSmall),
                    imageVector = PerfIcon.Api,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = li.songe.gkd.i18n.t("k_74c184306108"),
                )
            }
            val shizukuGranted by shizukuGrantedState.stateFlow.collectAsState()
            AnimatedVisibility(store.enableShizuku && !shizukuGranted) {
                AuthCard(
                    title = li.songe.gkd.i18n.t("k_86bddceb9d5f"),
                    subtitle = li.songe.gkd.i18n.t("k_a7bd6fc9bbb6"),
                    onAuthClick = {
                        mainVm.requestShizuku()
                    }
                )
            }
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_6b0ad26edfa3"),
                subtitle = li.songe.gkd.i18n.t("k_a3e561c7c6db"),
                suffix = li.songe.gkd.i18n.t("k_c0c78a429097"),
                suffixUnderline = true,
                onSuffixClick = { mainVm.navigateWebPage(ShortUrlSet.URL14) },
                checked = store.enableShizuku,
                suffixIcon = {
                    if (updateBinderMutex.state.collectAsState().value) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp),
                        )
                    }
                },
                onCheckedChange = {
                    mainVm.switchEnableShizuku(it)
                },
                onClick = null,
            )

            val server by HttpService.httpServerFlow.collectAsState()
            val httpServerRunning = server != null
            val localNetworkIps by HttpService.localNetworkIpsFlow.collectAsState()

            Text(
                text = "HTTP",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_6a43e3e09d7e"),
                subtitle = li.songe.gkd.i18n.t("k_e330f2e53cef"),
                suffixIcon = {
                    PerfCustomIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = li.songe.gkd.i18n.t("k_66b10cf5e534"),
                        onClick = { showHttpSettingDlg = !showHttpSettingDlg },
                        id = R.drawable.ic_page_info,
                        contentDescription = li.songe.gkd.i18n.t("k_c5f42f5a0f9e"),
                        tint = if (showHttpSettingDlg) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                },
                checked = httpServerRunning,
                onCheckedChange = throttle(fn = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        HttpService.start()
                    } else {
                        HttpService.stop()
                    }
                })
            )
            AnimatedVisibility(visible = httpServerRunning) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    Column(
                        modifier = Modifier.itemPadding()
                    ) {
                        Text(text = li.songe.gkd.i18n.t("k_354462977b15"))
                        Row {
                            val localUrl = "http://127.0.0.1:${store.httpServerPort}"
                            Text(
                                text = localUrl,
                                color = MaterialTheme.colorScheme.primary,
                                style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                modifier = Modifier.clickable(onClick = throttle {
                                    mainVm.openUrl(localUrl)
                                }),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = li.songe.gkd.i18n.t("k_89738e0ca1be"))
                        }
                        localNetworkIps.forEach { host ->
                            val lanUrl = "http://${host}:${store.httpServerPort}"
                            Text(
                                text = lanUrl,
                                color = MaterialTheme.colorScheme.primary,
                                style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                modifier = Modifier.clickable(onClick = throttle {
                                    mainVm.openUrl(lanUrl)
                                })
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = showHttpSettingDlg) {
                Column {
                    SettingItem(
                        title = li.songe.gkd.i18n.t("k_6f77ee7c5c99"),
                        subtitle = store.httpServerPort.toString(),
                        imageVector = PerfIcon.Edit,
                        onClickLabel = li.songe.gkd.i18n.t("k_07a62b1e96aa"),
                        onClick = {
                            showHttpSettingDlg = false
                            showEditPortDlg = true
                        }
                    )
                    TextSwitch(
                        title = li.songe.gkd.i18n.t("k_6b582fbb9dba"),
                        subtitle = li.songe.gkd.i18n.t("k_97424615a70e"),
                        checked = store.autoClearMemorySubs,
                        onCheckedChange = {
                            storeFlow.update {
                                it.copy(autoClearMemorySubs = !it.autoClearMemorySubs)
                            }
                        }
                    )
                }
            }

            Text(
                text = li.songe.gkd.i18n.t("k_83caf1badce1"),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingItem(
                title = li.songe.gkd.i18n.t("k_26c9e586fcb9"),
                subtitle = li.songe.gkd.i18n.t("k_8eddb6bd87e7"),
                onClick = {
                    mainVm.navigatePage(SnapshotPageRoute)
                }
            )

            if (!AndroidTarget.R) {
                val screenshotRunning by ScreenshotService.isRunning.collectAsState()
                TextSwitch(
                    title = li.songe.gkd.i18n.t("k_df95c4025b9c"),
                    subtitle = li.songe.gkd.i18n.t("k_0933e86c0e76"),
                    checked = screenshotRunning,
                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, notificationState)
                            val mediaProjectionManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            val activityResult =
                                context.launcher.launchForResult(mediaProjectionManager.createScreenCaptureIntent())
                            if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
                                ScreenshotService.start(intent = activityResult.data!!)
                            }
                        } else {
                            ScreenshotService.stop()
                        }
                    }
                )
            }

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_addb3c2ba231"),
                subtitle = li.songe.gkd.i18n.t("k_ef5f9af6036d"),
                checked = ButtonService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        ButtonService.start()
                    } else {
                        ButtonService.stop()
                    }
                },
            )

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_97f98cd92266"),
                subtitle = li.songe.gkd.i18n.t("k_7790dc931f09"),
                checked = store.captureVolumeChange,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        captureVolumeChange = it
                    )
                },
            )

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_ee5db675e1f0"),
                subtitle = li.songe.gkd.i18n.t("k_492729f7f9ca"),
                checked = store.captureScreenshot,
                suffixIcon = {
                    PerfCustomIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = li.songe.gkd.i18n.t("k_c41137ca16f9"),
                        onClick = throttle {
                            showCaptureScreenshotDlg = true
                        },
                        id = R.drawable.ic_page_info,
                        contentDescription = li.songe.gkd.i18n.t("k_bd7b6c1ed157"),
                    )
                },
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        captureScreenshot = it
                    )
                    if (it && store.screenshotTargetAppId.isEmpty() || store.screenshotEventSelector.isEmpty()) {
                        toast(li.songe.gkd.i18n.t("k_c456ae248712"))
                    }
                }
            )

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_c7dddf757afe"),
                subtitle = li.songe.gkd.i18n.t("k_37fbc765c690"),
                checked = store.hideSnapshotStatusBar,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        hideSnapshotStatusBar = it
                    )
                }
            )

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_108a9199f280"),
                subtitle = li.songe.gkd.i18n.t("k_24feb0f040e9"),
                checked = store.showSaveSnapshotToast,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        showSaveSnapshotToast = it
                    )
                }
            )

            SettingItem(
                title = "Github Cookie",
                subtitle = li.songe.gkd.i18n.t("k_58a79cef9954"),
                suffix = li.songe.gkd.i18n.t("k_ecc8ae752f3b"),
                suffixUnderline = true,
                onSuffixClick = {
                    mainVm.navigateWebPage(ShortUrlSet.URL1)
                },
                imageVector = PerfIcon.Edit,
                onClick = {
                    mainVm.showEditCookieDlgFlow.value = true
                }
            )

            Text(
                text = li.songe.gkd.i18n.t("k_4de50894b8c1"),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            SettingItem(
                title = li.songe.gkd.i18n.t("k_48ff47e21fda"),
                subtitle = li.songe.gkd.i18n.t("k_3e5e447fd343"),
                onClick = {
                    mainVm.navigatePage(ActivityLogRoute)
                }
            )
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_fcfcb10e4c90"),
                subtitle = li.songe.gkd.i18n.t("k_6c572506c2c6"),
                checked = ActivityService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        ActivityService.start()
                    } else {
                        ActivityService.stop()
                    }
                }
            )
            SettingItem(
                title = li.songe.gkd.i18n.t("k_12b64fb2df35"),
                subtitle = li.songe.gkd.i18n.t("k_69dc314d81b5"),
                onClick = {
                    mainVm.navigatePage(A11yEventLogRoute)
                }
            )
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_25af58e6873e"),
                subtitle = li.songe.gkd.i18n.t("k_8d864071da95"),
                checked = EventService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        EventService.start()
                    } else {
                        EventService.stop()
                    }
                }
            )

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}
