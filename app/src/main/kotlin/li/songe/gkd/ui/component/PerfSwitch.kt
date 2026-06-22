package li.songe.gkd.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import li.songe.gkd.util.throttle

@Composable
fun PerfSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    key: Any? = null,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) = androidx.compose.runtime.key(key) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { throttle(it) },
        modifier = modifier.semantics {
            stateDescription = if (checked) li.songe.gkd.i18n.t("k_d78cde076b67") else li.songe.gkd.i18n.t("k_f628761bf56a")
        },
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}