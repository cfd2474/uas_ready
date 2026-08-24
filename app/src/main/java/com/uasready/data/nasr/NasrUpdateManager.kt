package com.uasready.data.nasr

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class AiracUpdateStatus {
    object Idle : AiracUpdateStatus()
    object Checking : AiracUpdateStatus()
    data class UpdateAvailable(val newCycle: String, val daysRemaining: Int) : AiracUpdateStatus()
    object UpToDate : AiracUpdateStatus()
    data class Downloading(val progressPercent: Int) : AiracUpdateStatus()
    object Rebuilding : AiracUpdateStatus()
    data class Success(val cycleName: String) : AiracUpdateStatus()
    data class Error(val message: String) : AiracUpdateStatus()
}

class NasrUpdateManager(
    private val context: Context,
    private val dbHelper: NasrDatabaseHelper = NasrDatabaseHelper.getInstance(context)
) {
    companion object {
        private const val TAG = "NasrUpdateManager"
    }

    /**
     * Checks if a new AIRAC cycle is available based on 28-day calendar.
     */
    suspend fun checkForUpdates(): AiracUpdateStatus = withContext(Dispatchers.IO) {
        try {
            val currentCycle = dbHelper.getMetaValue("airac_cycle") ?: "2608"
            val calculated = AiracCycleCalculator.calculateCycleInfo()

            dbHelper.setMetaValue("last_checked_epoch_ms", System.currentTimeMillis().toString())

            if (calculated.cycleName != currentCycle || calculated.isExpired) {
                AiracUpdateStatus.UpdateAvailable(calculated.cycleName, calculated.daysUntilExpiry)
            } else {
                AiracUpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for AIRAC update: ${e.message}", e)
            AiracUpdateStatus.Error("Failed to check for update: ${e.localizedMessage}")
        }
    }

    /**
     * Downloads/populates an updated AIRAC cycle database into a temporary DB,
     * validates database integrity via PRAGMA integrity_check, and atomically swaps it.
     */
    suspend fun performUpdate(): AiracUpdateStatus = withContext(Dispatchers.IO) {
        val targetCycle = AiracCycleCalculator.calculateCycleInfo()
        val tempDbFile = File(context.getDatabasePath("nasr_temp.db").path)
        val activeDbFile = File(context.getDatabasePath(NasrDatabaseHelper.DB_NAME).path)

        try {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }

            // 1. Create and populate temp database
            val tempDb = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null)
            val tempHelper = NasrDatabaseHelper(context, "nasr_temp.db")
            NasrSeedData.populateDatabaseIfEmpty(tempHelper)
            tempDb.close()

            // 2. Perform integrity check on temp DB
            val verifyDb = SQLiteDatabase.openDatabase(tempDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            var isIntegrityValid = false
            verifyDb.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    isIntegrityValid = result.equals("ok", ignoreCase = true)
                }
            }
            verifyDb.close()

            if (!isIntegrityValid) {
                tempDbFile.delete()
                return@withContext AiracUpdateStatus.Error("Database integrity verification failed.")
            }

            // 3. Close active database and perform atomic swap
            dbHelper.close()
            if (activeDbFile.exists()) {
                activeDbFile.delete()
            }
            val renamed = tempDbFile.renameTo(activeDbFile)
            if (!renamed) {
                // Fallback copy
                tempDbFile.copyTo(activeDbFile, overwrite = true)
                tempDbFile.delete()
            }

            // 4. Update metadata in swapped active database
            dbHelper.setMetaValue("airac_cycle", targetCycle.cycleName)
            dbHelper.setMetaValue("effective_epoch_ms", targetCycle.effectiveEpochMs.toString())
            dbHelper.setMetaValue("expire_epoch_ms", targetCycle.expireEpochMs.toString())
            dbHelper.setMetaValue("last_updated_epoch_ms", System.currentTimeMillis().toString())

            Log.i(TAG, "Successfully performed atomic swap to AIRAC Cycle ${targetCycle.cycleName}")
            AiracUpdateStatus.Success(targetCycle.cycleName)
        } catch (e: Exception) {
            Log.e(TAG, "Atomic database swap failed: ${e.message}", e)
            if (tempDbFile.exists()) tempDbFile.delete()
            AiracUpdateStatus.Error("Update failed: ${e.localizedMessage}")
        }
    }

    /**
     * Cleans and rebuilds the local database from scratch with current seed data.
     */
    suspend fun rebuildDatabase(): AiracUpdateStatus = withContext(Dispatchers.IO) {
        try {
            dbHelper.resetDatabase()
            NasrSeedData.populateDatabaseIfEmpty(dbHelper)
            val cycle = dbHelper.getMetaValue("airac_cycle") ?: "2608"
            AiracUpdateStatus.Success(cycle)
        } catch (e: Exception) {
            Log.e(TAG, "Rebuild database failed: ${e.message}", e)
            AiracUpdateStatus.Error("Rebuild failed: ${e.localizedMessage}")
        }
    }
}
