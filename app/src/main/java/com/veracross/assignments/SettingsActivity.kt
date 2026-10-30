package com.veracross.assignments

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val edit = findViewById<EditText>(R.id.checksEdit)
        edit.setText(Prefs.getChecksPerDay(this).toString())
        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val n = edit.text.toString().toIntOrNull() ?: 4
            Prefs.saveChecksPerDay(this, n)
            Scheduler.schedule(this)
            finish()
        }
    }
}
