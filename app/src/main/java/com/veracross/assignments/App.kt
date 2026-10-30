package com.veracross.assignments

import android.app.Application
import com.norman.webviewup.lib.source.UpgradePackageSource
import com.norman.webviewup.lib.WebViewUpgrade

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Upgrade WebView to use Chrome's engine (before any WebView is created)
        try {
            val source = UpgradePackageSource("com.android.chrome")
            WebViewUpgrade.upgrade(source)
        } catch (e: Exception) {
            // Fallback to system WebView if upgrade fails
            e.printStackTrace()
        }
        instance = this
    }
    companion object {
        lateinit var instance: App
            private set
    }
}
