package com.veracross.assignments

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class BackendBrowserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wv = WebView(this)
        setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("veracross://")) {
                    // Handle deep link
                    val uri = android.net.Uri.parse(url)
                    val cookie = uri.getQueryParameter("cookie")
                    if (!cookie.isNullOrBlank()) {
                        Prefs.saveCookie(this@BackendBrowserActivity, cookie)
                        Scheduler.schedule(this@BackendBrowserActivity)
                        startActivity(Intent(this@BackendBrowserActivity, MainActivity::class.java))
                        finish()
                    }
                    return true
                }
                return false
            }
        }
        val url = intent.getStringExtra("url") ?: "https://veracross-backend.onrender.com"
        wv.loadUrl(url)
    }
}