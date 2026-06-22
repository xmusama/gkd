package li.songe.gkd.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.util.throttle

@Composable
fun InnerDisableSwitch(
    modifier: Modifier = Modifier,
    valid: Boolean = true,
    isSelectedMode: Boolean = false,
) {
    val mainVm = LocalMainViewModel.current
    val onClick = {
        if (valid) {
            mainVm.dialogFlow.updateDialogOptions(
                title = li.songe.gkd.i18n.t("k_f10b25a414c1"),
                text = li.songe.gkd.i18n.t("k_42e1b49044f8"),
            )
        } else {
            mainVm.dialogFlow.updateDialogOptions(
                title = li.songe.gkd.i18n.t("k_5c57086db539"),
                text = li.songe.gkd.i18n.t("k_ceea8ce8e559"),
            )
        }
    }
    PerfSwitch(
        checked = false,
        enabled = false,
        onCheckedChange = null,
        modifier = modifier.semantics {
            stateDescription = li.songe.gkd.i18n.t("k_0fe5a98e9f8c")
        }
            .minimumInteractiveComponentSize().run {
                if (isSelectedMode) {
                    this
                } else {
                    clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Switch,
                        onClick = throttle(onClick),
                        onClickLabel = li.songe.gkd.i18n.t("k_5e844385f492"),
                    )
                }
            }
    )
}
