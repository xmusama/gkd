package li.songe.gkd.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.update
import li.songe.gkd.R
import li.songe.gkd.data.Value
import li.songe.gkd.db.DbSet
import li.songe.gkd.store.storeFlow
import li.songe.gkd.store.switchStoreEnableMatch
import li.songe.gkd.ui.SlowGroupRoute
import li.songe.gkd.ui.UpsertRuleGroupRoute
import li.songe.gkd.ui.WebViewRoute
import li.songe.gkd.ui.component.AnimationFloatingActionButton
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.ScaffoldDialog
import li.songe.gkd.ui.component.SubsItemCard
import li.songe.gkd.ui.component.TextMenu
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.usePinnedScrollBehaviorState
import li.songe.gkd.ui.component.waitResult
import li.songe.gkd.ui.share.ListPlaceholder
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.util.LOCAL_SUBS_ID
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.UpdateTimeOption
import li.songe.gkd.util.checkSubsUpdate
import li.songe.gkd.util.deleteSubscription
import li.songe.gkd.util.findOption
import li.songe.gkd.util.getUpDownTransform
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.mapState
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.subsItemsFlow
import li.songe.gkd.util.subsMapFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import li.songe.gkd.util.updateSubsMutex
import li.songe.gkd.util.usedSubsEntriesFlow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun useSubsManagePage(): ScaffoldExt {
    val mainVm = LocalMainViewModel.current

    val vm = viewModel<HomeVm>()
    val subItems by subsItemsFlow.collectAsState()
    val subsIdToRaw by subsMapFlow.collectAsState()

    var orderSubItems by remember {
        mutableStateOf(subItems)
    }
    LaunchedEffect(subItems) {
        orderSubItems = subItems
    }

    val refreshing by updateSubsMutex.state.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    var isSelectedMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    val draggedFlag = remember { Value(false) }
    LaunchedEffect(key1 = isSelectedMode) {
        if (!isSelectedMode && selectedIds.isNotEmpty()) {
            selectedIds = emptySet()
        }
    }
    BackHandler(isSelectedMode) {
        isSelectedMode = false
    }
    LaunchedEffect(key1 = subItems.size) {
        if (subItems.size <= 1) {
            isSelectedMode = false
        }
    }

    var showSettingsDlg by remember { mutableStateOf(false) }
    if (showSettingsDlg) {
        ScaffoldDialog(
            onClose = { showSettingsDlg = false },
            title = li.songe.gkd.i18n.t("k_65f3531c34a3"),
            content = {
                val store by storeFlow.collectAsState()
                TextMenu(
                    title = li.songe.gkd.i18n.t("k_ecae7085cec2"),
                    option = UpdateTimeOption.objects.findOption(store.updateSubsInterval)
                ) {
                    storeFlow.update { s -> s.copy(updateSubsInterval = it.value) }
                }
                TextSwitch(
                    title = li.songe.gkd.i18n.t("k_b151485175d7"),
                    subtitle = li.songe.gkd.i18n.t("k_19ce2aa525a6"),
                    checked = store.subsPowerWarn,
                    onCheckedChange = throttle<Boolean> {
                        storeFlow.update { s -> s.copy(subsPowerWarn = it) }
                    }
                )
            }
        )
    }

    val scrollKey = rememberSaveable { mutableIntStateOf(0) }
    val (scrollBehavior, lazyListState) = usePinnedScrollBehaviorState(scrollKey)
    LaunchedEffect(null) {
        mainVm.resetPageScrollEvent.collect {
            if (it == BottomNavItem.SubsManage) {
                scrollKey.intValue++
            }
        }
    }
    return ScaffoldExt(
        navItem = BottomNavItem.SubsManage,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(scrollBehavior = scrollBehavior, navigationIcon = {
                if (isSelectedMode) {
                    PerfIconButton(
                        imageVector = PerfIcon.Close,
                        contentDescription = li.songe.gkd.i18n.t("k_f02e9439542f"),
                        onClick = { isSelectedMode = false },
                    )
                }
            }, title = {
                if (isSelectedMode) {
                    Text(
                        text = if (selectedIds.isNotEmpty()) selectedIds.size.toString() else "",
                    )
                } else {
                    Text(
                        text = BottomNavItem.SubsManage.label,
                    )
                }
            }, actions = {
                var expanded by remember { mutableStateOf(false) }
                AnimatedContent(
                    targetState = isSelectedMode,
                    transitionSpec = { getUpDownTransform() },
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Row {
                        if (it) {
                            val canDeleteIds = if (selectedIds.contains(LOCAL_SUBS_ID)) {
                                selectedIds - LOCAL_SUBS_ID
                            } else {
                                selectedIds
                            }
                            if (canDeleteIds.isNotEmpty()) {
                                val text = li.songe.gkd.i18n.t("k_08c29306b7a2", canDeleteIds.size).let { s ->
                                    if (selectedIds.contains(LOCAL_SUBS_ID)) li.songe.gkd.i18n.t("k_9cfd5c9d5956", s) else s
                                }
                                PerfIconButton(
                                    imageVector = PerfIcon.Delete,
                                    contentDescription = li.songe.gkd.i18n.t("k_7c50210d699f"),
                                    onClick = vm.viewModelScope.launchAsFn {
                                        mainVm.dialogFlow.waitResult(
                                            title = li.songe.gkd.i18n.t("k_fe7b16b5c082"),
                                            text = text,
                                            error = true,
                                        )
                                        deleteSubscription(*canDeleteIds.toLongArray())
                                        selectedIds = selectedIds - canDeleteIds
                                        if (selectedIds.size == canDeleteIds.size) {
                                            isSelectedMode = false
                                        }
                                    },
                                )
                            }
                        } else {
                            val ruleSummary by ruleSummaryFlow.collectAsState()
                            AnimatedVisibility(
                                visible = ruleSummary.slowGroupCount > 0,
                                enter = scaleIn(),
                                exit = scaleOut(),
                            ) {
                                PerfIconButton(
                                    imageVector = PerfIcon.Eco,
                                    contentDescription = li.songe.gkd.i18n.t("k_dee6fc95175a"),
                                    onClickLabel = li.songe.gkd.i18n.t("k_00c3630ef424"),
                                    onClick = throttle {
                                        mainVm.navigatePage(SlowGroupRoute)
                                    })
                            }
                            val scope = rememberCoroutineScope()
                            val enableMatch by remember {
                                storeFlow.mapState(scope) { s -> s.enableMatch }
                            }.collectAsState()
                            PerfIconButton(
                                id = if (enableMatch) R.drawable.ic_flash_on else R.drawable.ic_flash_off,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (!enableMatch) {
                                        CheckboxDefaults.colors().checkedBoxColor
                                    } else {
                                        LocalContentColor.current
                                    }
                                ),
                                contentDescription = li.songe.gkd.i18n.t("k_06b0c3e743dd") + if (enableMatch) li.songe.gkd.i18n.t("k_25d284315063") else li.songe.gkd.i18n.t("k_0fe5a98e9f8c"),
                                onClickLabel = li.songe.gkd.i18n.t("k_d7ee5b2dac5a"),
                                onClick = throttle { switchStoreEnableMatch() },
                            )
                            PerfIconButton(
                                id = R.drawable.ic_page_info,
                                contentDescription = li.songe.gkd.i18n.t("k_65f3531c34a3"),
                                onClickLabel = li.songe.gkd.i18n.t("k_c184e4944d8a"),
                                onClick = {
                                    showSettingsDlg = true
                                })
                        }
                    }
                }
                PerfIconButton(
                    imageVector = PerfIcon.MoreVert,
                    contentDescription = li.songe.gkd.i18n.t("k_77836d3a9942"),
                    onClick = {
                        if (updateSubsMutex.mutex.isLocked) {
                            toast(li.songe.gkd.i18n.t("k_db8d309a8ede"))
                        } else {
                            expanded = true
                        }
                    })
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopStart)
                ) {
                    key(isSelectedMode) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (isSelectedMode) {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = li.songe.gkd.i18n.t("k_3e44b2a93338"))
                                    },
                                    onClick = {
                                        expanded = false
                                        selectedIds = subItems.map { it.id }.toSet()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = li.songe.gkd.i18n.t("k_ae0588041180"))
                                    },
                                    onClick = {
                                        expanded = false
                                        val newSelectedIds =
                                            subItems.map { it.id }.toSet() - selectedIds
                                        if (newSelectedIds.isEmpty()) {
                                            isSelectedMode = false
                                        }
                                        selectedIds = newSelectedIds
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(text = li.songe.gkd.i18n.t("k_0d9a428066cd")) },
                                    onClick = throttle {
                                        expanded = false
                                        mainVm.navigatePage(
                                            UpsertRuleGroupRoute(
                                                subsId = LOCAL_SUBS_ID,
                                                groupKey = null,
                                                appId = "",
                                                forward = true,
                                            )
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = li.songe.gkd.i18n.t("k_d039ea532e29")) },
                                    onClick = throttle {
                                        expanded = false
                                        mainVm.navigatePage(
                                            UpsertRuleGroupRoute(
                                                subsId = LOCAL_SUBS_ID,
                                                groupKey = null,
                                                appId = null,
                                                forward = true,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            })
        },
        floatingActionButton = {
            AnimationFloatingActionButton(
                contentDescription = li.songe.gkd.i18n.t("k_6debaa888532"),
                onClickLabel = li.songe.gkd.i18n.t("k_907c36fc9442"),
                visible = !isSelectedMode,
                onClick = {
                    if (updateSubsMutex.mutex.isLocked) {
                        toast(li.songe.gkd.i18n.t("k_2c20f3fd5e63"))
                    } else {
                        mainVm.viewModelScope.launchTry {
                            val url = mainVm.inputSubsLinkOption.getResult() ?: return@launchTry
                            mainVm.addOrModifySubs(url)
                        }
                    }
                },
                imageVector = PerfIcon.Add,
            )
        },
    ) { contentPadding ->
        val reorderableLazyColumnState =
            rememberReorderableLazyListState(lazyListState) { from, to ->
                orderSubItems = orderSubItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                    forEachIndexed { index, subsItem ->
                        if (subsItem.order != index) {
                            this[index] = subsItem.copy(order = index)
                        }
                    }
                }
                draggedFlag.value = true
            }
        PullToRefreshBox(
            modifier = Modifier.padding(contentPadding),
            state = pullToRefreshState,
            isRefreshing = refreshing,
            onRefresh = { checkSubsUpdate(true) }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(orderSubItems, { _, subItem -> subItem.id }) { index, subItem ->
                    val canDrag = !refreshing && orderSubItems.size > 1
                    ReorderableItem(
                        state = reorderableLazyColumnState,
                        key = subItem.id,
                        enabled = canDrag,
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        SubsItemCard(
                            modifier = Modifier.longPressDraggableHandle(
                                enabled = canDrag,
                                interactionSource = interactionSource,
                                onDragStarted = {
                                    if (orderSubItems.size > 1 && !isSelectedMode) {
                                        isSelectedMode = true
                                        selectedIds = setOf(subItem.id)
                                    }
                                },
                                onDragStopped = {
                                    if (draggedFlag.value) {
                                        draggedFlag.value = false
                                        isSelectedMode = false
                                        selectedIds = emptySet()
                                    }
                                    val changeItems = orderSubItems.filter { newItem ->
                                        subItems.find { oldItem -> oldItem.id == newItem.id }?.order != newItem.order
                                    }
                                    if (changeItems.isNotEmpty()) {
                                        vm.viewModelScope.launchTry {
                                            DbSet.subsItemDao.batchUpdateOrder(changeItems)
                                        }
                                    }
                                },
                            ),
                            interactionSource = interactionSource,
                            subsItem = subItem,
                            subscription = subsIdToRaw[subItem.id],
                            index = index + 1,
                            isSelectedMode = isSelectedMode,
                            isSelected = selectedIds.contains(subItem.id),
                            onCheckedChange = mainVm.viewModelScope.launchAsFn { checked ->
                                if (checked && storeFlow.value.subsPowerWarn && !subItem.isLocal && usedSubsEntriesFlow.value.any { !it.subsItem.isLocal }) {
                                    mainVm.dialogFlow.waitResult(
                                        title = li.songe.gkd.i18n.t("k_b151485175d7"),
                                        textContent = {
                                            Column {
                                                Text(text = li.songe.gkd.i18n.t("k_e8c5028e79aa"))
                                                Text(
                                                    text = li.songe.gkd.i18n.t("k_9454e9b90d79"),
                                                    modifier = Modifier.clickable(onClick = throttle {
                                                        mainVm.dialogFlow.value = null
                                                        mainVm.navigatePage(
                                                            WebViewRoute(
                                                                initUrl = ShortUrlSet.URL6
                                                            )
                                                        )
                                                    }),
                                                    textDecoration = TextDecoration.Underline,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        },
                                        confirmText = li.songe.gkd.i18n.t("k_9fbe751b883e"),
                                        error = true
                                    )
                                }
                                DbSet.subsItemDao.updateEnable(subItem.id, checked)
                            },
                            onSelectedChange = {
                                val newSelectedIds = if (selectedIds.contains(subItem.id)) {
                                    selectedIds.toMutableSet().apply {
                                        remove(subItem.id)
                                    }
                                } else {
                                    selectedIds + subItem.id
                                }
                                selectedIds = newSelectedIds
                                if (newSelectedIds.isEmpty()) {
                                    isSelectedMode = false
                                }
                            },
                        )
                    }
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    Spacer(modifier = Modifier.height(EmptyHeight))
                }
            }
        }
    }
}
