package li.songe.gkd.ui.home

import android.view.KeyEvent
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.ignoreBatteryOptimizationsState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.service.StatusService
import li.songe.gkd.service.TrackService
import li.songe.gkd.service.fixRestartAutomatorService
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.AboutRoute
import li.songe.gkd.ui.AdvancedPageRoute
import li.songe.gkd.ui.BlockA11yAppListRoute
import li.songe.gkd.ui.component.CustomOutlinedTextField
import li.songe.gkd.ui.component.FullscreenDialog
import li.songe.gkd.ui.component.PerfCustomIconButton
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextListDialog
import li.songe.gkd.ui.component.TextMenu
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.autoFocus
import li.songe.gkd.ui.component.updateDialogOptions
import li.songe.gkd.ui.component.useScrollBehaviorState
import li.songe.gkd.ui.component.waitResult
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.iconTextSize
import li.songe.gkd.ui.style.itemHorizontalPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.BackupUtils
import li.songe.gkd.util.DarkThemeOption
import li.songe.gkd.util.LanguageOption
import li.songe.gkd.util.findOption
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.mapState
import li.songe.gkd.util.openAppDetailsSettings
import li.songe.gkd.util.saveFileToDownloads
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast

@Composable
fun useSettingsPage(): ScaffoldExt {
    val mainVm = LocalMainViewModel.current
    val context = LocalActivity.current as MainActivity
    val store by storeFlow.collectAsState()
    val vm = viewModel<HomeVm>()

    var showToastInputDlg by vm.showToastInputDlgFlow.asMutableState()

    if (showToastInputDlg) {
        var value by remember {
            mutableStateOf(store.actionToast)
        }
        val maxCharLen = 64
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = li.songe.gkd.i18n.t("k_5bf7ff408fc6"))
                    PerfIconButton(
                        imageVector = PerfIcon.HelpOutline,
                        contentDescription = li.songe.gkd.i18n.t("k_d88d6e6c2594"),
                        onClickLabel = li.songe.gkd.i18n.t("k_f93b6e228e6c"),
                        onClick = throttle {
                            showToastInputDlg = false
                            val confirmAction = {
                                mainVm.dialogFlow.value = null
                                showToastInputDlg = true
                            }
                            mainVm.dialogFlow.updateDialogOptions(
                                title = li.songe.gkd.i18n.t("k_d88d6e6c2594"),
                                text = li.songe.gkd.i18n.t("manual_action_toast_help"),
                                confirmAction = confirmAction,
                                onDismissRequest = confirmAction,
                            )
                        },
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = value,
                    placeholder = {
                        Text(text = li.songe.gkd.i18n.t("k_29207cc69597"))
                    },
                    onValueChange = {
                        value = it.take(maxCharLen)
                    },
                    supportingText = {
                        Text(
                            text = "${value.length} / $maxCharLen",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoFocus()
                )
            },
            onDismissRequest = { showToastInputDlg = false },
            confirmButton = {
                TextButton(enabled = value.isNotEmpty(), onClick = {
                    if (value != storeFlow.value.actionToast) {
                        storeFlow.update { it.copy(actionToast = value) }
                        toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                    }
                    showToastInputDlg = false
                }) {
                    Text(text = li.songe.gkd.i18n.t("k_b56d9ac6c5a0"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showToastInputDlg = false }) {
                    Text(text = li.songe.gkd.i18n.t("k_4d0b4688c787"))
                }
            }
        )
    }

    var showNotifTextInputDlg by vm.showNotifTextInputDlgFlow.asMutableState()
    if (showNotifTextInputDlg) {
        var titleValue by remember { mutableStateOf(store.customNotifTitle) }
        var textValue by remember { mutableStateOf(store.customNotifText) }
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = li.songe.gkd.i18n.t("k_ce7c7d71a7fc"))
                    PerfIconButton(
                        imageVector = PerfIcon.HelpOutline,
                        contentDescription = li.songe.gkd.i18n.t("k_d88d6e6c2594"),
                        onClickLabel = li.songe.gkd.i18n.t("k_f93b6e228e6c"),
                        onClick = throttle {
                            showNotifTextInputDlg = false
                            val confirmAction = {
                                mainVm.dialogFlow.value = null
                                showNotifTextInputDlg = true
                            }
                            mainVm.dialogFlow.updateDialogOptions(
                                title = li.songe.gkd.i18n.t("k_d88d6e6c2594"),
                                text = li.songe.gkd.i18n.t("manual_notif_text_help"),
                                confirmAction = confirmAction,
                                onDismissRequest = confirmAction,
                            )
                        },
                    )
                }
            },
            text = {
                val titleMaxLen = 32
                val textMaxLen = 64
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CustomOutlinedTextField(
                        label = { Text(li.songe.gkd.i18n.t("k_e6dc2df4a417")) },
                        value = titleValue,
                        placeholder = { Text(text = li.songe.gkd.i18n.t("k_d8eb8652a1ea")) },
                        onValueChange = {
                            titleValue = (if (it.length > titleMaxLen) it.take(titleMaxLen) else it)
                                .filter { c -> c !in "\n\r" }
                        },
                        supportingText = {
                            Text(
                                text = "${titleValue.length} / $titleMaxLen",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomOutlinedTextField(
                        label = { Text(li.songe.gkd.i18n.t("k_8344831e7c44")) },
                        value = textValue,
                        placeholder = { Text(text = li.songe.gkd.i18n.t("k_d8eb8652a1ea")) },
                        onValueChange = {
                            textValue = if (it.length > textMaxLen) it.take(textMaxLen) else it
                        },
                        supportingText = {
                            Text(
                                text = "${textValue.length} / $textMaxLen",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .autoFocus(),
                        contentPadding = PaddingValues(12.dp),
                    )
                }
            },
            onDismissRequest = {
                showNotifTextInputDlg = false
            },
            confirmButton = {
                TextButton(onClick = {
                    context.justHideSoftInput()
                    if (store.customNotifTitle != textValue || store.customNotifText != textValue) {
                        storeFlow.update {
                            it.copy(
                                customNotifTitle = titleValue,
                                customNotifText = textValue
                            )
                        }
                        toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                    }
                    showNotifTextInputDlg = false
                }) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_b56d9ac6c5a0"),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifTextInputDlg = false }) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_4d0b4688c787"),
                    )
                }
            })
    }


    var showA11yBlockDlg by vm.showA11yBlockDlgFlow.asMutableState()
    if (showA11yBlockDlg) {
        BlockA11yDialog(onDismissRequest = { showA11yBlockDlg = false })
    }
    if (vm.showBackupDlgFlow.collectAsState().value) {
        TextListDialog(
            onDismiss = { vm.showBackupDlgFlow.value = false },
            textList = listOf(
                li.songe.gkd.i18n.t("k_a26f1abe645c") to vm.viewModelScope.launchAsFn(Dispatchers.IO) {
                    val uri = context.pickFile("application/zip")
                    if (uri != null) {
                        BackupUtils.importBackUpData(uri)
                    }
                },
                li.songe.gkd.i18n.t("k_d11b66842a9c") to {
                    vm.showExportBackupDlgFlow.value = true
                },
            )
        )
    }
    if (vm.showExportBackupDlgFlow.collectAsState().value) {
        TextListDialog(
            onDismiss = { vm.showExportBackupDlgFlow.value = false },
            textList = listOf(
                li.songe.gkd.i18n.t("k_ad1a01b57aef") to vm.viewModelScope.launchAsFn(Dispatchers.IO) {
                    val file = BackupUtils.exportBackUpData()
                    context.shareFile(file, li.songe.gkd.i18n.t("k_475c9ebcff83"))
                },
                li.songe.gkd.i18n.t("k_973f07187d90") to vm.viewModelScope.launchAsFn(Dispatchers.IO) {
                    val file = BackupUtils.exportBackUpData()
                    context.saveFileToDownloads(file)
                },
            )
        )
    }

    val scrollKey = rememberSaveable { mutableIntStateOf(0) }
    val (scrollBehavior, scrollState) = useScrollBehaviorState(scrollKey)
    LaunchedEffect(null) {
        mainVm.resetPageScrollEvent.collect {
            if (it == BottomNavItem.Settings) {
                scrollKey.intValue++
            }
        }
    }
    return ScaffoldExt(
        navItem = BottomNavItem.Settings,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = BottomNavItem.Settings.label,
                    )
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {

            Text(
                text = li.songe.gkd.i18n.t("k_f1484fa78b69"),
                modifier = Modifier.titleItemPadding(showTop = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val showToastSettingsDlg by vm.showToastSettingsDlgFlow.asMutableState()
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_5bf7ff408fc6"),
                subtitle = store.actionToast,
                checked = store.toastWhenClick,
                onClickLabel = li.songe.gkd.i18n.t("k_2313ce7d2230"),
                onClick = {
                    showToastInputDlg = true
                },
                suffixIcon = {
                    PerfCustomIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = li.songe.gkd.i18n.t("k_be1e5b4074ef"),
                        onClick = { vm.showToastSettingsDlgFlow.update { !it } },
                        id = R.drawable.ic_page_info,
                        contentDescription = li.songe.gkd.i18n.t("k_aaa3b3883f13"),
                        tint = if (showToastSettingsDlg) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                },
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        toastWhenClick = it
                    )
                })

            AnimatedVisibility(visible = showToastSettingsDlg) {
                Column {
                    TextSwitch(
                        title = li.songe.gkd.i18n.t("k_29ea25d303f4"),
                        subtitle = li.songe.gkd.i18n.t("k_565e1ff87ac2"),
                        suffix = li.songe.gkd.i18n.t("k_aa3e8507f9b1"),
                        onSuffixClick = {
                            mainVm.dialogFlow.updateDialogOptions(
                                title = li.songe.gkd.i18n.t("k_1b2219a307a1"),
                                text = li.songe.gkd.i18n.t("k_5118a8094418"),
                            )
                        },
                        checked = store.useSystemToast,
                        onCheckedChange = {
                            storeFlow.value = store.copy(
                                useSystemToast = it
                            )
                        })
                    TextSwitch(
                        title = li.songe.gkd.i18n.t("k_6a1b839874f4"),
                        subtitle = li.songe.gkd.i18n.t("k_e78582fda34f"),
                        checked = TrackService.isRunning.collectAsState().value,
                        onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                            if (it) {
                                mainVm.dialogFlow.waitResult(
                                    title = li.songe.gkd.i18n.t("k_59e2c8e61dcd"),
                                    text = li.songe.gkd.i18n.t("k_881aca9e2390"),
                                    confirmText = li.songe.gkd.i18n.t("k_1fc1afc5c55e"),
                                )
                                requiredPermission(context, foregroundServiceSpecialUseState)
                                requiredPermission(context, notificationState)
                                requiredPermission(context, canDrawOverlaysState)
                                TrackService.start()
                            } else {
                                TrackService.stop()
                            }
                        }
                    )
                }
            }

            val subsStatus by vm.subsStatusFlow.collectAsState()
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_ce7c7d71a7fc"),
                subtitle = if (store.useCustomNotifText) {
                    store.customNotifTitle + " / " + store.customNotifText
                } else {
                    subsStatus
                },
                checked = store.useCustomNotifText,
                onClickLabel = li.songe.gkd.i18n.t("k_ac04fdc8b3d9"),
                onClick = { showNotifTextInputDlg = true },
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        useCustomNotifText = it
                    )
                })

            TextSwitch(
                title = li.songe.gkd.i18n.t("k_8c91b02262ba"),
                subtitle = li.songe.gkd.i18n.t("k_95a85cd30d36"),
                checked = store.excludeFromRecents,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        mainVm.dialogFlow.waitResult(
                            title = li.songe.gkd.i18n.t("k_8c91b02262ba"),
                            text = li.songe.gkd.i18n.t("k_7834885df6aa"),
                            confirmText = li.songe.gkd.i18n.t("k_1fc1afc5c55e"),
                        )
                    }
                    storeFlow.value = store.copy(
                        excludeFromRecents = !store.excludeFromRecents
                    )
                })

            val scope = rememberCoroutineScope()
            val lazyOn = remember {
                storeFlow.mapState(scope) { it.enableBlockA11yAppList }.debounce(300)
                    .stateIn(scope, SharingStarted.Eagerly, store.enableBlockA11yAppList)
            }.collectAsState()
            AnimatedVisibility(visible = lazyOn.value) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .titleItemPadding(),
                    text = li.songe.gkd.i18n.t("k_04c62c8f3d82"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextSwitch(
                title = li.songe.gkd.i18n.t("k_86613e925dea"),
                subtitle = li.songe.gkd.i18n.t("k_2e268f4e6417"),
                checked = store.enableBlockA11yAppList && shizukuContextFlow.collectAsState().value.ok,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        showA11yBlockDlg = true
                    } else {
                        storeFlow.value = store.copy(enableBlockA11yAppList = false)
                        fixRestartAutomatorService()
                    }
                },
            )
            AnimatedVisibility(visible = lazyOn.value) {
                SettingItem(title = li.songe.gkd.i18n.t("k_8f74cd015bef"), onClickLabel = li.songe.gkd.i18n.t("k_7e4c9176baeb"), onClick = {
                    mainVm.navigatePage(BlockA11yAppListRoute)
                })
            }

            Text(
                text = li.songe.gkd.i18n.t("k_09b58aa3422c"),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            TextMenu(
                title = li.songe.gkd.i18n.t("k_750642ccd6d4"),
                option = DarkThemeOption.objects.findOption(store.enableDarkTheme),
                onOptionChange = {
                    storeFlow.update { s -> s.copy(enableDarkTheme = it.value) }
                }
            )

            TextMenu(
                title = "Language",
                option = LanguageOption.objects.findOption(store.language),
                onOptionChange = {
                    storeFlow.update { s -> s.copy(language = it.value) }
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(it.value)
                    )
                }
            )

            if (AndroidTarget.S) {
                TextSwitch(
                    title = li.songe.gkd.i18n.t("k_a75357bc3501"),
                    checked = store.enableDynamicColor,
                    onCheckedChange = {
                        storeFlow.update { s -> s.copy(enableDynamicColor = it) }
                    }
                )
            }

            Text(
                text = li.songe.gkd.i18n.t("k_1a26edf94a81"),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingItem(title = li.songe.gkd.i18n.t("k_dd07e641ca66"), onClick = {
                mainVm.navigatePage(AdvancedPageRoute)
            })
            SettingItem(title = li.songe.gkd.i18n.t("k_8233960bfd9a"), onClick = {
                vm.showBackupDlgFlow.value = true
            })

            SettingItem(title = li.songe.gkd.i18n.t("k_bed172efc953"), onClick = {
                mainVm.navigatePage(AboutRoute)
            })

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}

