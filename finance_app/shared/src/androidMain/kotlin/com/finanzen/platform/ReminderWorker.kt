package com.finanzen.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

internal const val REMINDER_CHANNEL_ID = "finanzen_reminders"
internal const val KEY_NOTIF_ID = "notif_id"
internal const val KEY_TITLE = "title"
internal const val KEY_BODY = "body"

/** Crea el canal de recordatorios (idempotente). Necesario en API 26+. */
internal fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = context.getSystemService(NotificationManager::class.java) ?: return
    if (mgr.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        "Recordatorios",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply { description = "Avisos de cobros de suscripciones y gastos recurrentes." }
    mgr.createNotificationChannel(channel)
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

/** Publica una notificación de recordatorio ya. Compartido por el Worker y por notifyNow. */
internal fun postReminderNotification(context: Context, notifId: Int, title: String, body: String) {
    ensureReminderChannel(context)
    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        // ponytail: icono del sistema como placeholder. Reemplazar por el ic_stat monocromo en B1 (iconos).
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    if (hasNotificationPermission(context)) {
        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}

/** Worker disparado por WorkManager en la fecha del recordatorio; publica la notificación. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        val notifId = inputData.getLong(KEY_NOTIF_ID, 0L).toInt()
        val title = inputData.getString(KEY_TITLE) ?: "Cauce"
        val body = inputData.getString(KEY_BODY).orEmpty()
        postReminderNotification(ctx, notifId, title, body)
        return Result.success()
    }
}
