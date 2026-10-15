package com.vpmapps.pixelflow.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit

/**
 * Asks for a Play review the way Google recommends:
 * - only after the app has proven useful (installed a few days, several routines ran successfully),
 * - at a calm moment (opening the dashboard), never right after an error or in the middle of a task,
 * - without asking "do you like the app?" first (review gating isn't allowed) and without rewards,
 * - rarely: at most once every [COOLDOWN_DAYS]; Play also applies its own quota and may show nothing.
 *
 * Users can always rate on their own from the menu via [openStoreListing].
 */
object ReviewPrompter {

    private const val TAG = "ReviewPrompter"
    private const val PREFS = "review_prompter"
    private const val KEY_FIRST_SEEN = "first_seen"
    private const val KEY_SUCCESSFUL_RUNS = "successful_runs"
    private const val KEY_LAST_PROMPT = "last_prompt"

    private const val MIN_DAYS_INSTALLED = 3L
    private const val MIN_SUCCESSFUL_RUNS = 5
    private const val COOLDOWN_DAYS = 120L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Called when a routine finishes successfully; usefulness is what earns a review request. */
    fun recordSuccessfulRun(context: Context) {
        val prefs = prefs(context)
        prefs.edit().putInt(KEY_SUCCESSFUL_RUNS, prefs.getInt(KEY_SUCCESSFUL_RUNS, 0) + 1).apply()
    }

    fun isEligible(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = prefs(context)
        val firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L).takeIf { it > 0 } ?: now.also {
            prefs.edit().putLong(KEY_FIRST_SEEN, it).apply()
        }
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0L)
        return now - firstSeen >= TimeUnit.DAYS.toMillis(MIN_DAYS_INSTALLED) &&
            prefs.getInt(KEY_SUCCESSFUL_RUNS, 0) >= MIN_SUCCESSFUL_RUNS &&
            (lastPrompt == 0L || now - lastPrompt >= TimeUnit.DAYS.toMillis(COOLDOWN_DAYS))
    }

    /**
     * Shows Google Play's in-app review sheet if the user is eligible. Play decides whether the
     * sheet actually appears and never reports whether the user left a review.
     */
    suspend fun maybeAskForReview(activity: Activity) {
        if (!isEligible(activity)) return
        prefs(activity).edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
        try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
        } catch (e: Exception) {
            Log.w(TAG, "In-app review unavailable", e)
        }
    }

    /** Opens the Play Store listing so the user can rate whenever they want. */
    fun openStoreListing(context: Context) {
        val packageName = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
