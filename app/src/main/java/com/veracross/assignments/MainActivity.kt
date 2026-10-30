package com.veracross.assignments

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs.getCookie(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        Notifier.ensureChannels(this)
        requestNotifPermission()
        Scheduler.schedule(this)
        val list = findViewById<ListView>(R.id.list)
        findViewById<Button>(R.id.refreshBtn).setOnClickListener { refresh(list) }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        refresh(list)
    }

    private fun requestNotifPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun refresh(list: ListView) {
        val cookie = Prefs.getCookie(this) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val items = try { Scraper.fetchAssignments(cookie) } catch (e: Exception) { emptyList() }
            withContext(Dispatchers.Main) {
                if (items.isEmpty()) Toast.makeText(this@MainActivity, "No assignments / error", Toast.LENGTH_SHORT).show()
                val labels = items.map { "${it.due ?: "?"}  [${it.type}] ${it.title}  — ${it.course}" }
                list.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, labels)
            }
        }
    }
}
