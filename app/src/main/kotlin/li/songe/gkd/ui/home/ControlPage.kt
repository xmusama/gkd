package li.songe.gkd.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.permission.appOpsRestrictedFlow
import li.songe.gkd.permission.writeSecureSettingsState
import li.songe.gkd.service.A11yService
import li.songe.gkd.service.ActivityService
import li.songe.gkd.service.StatusService
import li.songe.gkd.service.a11yPartDisabledFlow
import li.songe.gkd.service.switchAutomatorService
import li.songe.gkd.service.topAppIdFlow
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.shizuku.uiAutomationFlow
import li.songe.gkd.store.actualA11yScopeAppList
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.ActionLogRoute
import li.songe.gkd.ui.ActivityLogRoute
import li.songe.gkd.ui.AppConfigRoute
import li.songe.gkd.ui.AuthA11yRoute
import li.songe.gkd.ui.WebViewRoute
import li.songe.gkd.ui.component.GroupNameText
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfSwitch
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.textSize
import li.songe.gkd.ui.component.useScrollBehaviorState
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemHorizontalPadding
import li.songe.gkd.ui.style.itemVerticalPadding
import li.songe.gkd.ui.style.surfaceCardColors
import li.songe.gkd.util.HOME_PAGE_URL
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.latestRecordDescFlow
import li.songe.gkd.util.latestRecordFlow
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.throttle

@Composable
fun useControlPage(): ScaffoldExt {
    val context = LocalActivity.current as MainActivity
    val mainVm = LocalMainViewModel.current
    val vm = viewModel<HomeVm>()
    val scrollKey = rememberSaveable { mutableIntStateOf(0) }
    val (scrollBehavior, scrollState) = useScrollBehaviorState(scrollKey)
    LaunchedEffect(null) {
        mainVm.resetPageScrollEvent.collect {
            if (it == BottomNavItem.Control) {
                scrollKey.intValue++
            }
        }
    }
    return ScaffoldExt(
        navItem = BottomNavItem.Control,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(scrollBehavior = scrollBehavior, title = {
                Text(
                    text = stringResource(R.string.app_name)
                )
            }, actions = {
                PerfIconButton(
                    imageVector = PerfIcon.RocketLaunch,
                    onClickLabel = li.songe.gkd.i18n.t("k_bb296f384176"),
                    contentDescription = li.songe.gkd.i18n.t("k_f8b4c14ff903"),
                    onClick = throttle {
                        mainVm.navigatePage(AuthA11yRoute)
                    },
                )
            })
        }) { contentPadding ->
        val store by storeFlow.collectAsState()

        val a11yRunning by A11yService.isRunning.collectAsState()
        val manageRunning by StatusService.isRunning.collectAsState()
        val writeSecureSettings by writeSecureSettingsState.stateFlow.collectAsState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = itemHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(itemHorizontalPadding / 2)
        ) {
            if (appOpsRestrictedFlow.collectAsState().value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.onClick(label = li.songe.gkd.i18n.t("k_25e98173d173"), action = null)
                        },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    onClick = throttle {
                        mainVm.navigateWebPage(ShortUrlSet.URL2)
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(itemVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PerfIcon(imageVector = PerfIcon.WarningAmber)
                        Text(
                            modifier = Modifier.weight(1f),
                            text = li.songe.gkd.i18n.t("k_7917327c689f"),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        PerfIcon(imageVector = PerfIcon.KeyboardArrowRight)
                    }
                }
            }
            if (store.useA11y || actualA11yScopeAppList.contains(topAppIdFlow.collectAsState().value)) {
                PageSwitchItemCard(
                    imageVector = PerfIcon.Memory,
                    title = li.songe.gkd.i18n.t("k_d42da9e2ace9"),
                    subtitle = if (a11yRunning) {
                        li.songe.gkd.i18n.t("k_f106cde067c2")
                    } else if (mainVm.a11yServiceEnabledFlow.collectAsState().value) {
                        li.songe.gkd.i18n.t("k_653c78b0b0a6")
                    } else if (writeSecureSettings) {
                        if (store.enableAutomator && a11yPartDisabledFlow.collectAsState().value) {
                            li.songe.gkd.i18n.t("k_6b683e741bcb")
                        } else {
                            li.songe.gkd.i18n.t("k_a6fa6ad6cb84")
                        }
                    } else {
                        li.songe.gkd.i18n.t("k_9d1239c185c4")
                    },
                    checked = a11yRunning,
                    onCheckedChange = { newEnabled ->
                        if (newEnabled && !writeSecureSettingsState.value) {
                            mainVm.navigatePage(AuthA11yRoute)
                        } else {
                            switchAutomatorService()
                        }
                    },
                )
            } else {
                PageSwitchItemCard(
                    imageVector = PerfIcon.Memory,
                    title = li.songe.gkd.i18n.t("k_d42da9e2ace9"),
                    subtitle = if (uiAutomationFlow.collectAsState().value != null) {
                        li.songe.gkd.i18n.t("k_88ad0447cf6d")
                    } else if (!shizukuContextFlow.collectAsState().value.ok) {
                        li.songe.gkd.i18n.t("k_5ad8b271e978")
                    } else {
                        if (store.enableAutomator && a11yPartDisabledFlow.collectAsState().value) {
                            li.songe.gkd.i18n.t("k_544facd55388")
                        } else {
                            li.songe.gkd.i18n.t("k_a8ece9678b0e")
                        }
                    },
                    checked = uiAutomationFlow.collectAsState().value != null,
                    onCheckedChange = vm.viewModelScope.launchAsFn(Dispatchers.IO) { newEnabled ->
                        if (newEnabled) {
                            mainVm.guardShizukuContext()
                        }
                        switchAutomatorService()
                    },
                )
            }

            PageSwitchItemCard(
                imageVector = PerfIcon.Notifications,
                title = li.songe.gkd.i18n.t("k_ccecf0f93b12"),
                subtitle = li.songe.gkd.i18n.t("k_8ffb15fe7b95"),
                checked = manageRunning && store.enableStatusService,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        StatusService.requestStart(context)
                    } else {
                        StatusService.stop()
                        storeFlow.value = store.copy(
                            enableStatusService = false
                        )
                    }
                },
            )

            ServerStatusCard()

            PageItemCard(
                title = li.songe.gkd.i18n.t("k_50532745b5a4"),
                subtitle = li.songe.gkd.i18n.t("k_48ec1b5fd3ea"),
                imageVector = PerfIcon.History,
                onClickLabel = li.songe.gkd.i18n.t("k_dcdd69a9d715"),
                onClick = {
                    mainVm.navigatePage(ActionLogRoute())
                })

            if (ActivityService.isRunning.collectAsState().value) {
                PageItemCard(
                    title = li.songe.gkd.i18n.t("k_48ff47e21fda"),
                    subtitle = li.songe.gkd.i18n.t("k_8f63dbc43072"),
                    imageVector = PerfIcon.Layers,
                    onClickLabel = li.songe.gkd.i18n.t("k_01a10cb43ab4"),
                    onClick = {
                        mainVm.navigatePage(ActivityLogRoute)
                    })
            }

            PageItemCard(
                title = li.songe.gkd.i18n.t("k_eaa2069ca828"),
                subtitle = li.songe.gkd.i18n.t("k_a9d3ec7ec4ea"),
                imageVector = PerfIcon.HelpOutline,
                onClickLabel = li.songe.gkd.i18n.t("k_11b5b7a56c1c"),
                onClick = {
                    mainVm.navigatePage(WebViewRoute(initUrl = HOME_PAGE_URL))
                })
            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}


