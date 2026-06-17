package com.progresswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.RemoteViews
import java.util.Calendar

class ProgressWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE = "com.progresswidget.ACTION_UPDATE"
        const val PREFS_NAME = "ProgressWidgetPrefs"
        const val KEY_WAKE_H = "wake_h"
        const val KEY_WAKE_M = "wake_m"
        const val KEY_SLEEP_H = "sleep_h"
        const val KEY_SLEEP_M = "sleep_m"

        // Pulse: cycle through 0-7 based on seconds
        fun getPulsePhase(): Float {
            val seconds = (System.currentTimeMillis() / 1000) % 8
            return seconds / 8f
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ProgressWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val wakeH = prefs.getInt(KEY_WAKE_H, 7)
            val wakeM = prefs.getInt(KEY_WAKE_M, 0)
            val sleepH = prefs.getInt(KEY_SLEEP_H, 23)
            val sleepM = prefs.getInt(KEY_SLEEP_M, 0)

            for (id in ids) {
                val opts = manager.getAppWidgetOptions(id)
                val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
                val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)

                // Convert dp to px (approximate, use 2.75 density for most phones)
                val density = context.resources.displayMetrics.density
                val widthPx = (minW * density).toInt().coerceAtLeast(600)
                val heightPx = (minH * density).toInt().coerceAtLeast(300)

                val bitmap = WidgetRenderer.render(
                    widthPx, heightPx,
                    wakeH, wakeM, sleepH, sleepM,
                    getPulsePhase()
                )

                val views = RemoteViews(context.packageName, R.layout.widget_loading)
                views.setImageViewBitmap(R.id.widget_image, bitmap)

                // Tap widget to open settings
                val settingsIntent = Intent(context, SettingsActivity::class.java)
                val pi = PendingIntent.getActivity(context, 0, settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image, pi)

                manager.updateAppWidget(id, views)
            }
        }

        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProgressWidgetReceiver::class.java).apply {
                action = ACTION_UPDATE
            }
            val pi = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            // Fire every 5 seconds for the pulse animation
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 5000,
                5000,
                pi
            )
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProgressWidgetReceiver::class.java).apply {
                action = ACTION_UPDATE
            }
            val pi = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pi)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
        scheduleAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        scheduleAlarm(context)
    }

    override fun onDisabled(context: Context) {
        cancelAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateAllWidgets(context)
    }
}