@Composable
private fun BlockA11yDialog(onDismissRequest: () -> Unit) = FullscreenDialog(onDismissRequest) {
    val mainVm = LocalMainViewModel.current
    val statusRunning by StatusService.isRunning.collectAsState()
    val shizukuContext by shizukuContextFlow.collectAsState()
    val ignoreBatteryOptimizations by ignoreBatteryOptimizationsState.stateFlow.collectAsState()
    val context = LocalActivity.current as MainActivity
    Scaffold(
        topBar = {
            PerfTopAppBar(
                navigationIcon = {
                    PerfIconButton(
                        imageVector = PerfIcon.Close,
                        onClickLabel = li.songe.gkd.i18n.t("k_81f18d4d4814"),
                        onClick = onDismissRequest,
                    )
                },
                title = {
                    Text(text = li.songe.gkd.i18n.t("k_86613e925dea"))
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    enabled = shizukuContext.ok && statusRunning && ignoreBatteryOptimizations,
                    onClick = mainVm.viewModelScope.launchAsFn {
                        onDismissRequest()
                        delay(200)
                        storeFlow.update { it.copy(enableBlockA11yAppList = true) }
                    }
                ) {
                    Text(text = li.songe.gkd.i18n.t("k_1fc1afc5c55e"))
                }
                Spacer(modifier = Modifier.width(itemHorizontalPadding))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = itemHorizontalPadding)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                Text(text = li.songe.gkd.i18n.t("k_be7bf1f6b39f"))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = li.songe.gkd.i18n.t("k_59e2c8e61dcd"), style = MaterialTheme.typography.titleMedium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RequiredTextItem(text = li.songe.gkd.i18n.t("k_cbdc0c0a7a64"))
                    RequiredTextItem(text = li.songe.gkd.i18n.t("k_ace5fab37400"))
                    RequiredTextItem(text = li.songe.gkd.i18n.t("k_610a7f9c6123"))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = li.songe.gkd.i18n.t("k_b412fa069ded"), style = MaterialTheme.typography.titleMedium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RequiredTextItem(
                        text = li.songe.gkd.i18n.t("k_0f0c48af67b6"),
                        enabled = !shizukuContext.ok,
                        imageVector = if (shizukuContext.ok) PerfIcon.Check else PerfIcon.ArrowForward,
                        onClick = mainVm.viewModelScope.launchAsFn(Dispatchers.IO) {
                            mainVm.guardShizukuContext()
                        },
                    )
                    RequiredTextItem(
                        text = li.songe.gkd.i18n.t("k_6325cb01afda"),
                        enabled = !statusRunning,
                        imageVector = if (statusRunning) PerfIcon.Check else PerfIcon.ArrowForward,
                        onClick = mainVm.viewModelScope.launchAsFn {
                            StatusService.requestStart(context)
                        },
                    )
                    RequiredTextItem(
                        text = li.songe.gkd.i18n.t("k_37cf171b6d8d"),
                        enabled = !ignoreBatteryOptimizations,
                        imageVector = if (ignoreBatteryOptimizations) PerfIcon.Check else PerfIcon.ArrowForward,
                        onClickLabel = li.songe.gkd.i18n.t("k_b2c141c8e179"),
                        onClick = mainVm.viewModelScope.launchAsFn {
                            requiredPermission(context, ignoreBatteryOptimizationsState)
                        },
                    )
                    RequiredTextItem(
                        text = li.songe.gkd.i18n.t("k_a31eb38058a9"),
                        enabled = true,
                        imageVector = PerfIcon.OpenInNew,
                        onClickLabel = li.songe.gkd.i18n.t("k_3714fd13744b"),
                        onClick = {
                            openAppDetailsSettings()
                        },
                    )
                    RequiredTextItem(
                        text = li.songe.gkd.i18n.t("k_5e5ac93cc0c5"),
                        enabled = true,
                        imageVector = PerfIcon.OpenInNew,
                        onClickLabel = li.songe.gkd.i18n.t("k_3714fd13744b"),
                        onClick = {
                            val m = shizukuContextFlow.value.inputManager
                            if (m != null) {
                                m.key(KeyEvent.KEYCODE_APP_SWITCH)
                            } else {
                                toast(li.songe.gkd.i18n.t("k_7b29e9051add"))
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = li.songe.gkd.i18n.t("k_d0cd80bc26b5"))
            }
            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}

@Composable
private fun RequiredTextItem(
    text: String,
    imageVector: ImageVector? = null,
    enabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .run {
                if (onClick != null) {
                    clickable(
                        enabled = enabled,
                        onClick = throttle(onClick),
                        onClickLabel = onClickLabel
                    )
                } else {
                    this
                }
            }
            .padding(horizontal = 4.dp),
    ) {
        val lineHeightDp = LocalDensity.current.run { LocalTextStyle.current.lineHeight.toDp() }
        Spacer(
            modifier = Modifier
                .padding(vertical = (lineHeightDp - 4.dp) / 2)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
                .size(4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
        if (imageVector != null) {
            PerfIcon(
                imageVector = imageVector,
                modifier = Modifier.iconTextSize(),
            )
        }
    }

}
