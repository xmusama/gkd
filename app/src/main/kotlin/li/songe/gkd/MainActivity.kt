package li.songe.gkd

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.core.os.LocaleListCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dylanc.activityresult.launcher.PickContentLauncher
import com.dylanc.activityresult.launcher.StartActivityLauncher
import com.dylanc.activityresult.launcher.launchForResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.songe.gkd.a11y.topActivityFlow
import li.songe.gkd.a11y.updateSystemDefaultAppId
import li.songe.gkd.a11y.updateTopActivity
import li.songe.gkd.permission.AuthDialog
import li.songe.gkd.permission.updatePermissionState
import li.songe.gkd.service.A11yService
import li.songe.gkd.service.StatusService
import li.songe.gkd.service.fixRestartAutomatorService
import li.songe.gkd.service.updateTopTaskAppId
import li.songe.gkd.shizuku.automationRegisteredExceptionFlow
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.A11YScopeAppListRoute
import li.songe.gkd.ui.A11yEventLogPage
import li.songe.gkd.ui.A11yEventLogRoute
import li.songe.gkd.ui.A11yScopeAppListPage
import li.songe.gkd.ui.AboutPage
import li.songe.gkd.ui.AboutRoute
import li.songe.gkd.ui.ActionLogPage
import li.songe.gkd.ui.ActionLogRoute
import li.songe.gkd.ui.ActivityLogPage
import li.songe.gkd.ui.ActivityLogRoute
import li.songe.gkd.ui.AdvancedPage
import li.songe.gkd.ui.AdvancedPageRoute
import li.songe.gkd.ui.AppConfigPage
import li.songe.gkd.ui.AppConfigRoute
import li.songe.gkd.ui.AppOpsAllowPage
import li.songe.gkd.ui.AppOpsAllowRoute
import li.songe.gkd.ui.AuthA11yPage
import li.songe.gkd.ui.AuthA11yRoute
import li.songe.gkd.ui.BlockA11yAppListPage
import li.songe.gkd.ui.BlockA11yAppListRoute
import li.songe.gkd.ui.CrashReportPage
import li.songe.gkd.ui.CrashReportRoute
import li.songe.gkd.ui.EditBlockAppListPage
import li.songe.gkd.ui.EditBlockAppListRoute
import li.songe.gkd.ui.ImagePreviewPage
import li.songe.gkd.ui.ImagePreviewRoute
import li.songe.gkd.ui.SlowGroupPage
import li.songe.gkd.ui.SlowGroupRoute
import li.songe.gkd.ui.SnapshotPage
import li.songe.gkd.ui.SnapshotPageRoute
import li.songe.gkd.ui.SubsAppGroupListPage
import li.songe.gkd.ui.SubsAppGroupListRoute
import li.songe.gkd.ui.SubsAppListPage
import li.songe.gkd.ui.SubsAppListRoute
import li.songe.gkd.ui.SubsCategoryGroupPage
import li.songe.gkd.ui.SubsCategoryGroupRoute
import li.songe.gkd.ui.SubsCategoryPage
import li.songe.gkd.ui.SubsCategoryRoute
import li.songe.gkd.ui.SubsGlobalGroupExcludePage
import li.songe.gkd.ui.SubsGlobalGroupExcludeRoute
import li.songe.gkd.ui.SubsGlobalGroupListPage
import li.songe.gkd.ui.SubsGlobalGroupListRoute
import li.songe.gkd.ui.UpsertRuleGroupPage
import li.songe.gkd.ui.UpsertRuleGroupRoute
import li.songe.gkd.ui.WebViewPage
import li.songe.gkd.ui.WebViewRoute
import li.songe.gkd.ui.component.BuildDialog
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.ShareLogDlg
import li.songe.gkd.ui.component.SubsSheet
import li.songe.gkd.ui.component.TermsAcceptDialog
import li.songe.gkd.ui.component.TextDialog
import li.songe.gkd.ui.home.HomePage
import li.songe.gkd.ui.home.HomeRoute
import li.songe.gkd.ui.share.FixedWindowInsets
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.style.AppTheme
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.BarUtils
import li.songe.gkd.util.EditGithubCookieDlg
import li.songe.gkd.util.KeyboardUtils
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.appInfoMapFlow
import li.songe.gkd.util.componentName
import li.songe.gkd.util.copyText
import li.songe.gkd.util.fixSomeProblems
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.mapState
import li.songe.gkd.util.openApp
import li.songe.gkd.util.openUri
import li.songe.gkd.util.shizukuAppId
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import kotlin.concurrent.Volatile
import kotlin.reflect.jvm.jvmName

