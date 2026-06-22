package li.songe.gkd.permission

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.content.pm.PackageManager
import android.provider.Settings
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import li.songe.gkd.MainActivity
import li.songe.gkd.MainViewModel
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.shizuku.SafeAppOpsService
import li.songe.gkd.shizuku.SafePackageManager
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.ui.AppOpsAllowRoute
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.toast
import li.songe.gkd.util.updateAllAppInfo
import li.songe.gkd.util.updateAppMutex
import rikka.shizuku.Shizuku

class PermissionState(
    val name: String,
    val check: () -> Boolean,
    val request: (suspend (context: MainActivity) -> PermissionResult)? = null,
    /**
     * show it when user doNotAskAgain
     */
    val reason: AuthReason? = null,
) {
    val stateFlow = MutableStateFlow(false)
    val value get() = stateFlow.value

    fun updateAndGet(): Boolean {
        return stateFlow.updateAndGet { check() }
    }

    fun updateChanged(): Boolean {
        return value != updateAndGet()
    }

    fun checkOrToast(): Boolean = if (!updateAndGet()) {
        val r = updateAndGet()
        if (!r) {
            reason?.text?.let { toast(it()) }
        }
        r
    } else {
        true
    }
}

private suspend fun asyncRequestPermission(
    context: Activity,
    permission: IPermission,
): PermissionResult {
    if (XXPermissions.isGrantedPermission(context, permission)) {
        return PermissionResult.Granted
    }
    val deferred = CompletableDeferred<PermissionResult>()
    XXPermissions.with(context)
        .unchecked()
        .permission(permission)
        .request { grantedList, _ ->
            if (grantedList.contains(permission)) {
                PermissionResult.Granted
            } else {
                PermissionResult.Denied(
                    XXPermissions.isDoNotAskAgainPermissions(
                        context,
                        arrayOf(permission)
                    )
                )
            }.let { deferred.complete(it) }
        }
    return deferred.await()
}

private fun checkAllowedOp(op: String): Boolean = app.appOpsManager.checkOpNoThrow(
    op,
    android.os.Process.myUid(),
    app.packageName
).let {
    it != AppOpsManager.MODE_IGNORED && it != AppOpsManager.MODE_ERRORED
}

// https://github.com/gkd-kit/gkd/issues/954
// https://github.com/gkd-kit/gkd/issues/887
val foregroundServiceSpecialUseState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_a5bd8641a75e"),
        check = {
            if (AndroidTarget.UPSIDE_DOWN_CAKE) {
                checkAllowedOp(AppOpsManagerHidden.OPSTR_FOREGROUND_SERVICE_SPECIAL_USE)
            } else {
                true
            }
        },
        reason = AuthReason(
            text = { li.songe.gkd.i18n.t("k_8d4148193c23") },
            confirm = {
                MainViewModel.instance.navigatePage(AppOpsAllowRoute)
            },
        ),
    )
}

// https://github.com/orgs/gkd-kit/discussions/1234
val accessA11yState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_78ce84cb46d6"),
        check = {
            if (AndroidTarget.Q) {
                checkAllowedOp(AppOpsManagerHidden.OPSTR_ACCESS_ACCESSIBILITY)
            } else {
                true
            }
        },
    )
}

val createA11yOverlayState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_a2ee304101b6"),
        check = {
            if (SafeAppOpsService.supportCreateA11yOverlay) {
                checkAllowedOp(AppOpsManagerHidden.OPSTR_CREATE_ACCESSIBILITY_OVERLAY)
            } else {
                true
            }
        },
    )
}

const val Manifest_permission_GET_APP_OPS_STATS = "android.permission.GET_APP_OPS_STATS"

val getAppOpsStatsState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_87577b0cc883"),
        check = {
            app.checkGrantedPermission(Manifest_permission_GET_APP_OPS_STATS)
        },
    )
}

private var canRestrictsRead = true
val accessRestrictedSettingsState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_3515eb9da0c7"),
        check = {
            if (canRestrictsRead && AndroidTarget.UPSIDE_DOWN_CAKE && getAppOpsStatsState.updateAndGet()) {
                try {
                    // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r55:frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java;l=4237
                    checkAllowedOp(AppOpsManagerHidden.OPSTR_ACCESS_RESTRICTED_SETTINGS)
                } catch (_: SecurityException) {
                    // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r54:frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java;l=4227
                    canRestrictsRead = false
                    true
                }
            } else {
                true
            }
        },
    )
}

val appOpsRestrictStateList by lazy {
    arrayOf(
        accessA11yState,
        createA11yOverlayState,
        accessRestrictedSettingsState,
        foregroundServiceSpecialUseState,
    )
}

