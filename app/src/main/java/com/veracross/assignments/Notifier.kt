package com.veracross.assignments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CH_TEST = "test_due"
    const val CH_HW = "hw_due"
    const val CH_NEW = "new_item"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CH_TEST, "Tests due", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tests due tomorrow (cannot be dismissed)"
                setShowBadge(true)
            })
            nm.createNotificationChannel(NotificationChannel(CH_HW, "Homework due", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Homework due tomorrow"
            })
            nm.createNotificationChannel(NotificationChannel(CH_NEW, "New assignments", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New homework or tests appeared"
            })
        }
    }

    private fun openIntent(ctx: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://portals.veracross.com/maimonides/student/student/upcoming-assignments"))
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun notifyTestDue(ctx: Context, a: Assignment) {
        val n = NotificationCompat.Builder(ctx, CH_TEST)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("TEST due tomorrow: ${a.title}")
            .setContentText("${a.course} — ${a.type}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${a.course}\n${a.type}\nDue: ${a.due}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openIntent(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(a.itemId.toInt().let { if (it == 0) a.title.hashCode() else it }, n)
    }

    fun notifyHwDue(ctx: Context, a: Assignment) {
        val n = NotificationCompat.Builder(ctx, CH_HW)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Homework due tomorrow: ${a.title}")
            .setContentText("${a.course} — ${a.type}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Course: ${a.course}\nType: ${a.type}\nDue: ${a.due}\nTeacher: ${a.teacher ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(openIntent(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(a.title.hashCode(), n)
    }

    fun notifyNewItem(ctx: Context, a: Assignment) {
        val n = NotificationCompat.Builder(ctx, CH_NEW)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("New ${a.type}: ${a.title}")
            .setContentText("${a.course} — due ${a.due}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openIntent(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(("new_" + a.itemId).hashCode(), n)
    }
}
