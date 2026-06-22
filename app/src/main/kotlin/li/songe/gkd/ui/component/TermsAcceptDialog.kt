package li.songe.gkd.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import li.songe.gkd.MainActivity
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.throttle


@Composable
fun TermsAcceptDialog() {
    val mainVm = LocalMainViewModel.current
    val context = LocalActivity.current as MainActivity
    val modifier = Modifier.fillMaxWidth()
    val stepDataList = remember {
        arrayOf(
            li.songe.gkd.i18n.t("k_92ee99a034a1") to @Composable {
                val linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                )
                Text(
                    modifier = modifier,
                    text = buildAnnotatedString {
                        append(li.songe.gkd.i18n.t("k_b910f2aa1075"))
                        withLink(
                            LinkAnnotation.Url(
                                ShortUrlSet.URL12,
                                linkStyles
                            )
                        ) {
                            append(li.songe.gkd.i18n.t("k_0399ad7167e0"))
                        }
                        append(li.songe.gkd.i18n.t("k_7f05f593978e"))
                        withLink(
                            LinkAnnotation.Url(
                                ShortUrlSet.URL11,
                                linkStyles
                            )
                        ) {
                            append(li.songe.gkd.i18n.t("k_8c276c1fea2d"))
                        }
                        append(li.songe.gkd.i18n.t("k_57d9707a6b1c"))
                    },
                )
            },
            li.songe.gkd.i18n.t("k_6152aa364d44") to @Composable {
                Text(
                    modifier = modifier,
                    text = li.songe.gkd.i18n.t("k_37c53d9dc082"),
                )
            }
        )
    }
    var step by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stepDataList[step].first)
        },
        text = stepDataList[step].second,
        confirmButton = {
            TextButton(onClick = throttle {
                if (step < stepDataList.size - 1) {
                    step++
                } else {
                    mainVm.termsAcceptedFlow.value = true
                }
            }) {
                Text(text = li.songe.gkd.i18n.t("k_d5f0847ff21f"))
            }
        },
        dismissButton = {
            TextButton(onClick = throttle {
                context.finish()
            }) {
                Text(text = li.songe.gkd.i18n.t("k_befce4eeb37c"))
            }
        }
    )
}