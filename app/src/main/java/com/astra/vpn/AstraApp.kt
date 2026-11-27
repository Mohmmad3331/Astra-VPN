package com.astra.vpn

import android.app.Application
import java.io.File

class AstraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Drop any crash report left by earlier builds.
        try {
            File(filesDir, "last_crash.txt").delete()
        } catch (_: Throwable) {
        }
    }
}