class MainActivity : ComponentActivity() {
    val startTime = System.currentTimeMillis()
    val mainVm by viewModels<MainViewModel>()
    val launcher by lazy { StartActivityLauncher(this) }
    val pickContentLauncher by lazy { PickContentLauncher(this) }

    val imeFullHiddenFlow = MutableStateFlow(true)
    val imePlayingFlow = MutableStateFlow(false)

    private val imeVisible: Boolean
        get() = ViewCompat.getRootWindowInsets(window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true  // fix #1315

    var topBarWindowInsets by mutableStateOf(WindowInsets(top = BarUtils.getStatusBarHeight()))

    private fun watchKeyboardVisible() {
        if (AndroidTarget.R) {
            ViewCompat.setWindowInsetsAnimationCallback(
                window.decorView,
                object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onStart(
                        animation: WindowInsetsAnimationCompat,
                        bounds: WindowInsetsAnimationCompat.BoundsCompat
                    ): WindowInsetsAnimationCompat.BoundsCompat {
                        imePlayingFlow.update { imeVisible }
                        return super.onStart(animation, bounds)
                    }

                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: List<WindowInsetsAnimationCompat>
                    ): WindowInsetsCompat {
                        return insets
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        imeFullHiddenFlow.update { !imeVisible }
                        imePlayingFlow.update { false }
                        super.onEnd(animation)
                    }
                })
        } else {
            KeyboardUtils.registerSoftInputChangedListener(window) { height ->
                // onEnd
                imeFullHiddenFlow.update { height == 0 }
            }
        }
    }

    suspend fun hideSoftInput(): Boolean {
        if (!imeFullHiddenFlow.updateAndGet { !imeVisible }) {
            KeyboardUtils.hideSoftInput(this@MainActivity)
            imeFullHiddenFlow.drop(1).first()
            return true
        }
        return false
    }

    fun justHideSoftInput(): Boolean {
        if (!imeFullHiddenFlow.updateAndGet { !imeVisible }) {
            KeyboardUtils.hideSoftInput(this@MainActivity)
            return true
        }
        return false
    }

    suspend fun pickFile(contentType: String): Uri? {
        val u = launcher.launchForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = contentType
        }).data?.data
        if (u == null) {
            toast(li.songe.gkd.i18n.t("k_dbb4430dc089"))
        }
        return u
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(storeFlow.value.language)
        )
        enableEdgeToEdge()
        fixSomeProblems()
        super.onCreate(savedInstanceState)
        LogUtils.d()
        mainVm
        launcher
        pickContentLauncher
        lifecycleScope.launch {
            storeFlow.mapState(lifecycleScope) { s -> s.excludeFromRecents }.collect {
                app.activityManager.appTasks.forEach { task ->
                    task.setExcludeFromRecents(it)
                }
            }
        }
        addOnNewIntentListener {
            mainVm.handleIntent(it)
            intent = null
        }
        watchKeyboardVisible()
        StatusService.autoStart()
        if (storeFlow.value.enableBlockA11yAppList) {
            updateTopTaskAppId(META.appId)
        }
        setContent {
            val latestInsets = TopAppBarDefaults.windowInsets
            val density = LocalDensity.current
            if (latestInsets.getTop(density) > topBarWindowInsets.getTop(density)) {
                topBarWindowInsets = FixedWindowInsets(latestInsets)
            }
            CompositionLocalProvider(
                LocalMainViewModel provides mainVm
            ) {
                AppTheme {
                    NavDisplay(
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        backStack = mainVm.backStack,
                        onBack = mainVm::popPage,
                        entryProvider = entryProvider {
                            entry<HomeRoute> { HomePage() }
                            entry<AuthA11yRoute> { AuthA11yPage() }
                            entry<AboutRoute> { AboutPage() }
                            entry<BlockA11yAppListRoute> { BlockA11yAppListPage() }
                            entry<AdvancedPageRoute> { AdvancedPage() }
                            entry<SnapshotPageRoute> { SnapshotPage() }
                            entry<AppOpsAllowRoute> { AppOpsAllowPage() }
                            entry<A11YScopeAppListRoute> { A11yScopeAppListPage() }
                            entry<ActivityLogRoute> { ActivityLogPage() }
                            entry<A11yEventLogRoute> { A11yEventLogPage() }
                            entry<EditBlockAppListRoute> { EditBlockAppListPage() }
                            entry<SlowGroupRoute> { SlowGroupPage() }
                            entry<SubsAppListRoute> { SubsAppListPage(it) }
                            entry<WebViewRoute> { WebViewPage(it) }
                            entry<SubsCategoryRoute> { SubsCategoryPage(it) }
                            entry<SubsGlobalGroupListRoute> { SubsGlobalGroupListPage(it) }
                            entry<SubsGlobalGroupExcludeRoute> { SubsGlobalGroupExcludePage(it) }
                            entry<ActionLogRoute> { ActionLogPage(it) }
                            entry<ImagePreviewRoute> { ImagePreviewPage(it) }
                            entry<UpsertRuleGroupRoute> { UpsertRuleGroupPage(it) }
                            entry<SubsAppGroupListRoute> { SubsAppGroupListPage(it) }
                            entry<AppConfigRoute> { AppConfigPage(it) }
                            entry<CrashReportRoute> { CrashReportPage() }
                            entry<SubsCategoryGroupRoute> { SubsCategoryGroupPage(it) }
                        },
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it })
                        },
                        popTransitionSpec = {
                            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it })
                        },
                    )
                    if (!mainVm.termsAcceptedFlow.collectAsState().value) {
                        TermsAcceptDialog()
                    } else {
                        UiAutomationAlreadyRegisteredDlg()
                        AccessRestrictedSettingsDlg()
                        ShizukuErrorDialog(mainVm.shizukuErrorFlow)
                        AuthDialog(mainVm.authReasonFlow)
                        BuildDialog(mainVm.dialogFlow)
                        mainVm.uploadOptions.ShowDialog()
                        EditGithubCookieDlg()
                        mainVm.updateStatus?.UpgradeDialog()
                        SubsSheet(mainVm, mainVm.sheetSubsIdFlow)
                        mainVm.inputSubsLinkOption.ContentDialog()
                        mainVm.ruleGroupState.Render()
                        TextDialog(mainVm.textFlow)
                        ShareLogDlg(mainVm.showShareLogDlgFlow)
                    }
                }
            }
            LaunchedEffect(null) {
                intent?.let {
                    mainVm.handleIntent(it)
                    intent = null
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LogUtils.d()
        activityVisibleState++
        if (topActivityFlow.value.appId != META.appId) {
            synchronized(topActivityFlow) {
                updateTopActivity(
                    META.appId,
                    MainActivity::class.jvmName
                )
            }
        }
    }

    var isFirstResume = true
    override fun onResume() {
        super.onResume()
        LogUtils.d()
        if (isFirstResume && startTime - app.startTime < 2000) {
            isFirstResume = false
        } else {
            syncFixState()
        }
    }

    override fun onStop() {
        super.onStop()
        LogUtils.d()
        activityVisibleState--
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d()
    }
}

