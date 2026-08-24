package com.uasready.data.nasr
 
import android.content.Context
import android.util.Log
import com.uasready.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Ensures that whenever the application package is updated or freshly installed,
 * the on-device SQLite database is automatically populated with the authoritative master
 * pre-compiled dataset (380,644 FAA UASFM cells, 2,267 National Security restrictions,
 * 931 airports, and 929 controlled surface airspaces).
 */
object NasrDatabaseSync {

    private const val TAG = "NasrDatabaseSync"
    private const val PREFS_NAME = "nasr_package_sync"
    private const val KEY_LAST_APP_VERSION = "last_installed_version_code"
    private const val MASTER_ASSET_PATH = "databases/nasr_airspace.db.gz"

    fun syncOnAppLaunch(context: Context, helper: NasrDatabaseHelper = NasrDatabaseHelper.getInstance(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt(KEY_LAST_APP_VERSION, -1)
        val currentVersion = BuildConfig.VERSION_CODE
        val dbFile = context.getDatabasePath(NasrDatabaseHelper.DB_NAME)

        val needsUnpack = lastVersion != currentVersion || !dbFile.exists() || dbFile.length() < 1000000L

        if (needsUnpack) {
            Log.i(
                TAG,
                "App package updated/unseeded (Build $lastVersion -> Build $currentVersion). Extracting master FAA database asset..."
            )
            val success = unpackMasterDatabaseFromAssets(context)
            if (success) {
                prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
                Log.i(TAG, "Master database unpacked successfully for build $currentVersion.")
            } else {
                Log.w(TAG, "Asset unpack skipped/failed; falling back to programmatic seed data.")
                NasrSeedData.populateDatabase(helper, force = true)
                prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
            }
        } else {
            // Ensure DB is healthy
            NasrSeedData.populateDatabaseIfEmpty(helper)
        }
    }

    /**
     * Decompresses the pre-compiled SQLite master database asset into the application database folder.
     */
    fun unpackMasterDatabaseFromAssets(context: Context): Boolean {
        return try {
            NasrDatabaseHelper.resetInstance()
            val dbFile = context.getDatabasePath(NasrDatabaseHelper.DB_NAME)
            dbFile.parentFile?.mkdirs()

            context.assets.open(MASTER_ASSET_PATH).use { rawIn ->
                GZIPInputStream(rawIn).use { gzIn ->
                    FileOutputStream(dbFile).use { out ->
                        gzIn.copyTo(out)
                    }
                }
            }
            Log.i(TAG, "Unpacked master database to ${dbFile.absolutePath} (${dbFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not unpack master database asset: ${e.message}")
            false
        }
    }
}

