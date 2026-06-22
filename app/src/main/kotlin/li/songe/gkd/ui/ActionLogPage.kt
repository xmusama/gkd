package li.songe.gkd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import li.songe.gkd.data.ActionLog
import li.songe.gkd.data.ExcludeData
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.AppNameText
import li.songe.gkd.ui.component.EmptyText
import li.songe.gkd.ui.component.FixedTimeText
import li.songe.gkd.ui.component.GroupNameText
import li.songe.gkd.ui.component.LocalNumberCharWidth
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.TowLineText
import li.songe.gkd.ui.component.animateListItem
import li.songe.gkd.ui.component.measureNumberTextWidth
import li.songe.gkd.ui.component.useListScrollState
import li.songe.gkd.ui.component.useSubs
import li.songe.gkd.ui.component.waitResult
import li.songe.gkd.ui.share.ListPlaceholder
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.noRippleClickable
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.iconTextSize
import li.songe.gkd.ui.style.itemHorizontalPadding
import li.songe.gkd.ui.style.scaffoldPadding
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.mapState
import li.songe.gkd.util.subsMapFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast

@Serializable
data class ActionLogRoute(
    val subsId: Long? = null,
    val appId: String? = null,
) : NavKey

@Composable
fun ActionLogPage(route: ActionLogRoute) {
    val subsId = route.subsId
    val appId = route.appId
    val mainVm = LocalMainViewModel.current
    val vm = viewModel { ActionLogVm(route) }
    val resetKey = rememberSaveable { mutableIntStateOf(0) }
    val list = vm.pagingDataFlow.collectAsLazyPagingItems()
    val (scrollBehavior, listState) = useListScrollState(resetKey, list.itemCount > 0)
    val timeTextWidth = measureNumberTextWidth(MaterialTheme.typography.bodySmall)

    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        PerfTopAppBar(
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                PerfIconButton(
                    imageVector = PerfIcon.ArrowBack,
                    onClick = {
                        mainVm.popPage()
                    },
                )
            },
            title = {
                val title = li.songe.gkd.i18n.t("k_50532745b5a4")
                val titleModifier = Modifier.noRippleClickable {
                    resetKey.intValue++
                }
                if (subsId != null) {
                    TowLineText(
                        title = title,
                        subtitle = useSubs(subsId)?.name ?: subsId.toString(),
                        modifier = titleModifier,
                    )
                } else if (appId != null) {
                    TowLineText(
                        title = title,
                        subtitle = appId,
                        showApp = true,
                        modifier = titleModifier,
                    )
                } else {
                    Text(
                        text = title,
                        modifier = titleModifier,
                    )
                }
            },
            actions = {
                if (list.itemCount > 0) {
                    PerfIconButton(
                        imageVector = PerfIcon.Delete,
                        onClick = throttle(fn = mainVm.viewModelScope.launchAsFn {
                            val text = if (subsId != null) {
                                li.songe.gkd.i18n.t("k_1e540d190ce6")
                            } else if (appId != null) {
                                li.songe.gkd.i18n.t("k_c3a7a4825622")
                            } else {
                                li.songe.gkd.i18n.t("k_cffd230efd90")
                            }
                            mainVm.dialogFlow.waitResult(
                                title = li.songe.gkd.i18n.t("k_8f22c9908ed4"),
                                text = text,
                                error = true,
                            )
                            if (subsId != null) {
                                DbSet.actionLogDao.deleteSubsAll(subsId)
                            } else if (appId != null) {
                                DbSet.actionLogDao.deleteAppAll(appId)
                            } else {
                                DbSet.actionLogDao.deleteAll()
                            }
                            toast(li.songe.gkd.i18n.t("k_86e8d12a79b3"))
                        })
                    )
                }
            })
    }, content = { contentPadding ->
        CompositionLocalProvider(
            LocalNumberCharWidth provides timeTextWidth
        ) {
            LazyColumn(
                modifier = Modifier.scaffoldPadding(contentPadding),
                state = listState,
            ) {
                items(
                    count = list.itemCount,
                    key = list.itemKey { c -> c.first.id }
                ) { i ->
                    val item = list[i] ?: return@items
                    val lastItem = if (i > 0) list[i - 1] else null
                    ActionLogCard(
                        modifier = Modifier.animateListItem(),
                        i = i,
                        item = item,
                        lastItem = lastItem,
                        onClick = {
                            vm.showActionLogFlow.value = item.first
                        },
                        subsId = subsId,
                        appId = appId,
                    )
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    Spacer(modifier = Modifier.height(EmptyHeight))
                    if (list.itemCount == 0 && list.loadState.refresh !is LoadState.Loading) {
                        EmptyText(text = li.songe.gkd.i18n.t("k_b246458f20c8"))
                    }
                }
            }
        }
    })

    vm.showActionLogFlow.collectAsState().value?.let {
        ActionLogDialog(
            vm = vm,
            actionLog = it,
            onDismissRequest = {
                vm.showActionLogFlow.value = null
            }
        )
    }
}


