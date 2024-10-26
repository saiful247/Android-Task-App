package com.example.lab3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "notes_notification_channel"
        const val CHANNEL_NAME = "Notes Notifications"
        const val CHANNEL_DESC = "Notifications for notes reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Check if permission to post notifications is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted; you can log or handle accordingly
            return
        }

        val subject = intent.getStringExtra("subject") ?: "Reminder"

        // Create notification channel if on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent for the notification tap action (optional)
        val tapIntent = Intent(context, NotesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setContentTitle("Note Reminder")
            .setContentText("Reminder for: $subject")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for visibility
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped
            .build()

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(subject.hashCode(), notification) // Use subject hashCode to uniquely identify each notification
            } catch (e: SecurityException) {
                // Log or handle the case where notification permission was not granted
                e.printStackTrace()
            }
        }
    }
}
