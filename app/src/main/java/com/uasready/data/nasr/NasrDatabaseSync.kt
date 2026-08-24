package com.uasready.data.nasr
 
import android.content.Context
import android.util.Log
import com.uasready.BuildConfig

/**
 * Ensures that whenever the application package is updated or re-installed,
 * the on-device SQLite database is automatically overwritten with the latest packaged seed dataset,
 * and checked for AIRAC currency.
 */
object NasrDatabaseSync {

    private const val TAG = "NasrDatabaseSync"
    private const val PREFS_NAME = "nasr_package_sync"
    private const val KEY_LAST_APP_VERSION = "last_installed_version_code"

    fun syncOnAppLaunch(context: Context, helper: NasrDatabaseHelper = NasrDatabaseHelper(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt(KEY_LAST_APP_VERSION, -1)
        val currentVersion = BuildConfig.VERSION_CODE

        if (lastVersion != currentVersion) {
            Log.i(
                TAG,
                "App package updated (Build $lastVersion -> Build $currentVersion). Overwriting SQLite database with latest app package dataset..."
            )
            try {
                NasrSeedData.populateDatabase(helper, force = true)
                prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
                Log.i(TAG, "Database overwrite and seed completed successfully for build $currentVersion.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to overwrite database on app update: ${e.message}", e)
            }
        } else {
            // Ensure DB is not empty or missing CONUS data
            NasrSeedData.populateDatabaseIfEmpty(helper)
        }
    }
}
