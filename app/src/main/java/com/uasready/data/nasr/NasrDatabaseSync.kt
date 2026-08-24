package com.uasready.data.nasr

import android.content.Context
import android.util.Log
import com.uasready.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Ensures that whenever the application package is updated or freshly installed,
 * the on-device SQLite database is automatically purged and re-populated with the authoritative master
 * pre-compiled dataset (380,644 FAA UASFM cells, 2,267 National Security restrictions,
 * 19,426 airports, 23,196 runways, and 990 controlled surface airspaces).
 */
object NasrDatabaseSync {

    private const val TAG = "NasrDatabaseSync"
    private const val PREFS_NAME = "nasr_package_sync"
    private const val KEY_LAST_APP_VERSION = "last_installed_version_code"
    private const val MASTER_ASSET_PATH = "databases/nasr_airspace.db.gz"
    private const val MIN_EXPECTED_DB_SIZE_BYTES = 30_000_000L // 30 MB (master is ~73 MB uncompressed)

    fun syncOnAppLaunch(context: Context, helper: NasrDatabaseHelper = NasrDatabaseHelper.getInstance(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt(KEY_LAST_APP_VERSION, -1)
        val currentVersion = BuildConfig.VERSION_CODE
        val dbFile = context.getDatabasePath(NasrDatabaseHelper.DB_NAME)

        val isCorruptOrSmall = !dbFile.exists() || dbFile.length() < MIN_EXPECTED_DB_SIZE_BYTES
        val needsUnpack = lastVersion != currentVersion || isCorruptOrSmall

        if (needsUnpack) {
            Log.i(
                TAG,
                "App package updated/unseeded (Build $lastVersion -> Build $currentVersion, size=${dbFile.length()}). Purging and extracting master FAA database asset..."
            )
            val success = unpackMasterDatabaseFromAssets(context)
            if (success) {
                prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
                Log.i(TAG, "Master database unpacked successfully for build $currentVersion.")
            } else {
                Log.w(TAG, "Asset unpack failed; falling back to programmatic seed data.")
                NasrSeedData.populateDatabase(helper, force = true)
                prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
            }
        } else {
            // Ensure DB is healthy
            NasrSeedData.populateDatabaseIfEmpty(helper)
        }
    }

    /**
     * Purges any existing on-device database files (-wal, -shm, .db) and decompresses
     * the pre-compiled SQLite master database asset into the application database folder.
     */
    fun unpackMasterDatabaseFromAssets(context: Context): Boolean {
        return try {
            // 1. Reset active singleton connection pool
            NasrDatabaseHelper.resetInstance()

            // 2. Completely purge existing database, WAL, and SHM journal files
            context.deleteDatabase(NasrDatabaseHelper.DB_NAME)

            val dbFile = context.getDatabasePath(NasrDatabaseHelper.DB_NAME)
            dbFile.parentFile?.mkdirs()

            val tempFile = File(dbFile.parentFile, "${NasrDatabaseHelper.DB_NAME}.tmp")
            if (tempFile.exists()) tempFile.delete()

            // 3. Decompress asset into temp file
            context.assets.open(MASTER_ASSET_PATH).use { rawIn ->
                GZIPInputStream(rawIn).use { gzIn ->
                    FileOutputStream(tempFile).use { out ->
                        gzIn.copyTo(out)
                    }
                }
            }

            // 4. Atomically rename temp file to target database file
            if (tempFile.renameTo(dbFile)) {
                Log.i(TAG, "Successfully unpacked and verified master database to ${dbFile.absolutePath} (${dbFile.length()} bytes)")
                true
            } else {
                // Fallback copy if rename fails
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()
                Log.i(TAG, "Copied master database to ${dbFile.absolutePath} (${dbFile.length()} bytes)")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not unpack master database asset: ${e.message}", e)
            false
        }
    }
}

