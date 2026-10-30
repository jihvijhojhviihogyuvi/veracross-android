package com.veracross.assignments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AuthCallbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri: Uri? = intent?.data
        val cookie = uri?.getQueryParameter("cookie")
        if (!cookie.isNullOrBlank()) {
            Prefs.saveCookie(this, cookie)
            Scheduler.schedule(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // No cookie; go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
