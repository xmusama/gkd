package li.songe.gkd.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.songe.gkd.ui.component.EmptyText
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.updateDialogOptions
import li.songe.gkd.ui.share.ListPlaceholder
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.scaffoldPadding
import li.songe.gkd.util.appInfoMapFlow
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.throttle

@Serializable
data object SlowGroupRoute : NavKey

@Composable
fun SlowGroupPage() {
    val mainVm = LocalMainViewModel.current
    val ruleSummary by ruleSummaryFlow.collectAsState()
    val appInfoCache by appInfoMapFlow.collectAsState()

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
                title = { Text(text = li.songe.gkd.i18n.t("k_9b30ccd61d8b")) },
                actions = {
                    PerfIconButton(imageVector = PerfIcon.Info, onClick = throttle {
                        mainVm.dialogFlow.updateDialogOptions(
                            title = li.songe.gkd.i18n.t("k_9b30ccd61d8b"),
                            text = arrayOf(
                                li.songe.gkd.i18n.t("k_ec16bcc06956"),
                                li.songe.gkd.i18n.t("k_56c84be1b64f"),
                                li.songe.gkd.i18n.t("k_cfac5026d2d5")
                            ).joinToString("\n\n"),
                        )
                    })
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.scaffoldPadding(contentPadding)
        ) {
            items(
                ruleSummary.slowGlobalGroups,
                { (_, r) -> r.subsItem.id to r.group.key }
            ) { (group, rule) ->
                SlowGroupCard(
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            mainVm.navigatePage(
                                SubsGlobalGroupListRoute(
                                    rule.subsItem.id,
                                    group.key
                                )
                            )
                        })
                        .itemPadding(),
                    title = group.name,
                    desc = li.songe.gkd.i18n.t("k_d65437092b94", rule.rawSubs.name)
                )
            }
            items(
                ruleSummary.slowAppGroups,
                { (_, r) -> Triple(r.subsItem.id, r.appId, r.group.key) }
            ) { (group, rule) ->
                SlowGroupCard(
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            mainVm.navigatePage(
                                SubsAppGroupListRoute(
                                    rule.subsItem.id,
                                    rule.app.id,
                                    group.key
                                )
                            )
                        })
                        .itemPadding(),
                    title = group.name,
                    desc = li.songe.gkd.i18n.t("k_f2d422d96609", rule.rawSubs.name, appInfoCache[rule.app.id]?.name ?: rule.app.name ?: rule.app.id)
                )
            }
            item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                Spacer(modifier = Modifier.height(EmptyHeight))
                if (ruleSummary.slowGroupCount == 0) {
                    EmptyText(text = li.songe.gkd.i18n.t("k_cff584d9ab83"))
                }
            }
        }
    }
}

@Composable
fun SlowGroupCard(title: String, desc: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PerfIcon(
            imageVector = PerfIcon.KeyboardArrowRight,
        )
    }
}