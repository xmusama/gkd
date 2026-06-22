package li.songe.gkd.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.ui.style.clearJson5TransformationCache
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.subsMapFlow
import li.songe.gkd.util.toast
import li.songe.gkd.util.updateSubscription
import li.songe.json5.Json5

class UpsertRuleGroupVm(val route: UpsertRuleGroupRoute) : ViewModel() {
    val groupKey = route.groupKey
    val appId = route.appId

    val isEdit = groupKey != null
    val isApp = appId != null
    val isAddAnyApp = appId == ""

    private val initialGroup: RawSubscription.RawGroupProps? = run {
        val subs = subsMapFlow.value[route.subsId]
        subs ?: return@run null
        if (groupKey != null) {
            if (appId != null) {
                subs.getAppGroups(appId)
            } else {
                subs.globalGroups
            }.find { it.key == route.groupKey }
        } else {
            null
        }
    }

    private val initText = initialGroup?.cacheStr ?: ""
    val textFlow = MutableStateFlow(initText)

    fun hasTextChanged(): Boolean {
        val text = textFlow.value
        if (!isEdit) return !text.isBlank()
        if (initText == text) return false
        return initialGroup?.cacheJsonObject != runCatching { Json5.parseToJson5Element(text) }.getOrNull()
    }


    var addAppId: String? = null