val appOpsRestrictedFlow by lazy {
    combine(
        *appOpsRestrictStateList.map { it.stateFlow }.toTypedArray(),
    ) { list ->
        list.any { !it }
    }.stateIn(appScope, SharingStarted.Eagerly, false)
}

val notificationState by lazy {
    val permission = PermissionLists.getNotificationServicePermission()
    PermissionState(
        name = li.songe.gkd.i18n.t("k_f1fb214b7f3d"),
        check = {
            XXPermissions.isGrantedPermission(app, permission)
        },
        request = { asyncRequestPermission(it, permission) },
        reason = AuthReason(
            text = { li.songe.gkd.i18n.t("k_73424c2092f7") },
            confirm = {
                XXPermissions.startPermissionActivity(app, permission)
            }
        ),
    )
}

val canQueryPkgState by lazy {
    val permission = PermissionLists.getGetInstalledAppsPermission()
    val supported by lazy { permission.isSupportRequestPermission(app) }
    PermissionState(
        name = li.songe.gkd.i18n.t("k_87f76e269c44"),
        check = {
            if (supported) {
                // 此框架内部有两个 printStackTrace 导致每次检测都会打印日志污染控制台
                XXPermissions.isGrantedPermission(app, permission)
            } else {
                true
            }
        },
        request = {
            asyncRequestPermission(it, permission)
        },
        reason = AuthReason(
            text = { li.songe.gkd.i18n.t("k_3d81f289fc54") },
            confirm = {
                XXPermissions.startPermissionActivity(app, permission)
            }
        ),
    )
}

val canDrawOverlaysState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_076b77e2acda"),
        check = {
            // https://developer.android.com/security/fraud-prevention/activities?hl=zh-cn#hide_overlay_windows
            Settings.canDrawOverlays(app)
        },
        reason = AuthReason(
            text = {
                li.songe.gkd.i18n.t("k_01cb84d34770")
            },
            confirm = {
                XXPermissions.startPermissionActivity(
                    app,
                    PermissionLists.getSystemAlertWindowPermission()
                )
            }
        ),
    )
}

val canWriteExternalStorage by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_b60d38705780"),
        check = {
            if (AndroidTarget.Q) {
                true
            } else {
                app.checkGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        },
        request = {
            if (AndroidTarget.Q) {
                PermissionResult.Granted
            } else {
                asyncRequestPermission(it, PermissionLists.getWriteExternalStoragePermission())
            }
        },
        reason = AuthReason(
            text = { li.songe.gkd.i18n.t("k_7e9ca9ca7e00") },
            confirm = {
                XXPermissions.startPermissionActivity(
                    app,
                    PermissionLists.getWriteExternalStoragePermission()
                )
            }
        ),
    )
}

val ignoreBatteryOptimizationsState by lazy {
    val permission = PermissionLists.getRequestIgnoreBatteryOptimizationsPermission()
    PermissionState(
        name = li.songe.gkd.i18n.t("k_945be1dcae2e"),
        check = {
            app.powerManager.isIgnoringBatteryOptimizations(app.packageName)
        },
        request = {
            asyncRequestPermission(it, permission)
        },
        reason = AuthReason(
            text = { li.songe.gkd.i18n.t("k_1572c596179e") },
            confirm = {
                XXPermissions.startPermissionActivity(
                    app,
                    permission
                )
            }
        ),
    )
}

val writeSecureSettingsState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_1cb2886c8b96"),
        check = { app.checkGrantedPermission(Manifest.permission.WRITE_SECURE_SETTINGS) },
    )
}

private fun shizukuCheckGranted(): Boolean {
    if (Shizuku.getBinder()?.isBinderAlive != true) return false
    val granted = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }
    if (!granted) return false
    val u = shizukuContextFlow.value.packageManager ?: SafePackageManager.newBinder()
    return u?.isSafeMode != null
}

val shizukuGrantedState by lazy {
    PermissionState(
        name = li.songe.gkd.i18n.t("k_76c9741888ff"),
        check = { shizukuCheckGranted() },
    )
}

val allPermissionStates by lazy {
    listOf(
        notificationState,
        foregroundServiceSpecialUseState,
        accessA11yState,
        createA11yOverlayState,
        getAppOpsStatsState,
        accessRestrictedSettingsState,
        canDrawOverlaysState,
        canWriteExternalStorage,
        ignoreBatteryOptimizationsState,
        writeSecureSettingsState,
        canQueryPkgState,
        shizukuGrantedState,
    )
}

fun updatePermissionState() {
    allPermissionStates.forEach {
        if (it === canQueryPkgState && !updateAppMutex.mutex.isLocked) {
            if (canQueryPkgState.updateChanged()) {
                updateAllAppInfo()
            }
        } else {
            it.updateAndGet()
        }
    }
}