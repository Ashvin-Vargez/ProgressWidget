package com.progresswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews

class ProgressWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE     = "com.progresswidget.ACTION_UPDATE"
        const val ACTION_PREV_MONTH = "com.progresswidget.PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.progresswidget.NEXT_MONTH"
        const val PREFS_NAME        = "ProgressWidgetPrefs"
        const val KEY_WAKE_H        = "wake_h"
        const val KEY_WAKE_M        = "wake_m"
        const val KEY_SLEEP_H       = "sleep_h"
        const val KEY_SLEEP_M       = "sleep_m"
        const val KEY_MONTH_OFFSET  = "month_offset"

        fun getPulsePhase(): Float =
            ((System.currentTimeMillis() / 1000L) % 8L) / 8f

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ProgressWidgetReceiver::class.java))
            if (ids.isEmpty()) return

            val prefs       = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val wakeH       = prefs.getInt(KEY_WAKE_H, 7)
            val wakeM       = prefs.getInt(KEY_WAKE_M, 0)
            val sleepH      = prefs.getInt(KEY_SLEEP_H, 23)
            val sleepM      = prefs.getInt(KEY_SLEEP_M, 0)
            val weekStart   = prefs.getInt(SettingsActivity.KEY_WEEK_START, 1)
            val monthOffset = prefs.getInt(KEY_MONTH_OFFSET, 0)
            val density     = context.resources.displayMetrics.density

            for (id in ids) {
                val opts = manager.getAppWidgetOptions(id)
                val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
                val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
                val wPx  = (minW * density).toInt().coerceAtLeast(600)
                val hPx  = (minH * density).toInt().coerceAtLeast(300)

                val bitmap = WidgetRenderer.render(
                    context, wPx, hPx,
                    wakeH, wakeM, sleepH, sleepM,
                    getPulsePhase(), weekStart, monthOffset)

                val views = RemoteViews(context.packageName, R.layout.widget_loading)
                views.setImageViewBitmap(R.id.widget_image, bitmap)

                // Whole widget taps → settings
                val settingsIntent = Intent(context, SettingsActivity::class.java)
                val settingsPi = PendingIntent.getActivity(context, 0, settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.tap_settings, settingsPi)

                // Month nav
                val prevPi = PendingIntent.getBroadcast(context, 20,
                    Intent(context, ProgressWidgetReceiver::class.java)
                        .apply { action = ACTION_PREV_MONTH },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.btn_prev_month, prevPi)

                val nextPi = PendingIntent.getBroadcast(context, 21,
                    Intent(context, ProgressWidgetReceiver::class.java)
                        .apply { action = ACTION_NEXT_MONTH },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.btn_next_month, nextPi)

                manager.updateAppWidget(id, views)
            }
        }

        fun scheduleAlarm(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 5000, 5000, alarmPi(context))
        }

        fun cancelAlarm(context: Context) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .cancel(alarmPi(context))
        }

        private fun alarmPi(context: Context): PendingIntent {
            val i = Intent(context, ProgressWidgetReceiver::class.java)
                .apply { action = ACTION_UPDATE }
            return PendingIntent.getBroadcast(context, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAllWidgets(context)
        scheduleAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_PREV_MONTH -> {
                prefs.edit()
                    .putInt(KEY_MONTH_OFFSET, prefs.getInt(KEY_MONTH_OFFSET, 0) - 1)
                    .apply()
                updateAllWidgets(context)
            }
            ACTION_NEXT_MONTH -> {
                val cur = prefs.getInt(KEY_MONTH_OFFSET, 0)
                if (cur < 0) prefs.edit().putInt(KEY_MONTH_OFFSET, cur + 1).apply()
                updateAllWidgets(context)
            }
            ACTION_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context)  { scheduleAlarm(context) }
    override fun onDisabled(context: Context) { cancelAlarm(context) }
    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager,
        id: Int, opts: android.os.Bundle) { updateAllWidgets(context) }
}
