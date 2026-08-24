package com.uasready

import android.app.Application
import com.uasready.data.nasr.NasrDatabaseSync

class UASReadyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Automatically overwrite/re-seed database if the app package was updated
        NasrDatabaseSync.syncOnAppLaunch(this)
    }
}