@Composable
private fun ActionLogCard(
    modifier: Modifier = Modifier,
    i: Int,
    item: Triple<ActionLog, RawSubscription.RawGroupProps?, RawSubscription.RawRuleProps?>,
    lastItem: Triple<ActionLog, RawSubscription.RawGroupProps?, RawSubscription.RawRuleProps?>?,
    onClick: () -> Unit,
    subsId: Long?,
    appId: String?,
) {
    val mainVm = LocalMainViewModel.current
    val (actionLog, group, rule) = item
    val lastActionLog = lastItem?.first
    val isDiffApp = actionLog.appId != lastActionLog?.appId
    val verticalPadding = if (i == 0) 0.dp else if (isDiffApp) 12.dp else 8.dp
    val subsIdToRaw by subsMapFlow.collectAsState()
    val subscription = subsIdToRaw[actionLog.subsId]
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = itemHorizontalPadding / 2,
                end = itemHorizontalPadding / 2,
                top = verticalPadding
            )
    ) {
        if (isDiffApp && appId == null) {
            Row(
                modifier = Modifier
                    .padding(start = itemHorizontalPadding / 4)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = throttle {
                        mainVm.navigatePage(
                            AppConfigRoute(
                                appId = actionLog.appId,
                            )
                        )
                    })
                    .fillMaxWidth()
                    .padding(start = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    Spacer(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .size(4.dp)
                    )
                    AppNameText(appId = actionLog.appId, modifier = Modifier.weight(1f))
                    PerfIcon(
                        imageVector = PerfIcon.KeyboardArrowRight,
                        modifier = Modifier
                            .iconTextSize()
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(start = itemHorizontalPadding / 4)
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = itemHorizontalPadding / 4)
        ) {
            if (appId == null) {
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                FixedTimeText(
                    text = actionLog.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    val showActivityId = actionLog.showActivityId
                    if (showActivityId != null) {
                        Text(
                            text = showActivityId,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis,
                        )
                    } else {
                        Text(
                            text = "null",
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                        )
                    }
                    if (subsId == null) {
                        Row {
                            Text(text = subscription?.name ?: "id=${actionLog.subsId}")
                            val lineHeightDp = LocalDensity.current.run {
                                LocalTextStyle.current.lineHeight.toDp()
                            }
                            Row(
                                modifier = Modifier
                                    .height(lineHeightDp)
                                    .padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "v${item.first.subsVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 2.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val groupDesc = group?.name.toString()
                        val textColor = LocalContentColor.current.let {
                            if (group?.name == null) it.copy(alpha = 0.5f) else it
                        }
                        GroupNameText(
                            isGlobal = actionLog.groupType == SubsConfig.GlobalGroupType,
                            text = groupDesc,
                            color = textColor,
                        )
                        val ruleDesc = rule?.name ?: (if ((group?.rules?.size ?: 0) > 1) {
                            val keyDesc = actionLog.ruleKey?.let { "key=$it, " } ?: ""
                            "${keyDesc}index=${actionLog.ruleIndex}"
                        } else {
                            null
                        })
                        if (ruleDesc != null) {
                            Text(
                                text = ruleDesc,
                                modifier = Modifier.padding(start = 8.dp),
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionLogDialog(
    vm: ViewModel,
    actionLog: ActionLog,
    onDismissRequest: () -> Unit,
) {
    val mainVm = LocalMainViewModel.current
    val scope = rememberCoroutineScope()
    val subsConfig = remember(actionLog) {
        (if (actionLog.groupType == SubsConfig.AppGroupType) {
            DbSet.subsConfigDao.queryAppGroupTypeConfig(
                actionLog.subsId, actionLog.appId, actionLog.groupKey
            )
        } else {
            DbSet.subsConfigDao.queryGlobalGroupTypeConfig(actionLog.subsId, actionLog.groupKey)
        }).stateIn(vm.viewModelScope, SharingStarted.Eagerly, null)
    }.collectAsState().value

    val oldExclude = remember(subsConfig?.exclude) {
        ExcludeData.parse(subsConfig?.exclude)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            ItemText(
                text = li.songe.gkd.i18n.t("k_ff668f83d5ba"),
                onClick = {
                    onDismissRequest()
                    if (actionLog.groupType == SubsConfig.AppGroupType) {
                        mainVm.navigatePage(
                            SubsAppGroupListRoute(
                                actionLog.subsId, actionLog.appId, actionLog.groupKey
                            )
                        )
                    } else if (actionLog.groupType == SubsConfig.GlobalGroupType) {
                        mainVm.navigatePage(
                            SubsGlobalGroupListRoute(
                                actionLog.subsId, actionLog.groupKey
                            )
                        )
                    }
                }
            )
            HorizontalDivider()

            if (actionLog.groupType == SubsConfig.GlobalGroupType) {
                val subs = remember(actionLog.subsId) {
                    subsMapFlow.mapState(scope) { it[actionLog.subsId] }
                }.collectAsState().value
                val group = subs?.globalGroups?.find { g -> g.key == actionLog.groupKey }
                val appChecked = if (group != null) {
                    getGlobalGroupChecked(
                        subs,
                        oldExclude,
                        group,
                        actionLog.appId,
                    )
                } else {
                    null
                }
                if (appChecked != null) {
                    ItemText(
                        text = if (appChecked) li.songe.gkd.i18n.t("k_cda492558366") else li.songe.gkd.i18n.t("k_d898e0730abf"),
                        onClick = vm.viewModelScope.launchAsFn {
                            val subsConfig = subsConfig ?: SubsConfig(
                                type = SubsConfig.GlobalGroupType,
                                subsId = actionLog.subsId,
                                groupKey = actionLog.groupKey,
                            )
                            val newSubsConfig = subsConfig.copy(
                                exclude = oldExclude
                                    .copy(
                                        appIds = oldExclude.appIds
                                            .toMutableMap()
                                            .apply {
                                                set(actionLog.appId, appChecked)
                                            })
                                    .stringify()
                            )
                            DbSet.subsConfigDao.insert(newSubsConfig)
                            toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (actionLog.activityId != null) {
                val disabled =
                    oldExclude.activityIds.contains(actionLog.appId to actionLog.activityId)
                ItemText(
                    text = if (disabled) li.songe.gkd.i18n.t("k_c9be3b44237c") else li.songe.gkd.i18n.t("k_d66870c0559e"),
                    onClick = vm.viewModelScope.launchAsFn {
                        val subsConfig = if (actionLog.groupType == SubsConfig.AppGroupType) {
                            subsConfig ?: SubsConfig(
                                type = SubsConfig.AppGroupType,
                                subsId = actionLog.subsId,
                                appId = actionLog.appId,
                                groupKey = actionLog.groupKey,
                            )
                        } else {
                            subsConfig ?: SubsConfig(
                                type = SubsConfig.GlobalGroupType,
                                subsId = actionLog.subsId,
                                groupKey = actionLog.groupKey,
                            )
                        }
                        val newSubsConfig = subsConfig.copy(
                            exclude = oldExclude
                                .switch(
                                    actionLog.appId,
                                    actionLog.activityId
                                )
                                .stringify()
                        )
                        DbSet.subsConfigDao.insert(newSubsConfig)
                        toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ItemText(
    text: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .clickable(onClick = throttle(onClick))
        .fillMaxWidth()
        .padding(16.dp)
    Text(
        modifier = modifier,
        text = text,
        color = color,
    )
}
