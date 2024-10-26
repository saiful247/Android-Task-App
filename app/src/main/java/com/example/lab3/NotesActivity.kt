package com.example.lab3

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import android.provider.Settings


class NotesActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var etSubject: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSave: Button
    private lateinit var notesTable: LinearLayout
    private val sharedPrefsFile = "notes_data"
    private val keyNotes = "saved_notes"
    private lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes)

        fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "notes_channel"
                val channelName = "Notes Notifications"
                val channelDescription = "Channel for notes notifications"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelDescription
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etSubject = findViewById(R.id.et_subject)
        etDate = findViewById(R.id.et_date)
        etTime = findViewById(R.id.et_time)
        etDescription = findViewById(R.id.et_description)
        btnSave = findViewById(R.id.btn_save)
        notesTable = findViewById(R.id.notes_table)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        etDate.setOnClickListener { showDatePickerDialog() }
        etTime.setOnClickListener { showTimePickerDialog() }

        btnSave.setOnClickListener {
            saveNote()
        }

        loadNotes()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf("android.permission.POST_NOTIFICATIONS"),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can schedule notifications now
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            etDate.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            etTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true)

        timePickerDialog.show()
    }

    /**
     * Create: Saves a new note to SharedPreferences and schedules a notification
     * This function handles input validation, JSON conversion, and SharedPreferences storage
     */
    private fun saveNote() {
        val subject = etSubject.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (subject.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty() && description.isNotEmpty()) {
            val newNote = JSONObject().apply {
                put("subject", subject)
                put("date", date)
                put("time", time)
                put("description", description)
            }

            val sharedPreferences = getSharedPreferences(sharedPrefsFile, Context.MODE_PRIVATE)
            val notesString = sharedPreferences.getString(keyNotes, "[]")
            val notesArray = JSONArray(notesString)

            notesArray.put(newNote)

            with(sharedPreferences.edit()) {
                putString(keyNotes, notesArray.toString())
                apply()
            }

            try {
                scheduleNotification(newNote)
                Toast.makeText(this, "Note saved successfully with notification", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("NotesActivity", "Error scheduling notification", e)
                Toast.makeText(this, "Note saved, but notification couldn't be scheduled: ${e.message}", Toast.LENGTH_LONG).show()
            }

            clearInputFields()
            loadNotes()
        } else {
            val emptyFields = mutableListOf<String>()
            if (subject.isEmpty()) emptyFields.add("Subject")
            if (date.isEmpty()) emptyFields.add("Date")
            if (time.isEmpty()) emptyFields.add("Time")
            if (description.isEmpty()) emptyFields.add("Description")

            val errorMessage = "Please fill in the following fields: ${emptyFields.joinToString(", ")}"
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification(note: JSONObject) {
        val dateString = note.getString("date")
        val timeString = note.getString("time")
        val subject = note.getString("subject")

        val calendar = Calendar.getInstance()
        val dateParts = dateString.split("/")
        val timeParts = timeString.split(":")

        if (dateParts.size != 3 || timeParts.size != 2) {
            throw IllegalArgumentException("Invalid date or time format")
        }

        try {
            calendar.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
            calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
            calendar.set(Calendar.YEAR, dateParts[2].toInt())
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid date or time values: ${e.message}")
        }

        val notificationIntent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("subject", subject)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                this,
                note.hashCode(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                note.hashCode(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Check for SCHEDULE_EXACT_ALARM permission for Android 12+ before setting the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val alarmPermissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(alarmPermissionIntent)
            return // Return early since we can't schedule the alarm yet
        }

        // Try scheduling the alarm with exact timing
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to schedule exact alarm. Notification might be delayed.", Toast.LENGTH_LONG).show()
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }


    /**
     * Read: Loads all saved notes from SharedPreferences and displays them in the UI
     * This function reads the JSON array of notes and creates views for each note
     */
    private fun loadNotes() {
        notesTable.removeAllViews()

        val sharedPreferences = getSharedPreferences(sharedPrefsFile, Context.MODE_PRIVATE)
        val notesString = sharedPreferences.getString(keyNotes, "[]")
        val notesArray = JSONArray(notesString)

        for (i in 0 until notesArray.length()) {
            val noteObject = notesArray.getJSONObject(i)
            val noteView = createNoteView(noteObject, i)
            notesTable.addView(noteView)
        }
    }

    private fun createNoteView(note: JSONObject, index: Int): LinearLayout {
        val noteContainer = LinearLayout(this)
        noteContainer.orientation = LinearLayout.VERTICAL
        noteContainer.setPadding(16, 16, 16, 16)
        noteContainer.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)

        val subjectText = TextView(this)
        subjectText.text = "Subject: ${note.getString("subject")}"
        subjectText.textSize = 16f
        noteContainer.addView(subjectText)

        val dateText = TextView(this)
        dateText.text = "Date: ${note.getString("date")}"
        dateText.textSize = 16f
        noteContainer.addView(dateText)

        val timeText = TextView(this)
        timeText.text = "Time: ${note.getString("time")}"
        timeText.textSize = 16f
        noteContainer.addView(timeText)

        val descriptionText = TextView(this)
        descriptionText.text = "Description: ${note.getString("description")}"
        descriptionText.textSize = 16f
        noteContainer.addView(descriptionText)

        val buttonLayout = LinearLayout(this)
        buttonLayout.orientation = LinearLayout.HORIZONTAL
        noteContainer.addView(buttonLayout)

        val editButton = Button(this)
        editButton.text = "Edit"
        buttonLayout.addView(editButton)

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        buttonLayout.addView(deleteButton)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(16, 16, 16, 16)
        noteContainer.layoutParams = layoutParams

        /**
         * Update: Populates the input fields with an existing note's data for editing
         * This function is called when the user clicks the "Edit" button on a note
         * After populating the fields, it deletes the old note to be replaced by the edited version
         */
        // This functionality is part of the editButton.setOnClickListener in createNoteView function
        editButton.setOnClickListener {
            etSubject.setText(note.getString("subject"))
            etDate.setText(note.getString("date"))
            etTime.setText(note.getString("time"))
            etDescription.setText(note.getString("description"))

            deleteNoteAt(index)
        }

        deleteButton.setOnClickListener {
            deleteNoteAt(index)
        }

        return noteContainer
    }

    /**
     * Delete: Removes a note from the saved notes in SharedPreferences
     * This function is called when the user clicks the "Delete" button on a note
     */
    private fun deleteNoteAt(index: Int) {
        val sharedPreferences = getSharedPreferences(sharedPrefsFile, Context.MODE_PRIVATE)
        val notesString = sharedPreferences.getString(keyNotes, "[]")
        val notesArray = JSONArray(notesString)

        val updatedNotesArray = JSONArray()
        for (i in 0 until notesArray.length()) {
            if (i != index) {
                updatedNotesArray.put(notesArray.getJSONObject(i))
            }
        }

        sharedPreferences.edit().putString(keyNotes, updatedNotesArray.toString()).apply()

        loadNotes()
    }

    private fun clearInputFields() {
        etSubject.text.clear()
        etDate.text.clear()
        etTime.text.clear()
        etDescription.text.clear()
    }
}