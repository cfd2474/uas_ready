package com.uasready.data.nasr

import android.content.Context
import android.util.Log
import com.uasready.BuildConfig

/**
 * Ensures that whenever the application package is updated or freshly installed,
 * the on-device SQLite database is automatically populated with the authoritative master
 * pre-compiled dataset (380,644 FAA UASFM cells, 2,267 National Security restrictions,
 * 19,426 airports, 23,196 runways, and 990 controlled surface airspaces).
 */
object NasrDatabaseSync {

    private const val TAG = "NasrDatabaseSync"
    private const val PREFS_NAME = "nasr_package_sync"
    private const val KEY_LAST_APP_VERSION = "last_installed_version_code"

    fun syncOnAppLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getInt(KEY_LAST_APP_VERSION, -1)
        val currentVersion = BuildConfig.VERSION_CODE

        val needsForceExtract = lastVersion != currentVersion

        if (needsForceExtract) {
            Log.i(TAG, "New app version detected (Build $lastVersion -> Build $currentVersion). Resetting database...")
        }

        val success = NasrDatabaseHelper.ensureMasterDatabaseExtracted(context, forceExtract = needsForceExtract)
        if (success) {
            prefs.edit().putInt(KEY_LAST_APP_VERSION, currentVersion).apply()
            val helper = NasrDatabaseHelper.getInstance(context)
            Log.i(TAG, "Master DB Ready: ${helper.getUasfmCount()} UASFM cells, ${helper.getAirportCount()} airports.")
        } else {
            Log.e(TAG, "Master DB extraction failed!")
        }
    }
}

