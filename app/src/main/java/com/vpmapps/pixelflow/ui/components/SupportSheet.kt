package com.vpmapps.pixelflow.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vpmapps.pixelflow.billing.SupporterBilling
import com.vpmapps.pixelflow.billing.SupporterTier

/**
 * Optional support: a monthly subscription or a one-time lifetime purchase. Nothing is locked;
 * supporters get a badge on the dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SupportSheet(onDismiss: () -> Unit) {
    val state by SupporterBilling.state.collectAsState()
    val activity = LocalActivity.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("support_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isSupporter) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (state.isSupporter) {
                Text("Thank you for supporting PixelFlow", style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
                Text(
                    text = if (state.tier == SupporterTier.LIFETIME) {
                        "You're a lifetime supporter. Your badge stays on the dashboard for good."
                    } else {
                        "Your monthly support keeps the badge active. You can cancel any time in Google Play."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (state.tier == SupporterTier.MONTHLY) {
                    OutlinedButton(onClick = { context.startActivity(SupporterBilling.manageSubscriptionIntent(context)) }) {
                        Text("Manage subscription")
                    }
                    LifetimeButton(state.lifetimePrice, label = "Switch to lifetime") {
                        activity?.let { SupporterBilling.launchPurchase(it, SupporterBilling.PRODUCT_LIFETIME) }
                    }
                    Text(
                        text = "If you switch to lifetime, cancel the monthly subscription in Google Play so it doesn't renew.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text("Support PixelFlow", style = MaterialTheme.typography.titleLargeEmphasized)
                Text(
                    text = "Every feature is free, with no ads. Optional support helps PixelFlow grow, and you get a supporter badge on your dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { activity?.let { SupporterBilling.launchPurchase(it, SupporterBilling.PRODUCT_MONTHLY) } },
                        enabled = state.monthlyPrice != null,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("support_monthly_button")
                    ) {
                        Text(state.monthlyPrice?.let { "$it / month" } ?: "Monthly")
                    }
                    LifetimeButton(state.lifetimePrice, label = null, modifier = Modifier.weight(1f)) {
                        activity?.let { SupporterBilling.launchPurchase(it, SupporterBilling.PRODUCT_LIFETIME) }
                    }
                }
                if (state.monthlyPrice == null && state.lifetimePrice == null) {
                    Text(
                        text = "Prices load from Google Play. Support is available in the version installed from the Play Store.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (state.hasPendingPurchase) {
                    Text(
                        text = "A payment is pending. Your badge appears once Google Play confirms it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("support_continue_free")) {
                    Text("Continue free · all features stay available")
                }
                Text(
                    text = "Monthly support renews automatically until you cancel in Google Play and keeps the badge active. " +
                        "Lifetime is a single payment and keeps the badge permanently. Support is optional and doesn't unlock features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LifetimeButton(price: String?, label: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = price != null,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier
            .height(56.dp)
            .testTag("support_lifetime_button")
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            when {
                label != null && price != null -> "$label · $price"
                label != null -> label
                price != null -> "$price lifetime"
                else -> "Lifetime"
            }
        )
    }
}

/** Small "Supporter" badge shown next to the app name. */
@Composable
fun SupporterBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.testTag("supporter_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Supporter",
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
