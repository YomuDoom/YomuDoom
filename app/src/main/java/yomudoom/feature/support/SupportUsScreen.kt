package yomudoom.feature.support

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Patreon

class SupportUsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val clipboard: Clipboard = LocalClipboard.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val pixCopiedMessage = stringResource(MR.strings.supportUsScreen_pixCopied)

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.label_support_us),
                    navigateUp = navigator::pop,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(
                    remember(paddingValues) {
                        object : PaddingValues {
                            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                                paddingValues.calculateLeftPadding(layoutDirection) + MaterialTheme.padding.medium

                            override fun calculateTopPadding(): Dp = 0.dp

                            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                                paddingValues.calculateRightPadding(layoutDirection) + MaterialTheme.padding.medium

                            override fun calculateBottomPadding(): Dp = 0.dp
                        }
                    },
                ),
            ) {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))

                Text(
                    text = stringResource(MR.strings.supportUsScreen_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )

                SupportItem(
                    icon = CustomIcons.Patreon,
                    title = stringResource(MR.strings.supportUsScreen_donationPlatform_patreon),
                    subtitle = stringResource(MR.strings.supportUsScreen_donationPlatform_patreonDescription),
                    widgetIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = { uriHandler.openUri(Constants.URL_DONATE_PATREON) },
                )
                SupportItem(
                    icon = Icons.Outlined.CreditCard,
                    title = stringResource(MR.strings.supportUsScreen_donationPlatform_stripe),
                    subtitle = stringResource(MR.strings.supportUsScreen_donationPlatform_stripeDescription),
                    widgetIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = { uriHandler.openUri(Constants.URL_DONATE_STRIPE) },
                )
                SupportItem(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = stringResource(MR.strings.supportUsScreen_donationPlatform_pix),
                    subtitle = Constants.PIX_KEY,
                    widgetIcon = Icons.Outlined.ContentCopy,
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText("Pix", Constants.PIX_KEY).toClipEntry(),
                            )
                            snackbarHostState.showSnackbar(pixCopiedMessage)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }

    @Composable
    private fun SupportItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        widgetIcon: ImageVector,
        onClick: () -> Unit,
    ) {
        Card {
            TextPreferenceWidget(
                title = title,
                subtitle = subtitle,
                icon = icon,
                widget = {
                    Icon(
                        imageVector = widgetIcon,
                        contentDescription = null,
                    )
                },
                onPreferenceClick = onClick,
            )
        }
    }
}
