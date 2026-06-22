package li.songe.gkd.util

import androidx.compose.ui.graphics.vector.ImageVector
import li.songe.gkd.ui.component.PerfIcon

private fun trLabel(key: String) = li.songe.gkd.i18n.t(key)

sealed interface Option<T> {
    val value: T
    val label: String
    val options: List<Option<T>>
}

sealed interface OptionIcon {
    val icon: ImageVector
}

sealed interface OptionMenuLabel {
    val menuLabel: String
}

fun <V, T : Option<V>> Iterable<T>.findOption(value: V): T {
    return find { it.value == value } ?: first()
}

sealed class AppSortOption(override val value: Int, private val labelKey: String) : Option<Int> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object ByAppName : AppSortOption(0, "k_0170e6ea452c")
    data object ByActionTime : AppSortOption(2, "k_99d9660d5d6e")
    data object ByUsedTime : AppSortOption(3, "k_ffa2f767bab9")

    companion object {
        val objects by lazy { listOf(ByAppName, ByUsedTime, ByActionTime) }
    }
}

sealed class UpdateTimeOption(
    override val value: Long,
    private val labelKey: String
) : Option<Long> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object Pause : UpdateTimeOption(-1, "k_130448bce675")
    data object Everyday : UpdateTimeOption(24 * 60 * 60_000, "k_5e4715ffc008")
    data object Every3Days : UpdateTimeOption(24 * 60 * 60_000 * 3, "k_713c348d8b72")
    data object Every7Days : UpdateTimeOption(24 * 60 * 60_000 * 7, "k_713c3b3f44d4")

    companion object {
        val objects by lazy { listOf(Pause, Everyday, Every3Days, Every7Days) }
    }
}

sealed class DarkThemeOption(
    override val value: Boolean?,
    private val labelKey: String,
    private val menuLabelKey: String,
    override val icon: ImageVector
) : Option<Boolean?>, OptionIcon, OptionMenuLabel {
    override val label get() = trLabel(labelKey)
    override val menuLabel get() = trLabel(menuLabelKey)
    override val options get() = objects

    data object FollowSystem : DarkThemeOption(null, "k_4afad877551a", "k_4afad877551a", PerfIcon.AutoMode)
    data object AlwaysEnable : DarkThemeOption(true, "k_d4e9ca3dd494", "k_30b2c979ac87", PerfIcon.DarkMode)
    data object AlwaysDisable : DarkThemeOption(false, "k_6c14bd7f6f9e", "k_80ec9e2b1bc4", PerfIcon.LightMode)

    companion object {
        val objects by lazy { listOf(FollowSystem, AlwaysEnable, AlwaysDisable) }
    }
}

sealed class LanguageOption(
    override val value: String,
    private val labelKey: String?,
    private val fallbackLabel: String,
) : Option<String> {
    override val label get() = labelKey?.let(::trLabel) ?: fallbackLabel
    override val options get() = objects

    data object English : LanguageOption("en", null, "English")
    data object Chinese : LanguageOption("zh", "k_7be2d2d20c10", "Chinese")
    data object Indonesian : LanguageOption("id", null, "Indonesia")

    companion object {
        val objects by lazy { listOf(English, Chinese, Indonesian) }
    }
}

sealed class EnableGroupOption(
    override val value: Boolean?,
    private val labelKey: String
) : Option<Boolean?> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object FollowSubs : EnableGroupOption(null, "k_8789f6741642")
    data object AllEnable : EnableGroupOption(true, "k_6bc71e03428d")
    data object AllDisable : EnableGroupOption(false, "k_6fe9dcf20a67")

    companion object {
        val objects by lazy { listOf(FollowSubs, AllEnable, AllDisable) }
    }
}

sealed class RuleSortOption(override val value: Int, private val labelKey: String) : Option<Int> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object ByDefault : RuleSortOption(0, "k_53fd5ea8dd21")
    data object ByActionTime : RuleSortOption(1, "k_99d9660d5d6e")
    data object ByRuleName : RuleSortOption(2, "k_4d3a4b7c823a")

    companion object {
        val objects by lazy { listOf(ByDefault, ByActionTime, ByRuleName) }
    }
}

sealed class UpdateChannelOption(
    override val value: Int,
    private val labelKey: String,
    val url: String
) : Option<Int> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object Stable : UpdateChannelOption(
        0,
        "k_18b6cea8849b",
        "https://registry.npmmirror.com/@gkd-kit/app/latest/files/index.json"
    )

    data object Beta : UpdateChannelOption(
        1,
        "k_6bb1fe4c8fb9",
        "https://registry.npmmirror.com/@gkd-kit/app-beta/latest/files/index.json"
    )

    companion object {
        val objects by lazy { listOf(Stable, Beta) }
    }
}

sealed interface BinaryOption : Option<Int> {
    fun include(flag: Int): Boolean = (value and flag) != 0
    fun invert(flag: Int): Int = value xor flag

    companion object {
        fun combine(options: Collection<BinaryOption>): Int {
            return options.fold(0) { a, b -> a or b.value }
        }
    }
}


sealed class AppGroupOption(
    override val value: Int,
    private val labelKey: String
) : BinaryOption {
    override val label get() = trLabel(labelKey)
    override val options get() = allObjects

    data object SystemGroup : AppGroupOption(1 shl 0, "k_a4be5dfa64b2")
    data object UserGroup : AppGroupOption(1 shl 1, "k_ee2a5dad81fc")
    data object UnInstalledGroup : AppGroupOption(1 shl 2, "k_dcaf1ae9ae72")

    companion object {
        val normalObjects by lazy { listOf(SystemGroup, UserGroup) }
        val allObjects by lazy { listOf(SystemGroup, UserGroup, UnInstalledGroup) }
    }
}

sealed class AutomatorModeOption(
    override val value: Int,
    private val labelKey: String,
) : Option<Int> {
    override val label get() = trLabel(labelKey)
    override val options get() = objects

    data object A11yMode : AutomatorModeOption(1, "k_04c62c8f3d82")
    data object AutomationMode : AutomatorModeOption(2, "k_90e4e9bd1a78")

    companion object {
        val objects by lazy { listOf(A11yMode, AutomationMode) }
    }
}