    fun saveRule() {
        val subs = subsMapFlow.value[route.subsId] ?: error(li.songe.gkd.i18n.t("k_9e5cc3140b15"))
        val text = textFlow.value
        if (text.isBlank()) {
            error(li.songe.gkd.i18n.t("k_af89d2fadeac"))
        }
        if (text == initText) {
            toast(li.songe.gkd.i18n.t("k_340b2031bf1e"))
            return
        }
        var jsonObject = runCatching { Json5.parseToJson5Element(text) }.run {
            if (isFailure) {
                error(li.songe.gkd.i18n.t("k_cc336d4918a3", exceptionOrNull()?.message))
            }
            getOrThrow()
        }
        if (jsonObject !is JsonObject) {
            error(li.songe.gkd.i18n.t("k_5057a1d2a5c8"))
        }
        // 自动填充 key
        if (jsonObject["name"] != null && jsonObject["key"] == null) {
            jsonObject = JsonObject(jsonObject + mapOf("key" to JsonPrimitive(groupKey ?: 0)))
        }
        if (jsonObject["id"] is JsonPrimitive && jsonObject["groups"] is JsonArray) {
            val groups = jsonObject["groups"] as JsonArray
            val newGroups = groups.map {
                if (it is JsonObject && it["name"] != null && it["key"] == null) {
                    JsonObject(it + mapOf("key" to JsonPrimitive(groupKey ?: 0)))
                } else {
                    it
                }
            }
            jsonObject = JsonObject(mapOf("groups" to JsonArray(newGroups)) + jsonObject)
        }

        if (jsonObject == initialGroup?.cacheJsonObject) {
            toast(li.songe.gkd.i18n.t("k_340b2031bf1e"))
            return
        }
        if (groupKey != null) {
            var newGroup = try {
                if (appId != null) {
                    if (jsonObject["groups"] is JsonArray) {
                        val id = jsonObject["id"] ?: error(li.songe.gkd.i18n.t("k_fab7e8b9f326"))
                        if (!(id is JsonPrimitive && id.isString && id.content == appId)) {
                            error(li.songe.gkd.i18n.t("k_0e78efa23d4b"))
                        }
                        RawSubscription.parseApp(jsonObject).let { newApp ->
                            if (newApp.groups.isEmpty()) {
                                error(li.songe.gkd.i18n.t("k_7d1dac68ad88"))
                            }
                            newApp.groups.first()
                        }
                    } else {
                        null
                    } ?: RawSubscription.parseAppGroup(jsonObject)
                } else {
                    RawSubscription.parseGlobalGroup(jsonObject)
                }
            } catch (e: Exception) {
                LogUtils.d(e)
                error(li.songe.gkd.i18n.t("k_91345f02fac4", e.message))
            }
            newGroup.errorDesc?.let(::error)
            if (newGroup.key != groupKey) {
                // 自动修正 key 与原来一致
                newGroup = when (newGroup) {
                    is RawSubscription.RawAppGroup -> newGroup.copy(key = groupKey)
                    is RawSubscription.RawGlobalGroup -> newGroup.copy(key = groupKey)
                }
            }
            if (newGroup == initialGroup) {
                toast(li.songe.gkd.i18n.t("k_340b2031bf1e"))
                return
            }
            val newSubs = if (appId != null) {
                newGroup as RawSubscription.RawAppGroup
                val app = subs.apps.find { a -> a.id == appId } ?: error(li.songe.gkd.i18n.t("k_4cce244abd54"))
                subs.copy(apps = subs.apps.toMutableList().apply {
                    set(
                        indexOfFirst { a -> a.id == appId },
                        app.copy(groups = app.groups.toMutableList().apply {
                            set(
                                indexOfFirst { g -> g.key == newGroup.key },
                                newGroup
                            )
                        })
                    )
                })
            } else {
                newGroup as RawSubscription.RawGlobalGroup
                subs.copy(globalGroups = subs.globalGroups.toMutableList().apply {
                    set(indexOfFirst { g -> g.key == newGroup.key }, newGroup)
                })
            }
            updateSubscription(newSubs)
        } else {
            if (isAddAnyApp) {
                var newApp = try {
                    RawSubscription.parseApp(jsonObject).apply {
                        if (groups.isEmpty()) {
                            error(li.songe.gkd.i18n.t("k_7d1dac68ad88"))
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.d(e)
                    error(li.songe.gkd.i18n.t("k_91345f02fac4", e.message))
                }
                val oldApp = subs.apps.find { it.id == newApp.id }
                if (oldApp != null) {
                    newApp.groups.forEach { g ->
                        checkGroupKeyName(oldApp.groups, g)
                    }
                    // 自动修正 key 与原来不重复
                    val usedKeys = oldApp.groups.map { it.key }.toHashSet()
                    newApp = newApp.copy(groups = newApp.groups.map { g ->
                        if (g.key in usedKeys) {
                            g.copy(key = usedKeys.max() + 1).also {
                                usedKeys.add(it.key)
                            }
                        } else {
                            g
                        }
                    })
                }
                val newSubs = subs.copy(apps = subs.apps.toMutableList().apply {
                    val i = indexOfFirst { a -> a.id == newApp.id }
                    if (i >= 0) {
                        set(
                            i,
                            get(i).copy(groups = get(i).groups + newApp.groups),
                        )
                    } else {
                        add(newApp)
                    }
                })
                addAppId = newApp.id
                updateSubscription(newSubs)
            } else if (appId != null) {
                // add specified app group
                var newGroups = try {
                    if (jsonObject["groups"] is JsonArray) {
                        val id = jsonObject["id"] ?: error(li.songe.gkd.i18n.t("k_fab7e8b9f326"))
                        if (!(id is JsonPrimitive && id.isString && id.content == appId)) {
                            error(li.songe.gkd.i18n.t("k_0e78efa23d4b"))
                        }
                        RawSubscription.parseApp(jsonObject).apply {
                            if (groups.isEmpty()) {
                                error(li.songe.gkd.i18n.t("k_7d1dac68ad88"))
                            }
                        }.groups
                    } else {
                        null
                    } ?: listOf(RawSubscription.parseAppGroup(jsonObject))
                } catch (e: Exception) {
                    LogUtils.d(e)
                    error(li.songe.gkd.i18n.t("k_91345f02fac4", e.message))
                }
                val oldApp = subs.getApp(appId)
                newGroups.forEach { g ->
                    checkGroupKeyName(oldApp.groups, g)
                    g.errorDesc?.let { error(it) }
                }
                // 自动修正 key 与原来不重复
                val usedKeys = oldApp.groups.map { it.key }.toHashSet()
                newGroups = newGroups.map { g ->
                    if (g.key in usedKeys) {
                        g.copy(key = usedKeys.max() + 1).also {
                            usedKeys.add(it.key)
                        }
                    } else {
                        g
                    }
                }
                val newSubs = subs.copy(apps = subs.apps.toMutableList().apply {
                    val newApp = oldApp.copy(groups = oldApp.groups + newGroups)
                    val i = indexOfFirst { a -> a.id == newApp.id }
                    if (i >= 0) {
                        set(
                            i,
                            newApp
                        )
                    } else {
                        add(newApp)
                    }
                })
                updateSubscription(newSubs)
            } else {
                // add global group
                var newGroup = try {
                    RawSubscription.parseGlobalGroup(jsonObject)
                } catch (e: Exception) {
                    LogUtils.d(e)
                    error(li.songe.gkd.i18n.t("k_91345f02fac4", e.message))
                }
                checkGroupKeyName(subs.globalGroups, newGroup)
                if (subs.globalGroups.any { it.key == newGroup.key }) {
                    newGroup = newGroup.copy(key = subs.globalGroups.maxOf { it.key } + 1)
                }
                updateSubscription(
                    subs.copy(
                        globalGroups = subs.globalGroups + newGroup
                    )
                )
            }
        }
        if (isEdit) {
            toast(li.songe.gkd.i18n.t("k_e2cff7737269"))
        } else {
            toast(li.songe.gkd.i18n.t("k_6950d05c09db"))
        }
    }

    init {
        addCloseable { clearJson5TransformationCache() }
    }
}

private fun checkGroupKeyName(
    groups: List<RawSubscription.RawGroupProps>,
    newGroup: RawSubscription.RawGroupProps
) {
    if (groups.any { it.name == newGroup.name }) {
        error(li.songe.gkd.i18n.t("k_6a1bafc9b3f7", newGroup.name))
    }
}
