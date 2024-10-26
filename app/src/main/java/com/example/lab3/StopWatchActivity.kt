package com.example.lab3

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StopWatchActivity : AppCompatActivity() {

    private lateinit var timeTextView: TextView
    private var isRunning = false
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var savedTimes: MutableList<String> = mutableListOf()
    private lateinit var savedTimesTextView: TextView

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - startTime
            updateTimeDisplay()
            handler.postDelayed(this, 10) // Update every 10 milliseconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stop_watch)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        timeTextView = findViewById(R.id.timeTextView)
        val startButton: Button = findViewById(R.id.btnStart)
        val stopButton: Button = findViewById(R.id.btnStop)
        val saveButton: Button = findViewById(R.id.btnSave)
        val resetButton: Button = findViewById(R.id.btnReset)
        savedTimesTextView = findViewById(R.id.savedTimesTextView)

        startButton.setOnClickListener {
            if (!isRunning) {
                startTime = System.currentTimeMillis() - elapsedTime
                handler.post(updateTimeRunnable)
                isRunning = true
            }
        }

        stopButton.setOnClickListener {
            if (isRunning) {
                handler.removeCallbacks(updateTimeRunnable)
                isRunning = false
            }
        }

        saveButton.setOnClickListener {
            if (!isRunning) {
                savedTimes.add(formatTime(elapsedTime))
                updateSavedTimesDisplay()
            }
        }

        resetButton.setOnClickListener {
            handler.removeCallbacks(updateTimeRunnable)
            isRunning = false
            elapsedTime = 0
            updateTimeDisplay()
            savedTimes.clear()
            updateSavedTimesDisplay()
        }
    }

    private fun updateTimeDisplay() {
        timeTextView.text = formatTime(elapsedTime)
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = timeInMillis / 3600000
        val minutes = (timeInMillis % 3600000) / 60000
        val seconds = (timeInMillis % 60000) / 1000
        val milliseconds = timeInMillis % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }

    private fun updateSavedTimesDisplay() {
        val savedTimesString = savedTimes.joinToString("\n")
        savedTimesTextView.text = savedTimesString
    }
}