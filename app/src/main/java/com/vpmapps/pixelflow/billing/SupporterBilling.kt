package com.vpmapps.pixelflow.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the user currently supports the app with. */
enum class SupporterTier { NONE, MONTHLY, LIFETIME }

data class SupporterState(
    val tier: SupporterTier = SupporterTier.NONE,
    /** Localized prices from Play, e.g. "$0.99" or "RSD 99"; null until loaded or if unavailable. */
    val monthlyPrice: String? = null,
    val lifetimePrice: String? = null,
    val isConnected: Boolean = false,
    val hasPendingPurchase: Boolean = false,
    /** One-off message for the UI (purchase failed, pending, thank you). */
    val message: String? = null
) {
    val isSupporter: Boolean get() = tier != SupporterTier.NONE
}

/**
 * Optional "support the app" purchases through Google Play Billing: a monthly subscription and a
 * one-time lifetime purchase. Every feature stays free; supporting only shows a badge.
 *
 * Purchases are restored from Play on every app start and resume, so reinstalls, new phones and
 * lapsed subscriptions are handled without any account. The last known tier is cached so the badge
 * shows immediately, even offline.
 */
object SupporterBilling {

    private const val TAG = "SupporterBilling"

    /** Product IDs, created in Play Console → Monetize → Products. */
    const val PRODUCT_MONTHLY = "supporter_monthly"
    const val PRODUCT_LIFETIME = "supporter_lifetime"

    private const val PREFS = "supporter_billing"
    private const val KEY_TIER = "tier"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(SupporterState())
    val state: StateFlow<SupporterState> = _state.asStateFlow()

    private var client: BillingClient? = null
    private lateinit var appContext: Context
    private var monthlyDetails: ProductDetails? = null
    private var lifetimeDetails: ProductDetails? = null

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext
        val cached = runCatching {
            SupporterTier.valueOf(prefs().getString(KEY_TIER, SupporterTier.NONE.name)!!)
        }.getOrDefault(SupporterTier.NONE)
        _state.update { it.copy(tier = cached) }

        client = BillingClient.newBuilder(appContext)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        connect()
    }

    /** Re-reads products and owned purchases, e.g. when the app returns to the foreground. */
    fun refresh() {
        val billing = client ?: return
        if (billing.isReady) {
            scope.launch {
                loadProducts()
                restorePurchases()
            }
        } else {
            connect()
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Opens Google Play's purchase sheet. Returns false if the product isn't available. */
    fun launchPurchase(activity: Activity, productId: String): Boolean {
        val billing = client ?: return false
        val details = if (productId == PRODUCT_MONTHLY) monthlyDetails else lifetimeDetails
        if (details == null || !billing.isReady) {
            _state.update { it.copy(message = "Purchases aren't available right now. Install PixelFlow from Google Play and try again.") }
            return false
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                if (productId == PRODUCT_MONTHLY) {
                    // The plain monthly base plan (an offer without an offerId)
                    val offer = details.subscriptionOfferDetails?.firstOrNull { it.offerId == null }
                        ?: details.subscriptionOfferDetails?.firstOrNull()
                    offer?.let { setOfferToken(it.offerToken) }
                }
            }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billing.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            _state.update { it.copy(message = "Couldn't open Google Play purchase. Please try again.") }
            return false
        }
        return true
    }

    /** Google Play's page where users can cancel or change the monthly subscription. */
    fun manageSubscriptionIntent(context: Context): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/account/subscriptions?sku=$PRODUCT_MONTHLY&package=${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _state.update { it.copy(isConnected = ok) }
                if (ok) {
                    scope.launch {
                        loadProducts()
                        restorePurchases()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() reconnects on the next call
                _state.update { it.copy(isConnected = false) }
            }
        })
    }

    private suspend fun loadProducts() {
        val billing = client ?: return
        monthlyDetails = queryProduct(billing, PRODUCT_MONTHLY, BillingClient.ProductType.SUBS)
        lifetimeDetails = queryProduct(billing, PRODUCT_LIFETIME, BillingClient.ProductType.INAPP)
        _state.update {
            it.copy(
                monthlyPrice = monthlyDetails?.subscriptionOfferDetails
                    ?.firstOrNull { offer -> offer.offerId == null }
                    ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice,
                lifetimePrice = lifetimeDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            )
        }
    }

    private suspend fun queryProduct(billing: BillingClient, productId: String, type: String): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()
                )
            )
            .build()
        val result = billing.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Product query for $productId failed: ${result.billingResult.debugMessage}")
        }
        return result.productDetailsList?.firstOrNull()
    }

    /** Asks Play what the user owns: active subscriptions and the lifetime purchase. */
    private suspend fun restorePurchases() {
        val billing = client ?: return
        val subs = billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val inApp = billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        val ok = subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            inApp.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (!ok) {
            Log.w(TAG, "Purchase query failed; keeping cached supporter state")
            return
        }
        handlePurchases(subs.purchasesList + inApp.purchasesList, replaceAll = true)
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                handlePurchases(purchases.orEmpty(), replaceAll = false)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restorePurchases() }
            else -> _state.update { it.copy(message = "Purchase didn't go through. You weren't charged.") }
        }
    }

    /**
     * @param replaceAll true when [purchases] is the full list of what the user owns (a restore),
     *        false when it only contains new purchases.
     */
    private suspend fun handlePurchases(purchases: List<Purchase>, replaceAll: Boolean) {
        var tier = if (replaceAll) SupporterTier.NONE else _state.value.tier
        var pending = false
        var newlyPurchased = false

        for (purchase in purchases) {
            val isLifetime = PRODUCT_LIFETIME in purchase.products
            val isMonthly = PRODUCT_MONTHLY in purchase.products
            if (!isLifetime && !isMonthly) continue

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    // Unacknowledged purchases are refunded by Google after 3 days
                    if (!purchase.isAcknowledged) {
                        val ack = client?.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        )
                        if (ack?.responseCode == BillingClient.BillingResponseCode.OK) newlyPurchased = true
                    }
                    tier = when {
                        isLifetime -> SupporterTier.LIFETIME
                        tier == SupporterTier.LIFETIME -> SupporterTier.LIFETIME
                        else -> SupporterTier.MONTHLY
                    }
                }
                Purchase.PurchaseState.PENDING -> pending = true
                else -> Unit
            }
        }

        prefs().edit().putString(KEY_TIER, tier.name).apply()
        _state.update {
            it.copy(
                tier = tier,
                hasPendingPurchase = pending,
                message = when {
                    newlyPurchased -> "Thank you for supporting PixelFlow!"
                    pending && !replaceAll -> "Your payment is pending. Your badge appears once Google Play confirms it."
                    else -> it.message
                }
            )
        }
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
