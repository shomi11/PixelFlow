package com.vpmapps.pixelflow.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ReviewPrompterTest {

    private lateinit var context: Context
    private val day = TimeUnit.DAYS.toMillis(1)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("review_prompter", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun notEligible_untilUsefulForAFewDays() {
        val installed = 1_000_000_000_000L
        assertFalse(ReviewPrompter.isEligible(context, now = installed)) // records first use

        repeat(5) { ReviewPrompter.recordSuccessfulRun(context) }
        assertFalse("too soon after install", ReviewPrompter.isEligible(context, now = installed + 2 * day))
        assertTrue(ReviewPrompter.isEligible(context, now = installed + 3 * day))
    }

    @Test
    fun notEligible_withFewSuccessfulRuns() {
        val installed = 1_000_000_000_000L
        ReviewPrompter.isEligible(context, now = installed)
        repeat(4) { ReviewPrompter.recordSuccessfulRun(context) }
        assertFalse(ReviewPrompter.isEligible(context, now = installed + 10 * day))
    }

    @Test
    fun cooldown_afterAPrompt() {
        val installed = 1_000_000_000_000L
        ReviewPrompter.isEligible(context, now = installed)
        repeat(5) { ReviewPrompter.recordSuccessfulRun(context) }
        val promptedAt = installed + 5 * day
        context.getSharedPreferences("review_prompter", Context.MODE_PRIVATE).edit()
            .putLong("last_prompt", promptedAt).commit()

        assertFalse(ReviewPrompter.isEligible(context, now = promptedAt + 30 * day))
        assertTrue(ReviewPrompter.isEligible(context, now = promptedAt + 120 * day))
    }
}