@Volatile
private var activityVisibleState = 0
val isActivityVisible get() = activityVisibleState > 0

val activityNavSourceName by lazy { META.appId + ".activity.nav.source" }

fun Activity.navToMainActivity() {
    if (intent != null) {
        val navIntent = Intent(intent)
        navIntent.component = MainActivity::class.componentName
        navIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        navIntent.putExtra(activityNavSourceName, this::class.jvmName)
        startActivity(navIntent)
    }
    finish()
}

private val syncStateMutex = Mutex()
fun syncFixState() {
    appScope.launchTry(Dispatchers.IO) {
        if (syncStateMutex.isLocked) {
            LogUtils.d("syncFixState isLocked")
        }
        syncStateMutex.withLock {
            updateSystemDefaultAppId()
            shizukuContextFlow.value.grantSelf()
            updatePermissionState()
            fixRestartAutomatorService()
        }
    }
}

@Composable
private fun ShizukuErrorDialog(stateFlow: MutableStateFlow<Throwable?>) {
    val state = stateFlow.collectAsState().value
    if (state != null) {
        val errorText = remember { state.stackTraceToString() }
        val appInfoCache = appInfoMapFlow.collectAsState().value
        val installed = appInfoCache.contains(shizukuAppId)
        AlertDialog(
            onDismissRequest = { stateFlow.value = null },
            title = { Text(text = li.songe.gkd.i18n.t("k_9c8db95f1272")) },
            text = {
                Column {
                    Text(
                        text = if (installed) {
                            li.songe.gkd.i18n.t("k_f08db2ab6e9b")
                        } else {
                            li.songe.gkd.i18n.t("k_6dc32911b1cd")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = errorText,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(8.dp)
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        PerfIcon(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clickable(onClick = throttle {
                                    copyText(errorText)
                                })
                                .padding(4.dp)
                                .size(20.dp),
                            imageVector = PerfIcon.ContentCopy,
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f),
                        )
                    }
                }
            },
            confirmButton = {
                if (installed) {
                    TextButton(onClick = {
                        stateFlow.value = null
                        openApp(shizukuAppId)
                    }) {
                        Text(text = li.songe.gkd.i18n.t("k_894a72442f41"))
                    }
                } else {
                    TextButton(onClick = {
                        stateFlow.value = null
                        openUri(ShortUrlSet.URL4)
                    }) {
                        Text(text = li.songe.gkd.i18n.t("k_21654037e21c"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { stateFlow.value = null }) {
                    Text(text = li.songe.gkd.i18n.t("k_dd3760c80abe"))
                }
            }
        )
    }
}


val accessRestrictedSettingsShowFlow = MutableStateFlow(false)

@Composable
fun AccessRestrictedSettingsDlg() {
    val a11yRunning by A11yService.isRunning.collectAsState()
    LaunchedEffect(a11yRunning) {
        if (a11yRunning) {
            accessRestrictedSettingsShowFlow.value = false
        }
    }
    val accessRestrictedSettingsShow by accessRestrictedSettingsShowFlow.collectAsState()
    val mainVm = LocalMainViewModel.current
    val isA11yPage = mainVm.topRoute is AuthA11yRoute
    LaunchedEffect(isA11yPage, accessRestrictedSettingsShow) {
        if (isA11yPage && accessRestrictedSettingsShow && !a11yRunning) {
            toast(li.songe.gkd.i18n.t("k_a0995a1cf809"))
            accessRestrictedSettingsShowFlow.value = false
        }
    }
    if (accessRestrictedSettingsShow && !isA11yPage && !a11yRunning) {
        AlertDialog(
            title = {
                Text(text = li.songe.gkd.i18n.t("k_17bfc950b710"))
            },
            text = {
                Text(text = li.songe.gkd.i18n.t("k_1262ae439f57"))
            },
            onDismissRequest = {
                accessRestrictedSettingsShowFlow.value = false
            },
            confirmButton = {
                TextButton({
                    accessRestrictedSettingsShowFlow.value = false
                    mainVm.navigateWebPage(ShortUrlSet.URL2)
                }) {
                    Text(text = li.songe.gkd.i18n.t("k_ec7ae06b0999"))
                }
            },
            dismissButton = {
                TextButton({
                    accessRestrictedSettingsShowFlow.value = false
                }) {
                    Text(text = li.songe.gkd.i18n.t("k_6c14bd7f6f9e"))
                }
            },
        )
    }
}

@Composable
fun UiAutomationAlreadyRegisteredDlg() {
    if (automationRegisteredExceptionFlow.collectAsState().value != null) {
        AlertDialog(
            onDismissRequest = {
                automationRegisteredExceptionFlow.value = null
            },
            title = { Text(text = li.songe.gkd.i18n.t("k_65525f0f4417")) },
            text = {
                Text(text = li.songe.gkd.i18n.t("k_914a3f7e1522"))
            },
            confirmButton = {
                TextButton(onClick = {
                    automationRegisteredExceptionFlow.value = null
                }) {
                    Text(text = li.songe.gkd.i18n.t("k_dd3760c80abe"))
                }
            }
        )
    }
}