@Composable
private fun PageItemCard(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.onClick(label = onClickLabel, action = null)
            },
        shape = MaterialTheme.shapes.large,
        colors = surfaceCardColors,
        onClick = throttle(fn = onClick)
    ) {
        IconTextCard(
            imageVector = imageVector,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PageSwitchItemCard(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val onClick = throttle { onCheckedChange(!checked) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.onClick(label = li.songe.gkd.i18n.t("k_b96ab0fab650", title), action = null)
            },
        shape = MaterialTheme.shapes.large,
        colors = surfaceCardColors,
        onClick = onClick,
    ) {
        IconTextCard(
            imageVector = imageVector,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            PerfSwitch(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun IconTextCard(
    imageVector: ImageVector, content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(itemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PerfIcon(
            imageVector = imageVector,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(itemHorizontalPadding))
        content()
    }
}

@Composable
private fun ServerStatusCard() {
    val mainVm = LocalMainViewModel.current
    val vm = viewModel<HomeVm>()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                onClick(label = li.songe.gkd.i18n.t("k_4a192ce2868e"), action = null)
            }, shape = RoundedCornerShape(20.dp), colors = surfaceCardColors, onClick = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = itemVerticalPadding,
                    end = itemVerticalPadding,
                    top = itemVerticalPadding,
                    bottom = itemVerticalPadding / 2
                ), verticalAlignment = Alignment.CenterVertically
        ) {
            PerfIcon(
                imageVector = PerfIcon.Equalizer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(itemHorizontalPadding))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = li.songe.gkd.i18n.t("k_354077c7646a"),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val usedSubsItemCount by vm.usedSubsItemCountFlow.collectAsState()
                AnimatedVisibility(usedSubsItemCount > 0) {
                    Text(
                        text = li.songe.gkd.i18n.t("k_f0651ad04df2", usedSubsItemCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = itemVerticalPadding)
        ) {
            val subsStatus by vm.subsStatusFlow.collectAsState()
            AnimatedVisibility(subsStatus.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = subsStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val latestRecordDesc by latestRecordDescFlow.collectAsState()
            if (latestRecordDesc != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClickLabel = li.songe.gkd.i18n.t("k_3e9db10c1ccb"), onClick = throttle {
                            latestRecordFlow.value?.let {
                                mainVm.navigatePage(
                                    AppConfigRoute(
                                        appId = it.appId, focusLog = it
                                    )
                                )
                            }
                        })
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        GroupNameText(
                            modifier = Modifier.fillMaxWidth(),
                            preText = li.songe.gkd.i18n.t("k_cb28015338ea"),
                            isGlobal = latestRecordFlow.collectAsState().value?.groupType == SubsConfig.GlobalGroupType,
                            text = latestRecordDesc ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    PerfIcon(
                        imageVector = PerfIcon.KeyboardArrowRight,
                        modifier = Modifier.textSize(style = MaterialTheme.typography.bodyMedium),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(itemVerticalPadding))
        }
    }
}
