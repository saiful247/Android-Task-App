package com.example.lab3

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the stopWatchButton and set its click listener
        val stopWatchButton: Button = findViewById(R.id.stopWatchButton)
        stopWatchButton.setOnClickListener {
            // Navigate to StopWatchActivity
            val intent = Intent(this, StopWatchActivity::class.java)
            startActivity(intent)
        }

        // Find the taskManagerButton and set its click listener
        val taskManagerButton: Button = findViewById(R.id.taskManagerButton)
        taskManagerButton.setOnClickListener {
            // Navigate to NotesActivity
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }
    }
}
