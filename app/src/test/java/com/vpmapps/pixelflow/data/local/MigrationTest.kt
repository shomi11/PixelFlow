package com.vpmapps.pixelflow.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Makes sure existing users keep their routines when the database schema changes.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate3To4_keepsRoutinesAndDefaultsToAll() {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                "INSERT INTO routines (id, name, isEnabled, triggers, actions, description, iconName, colorHex, " +
                    "lastExecutedTimestamp, lastExecutionStatus, restoreOnEnd, createdAt) " +
                    "VALUES (1, 'Bedtime', 1, '[]', '[]', '', 'schedule', '#3871E0', 0, 'IDLE', 1, 0)"
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT name, matchMode FROM routines WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Bedtime", cursor.getString(0))
                assertEquals("ALL", cursor.getString(1))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
