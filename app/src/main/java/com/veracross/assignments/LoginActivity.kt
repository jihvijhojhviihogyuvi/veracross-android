package com.veracross.assignments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cefrium.CefriumBrowser

class LoginActivity : AppCompatActivity() {
    private var autoProceeded = false
    private val handler = Handler(Looper.getMainLooper())
    private var browser: CefriumBrowser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val container = findViewById<FrameLayout>(R.id.browserContainer)
        browser = CefriumBrowser.createWithSurface(this)
        val surfaceContainer = browser!!.getSurfaceContainer()
        container.addView(surfaceContainer)

        browser!!.loadUrl("https://portals.veracross.com/maimonides/student")

        // Poll for login completion
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (autoProceeded) return
                browser?.getCookies("https://portals.veracross.com") { cookies ->
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    val realCookies = cookieStr.split(";").filter { c ->
                        val name = c.trim().substringBefore("=").trim().lowercase()
                        name != "_veracross_session"
                    }.joinToString(";")
                    if (realCookies.isNotBlank() && realCookies.contains("JSESSIONID") || realCookies.contains("session")) {
                        if (!autoProceeded) {
                            autoProceeded = true
                            handler.removeCallbacksAndMessages(null)
                            Prefs.saveCookie(this@LoginActivity, realCookies)
                            Scheduler.schedule(this@LoginActivity)
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
                handler.postDelayed(this, 3000)
            }
        }, 3000)

        findViewById<Button>(R.id.doneBtn).setOnClickListener {
            browser?.getCookies("https://portals.veracross.com") { cookies ->
                val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                val realCookies = cookieStr.split(";").filter { c ->
                    val name = c.trim().substringBefore("=").trim().lowercase()
                    name != "_veracross_session"
                }.joinToString(";")
                if (realCookies.isNotBlank()) {
                    autoProceeded = true
                    handler.removeCallbacksAndMessages(null)
                    Prefs.saveCookie(this, realCookies)
                    Scheduler.schedule(this)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "No cookies yet.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.pasteBtn).setOnClickListener {
            val input = EditText(this)
            input.hint = "Paste cookie string"
            AlertDialog.Builder(this)
                .setTitle("Paste cookies")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val cookie = input.text.toString().trim()
                    if (cookie.isNotBlank()) {
                        autoProceeded = true
                        handler.removeCallbacksAndMessages(null)
                        Prefs.saveCookie(this, cookie)
                        Scheduler.schedule(this)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        browser?.let {
            // CefriumBrowser cleanup
        }
    }
}
