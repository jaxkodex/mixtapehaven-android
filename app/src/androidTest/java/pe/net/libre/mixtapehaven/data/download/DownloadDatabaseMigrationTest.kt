package pe.net.libre.mixtapehaven.data.download

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays the exported schema history against real SQLite. Guards the additive video-download
 * migrations: v3 -> v4 (status column, incomplete rows surfaced as FAILED) and v4 -> v5
 * (attempts retry-budget counter).
 */
@RunWith(AndroidJUnit4::class)
class DownloadDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DownloadDatabase::class.java,
    )

    @Test
    fun migratesV3ToV5_completeRowKeepsDataAndGainsDefaults() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO downloaded_videos " +
                    "(id, title, kind, seriesName, seasonEpisodeLabel, runtimeLabel, posterUrl, " +
                    "artColorArgb, qualityLabel, filePath, sizeBytes, complete) " +
                    "VALUES ('abc', 'Dracula', 'MOVIE', NULL, NULL, '1h 14m', NULL, " +
                    "0, '720p', '/data/x.ts', 1000, 1)",
            )
        }

        helper.runMigrationsAndValidate(DB, 5, true, *DownloadDatabase.MIGRATIONS).use { db ->
            db.query("SELECT title, status, attempts, complete FROM downloaded_videos WHERE id = 'abc'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Dracula", cursor.getString(0))
                    assertEquals(VideoDownloadStatus.COMPLETE.name, cursor.getString(1))
                    assertEquals(0, cursor.getInt(2))
                    assertEquals(1, cursor.getInt(3))
                }
        }
    }

    @Test
    fun migratesV3ToV5_incompleteRowSurfacesAsFailedForRetry() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO downloaded_videos " +
                    "(id, title, kind, seriesName, seasonEpisodeLabel, runtimeLabel, posterUrl, " +
                    "artColorArgb, qualityLabel, filePath, sizeBytes, complete) " +
                    "VALUES ('mid', 'Half Movie', 'MOVIE', NULL, NULL, '2h', NULL, " +
                    "0, '480p', '', 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB, 5, true, *DownloadDatabase.MIGRATIONS).use { db ->
            db.query("SELECT status FROM downloaded_videos WHERE id = 'mid'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(VideoDownloadStatus.FAILED.name, cursor.getString(0))
            }
        }
    }

    @Test
    fun migratesV3ToV5_audioAndProgressTablesSurvive() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO video_progress " +
                    "(id, positionMs, runtimeMs, updatedAtMs, title, kind, seriesName, seriesId, " +
                    "seasonEpisodeLabel, posterUrl, backdropUrl, artColorArgb) " +
                    "VALUES ('ep1', 5000, 60000, 1, 'Pilot', 'EPISODE', NULL, NULL, NULL, NULL, NULL, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB, 5, true, *DownloadDatabase.MIGRATIONS).use { db ->
            db.query("SELECT positionMs FROM video_progress WHERE id = 'ep1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(5000, cursor.getInt(0))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
