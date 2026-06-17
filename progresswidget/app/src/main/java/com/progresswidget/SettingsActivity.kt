package com.progresswidget

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val KEY_WEEK_START = "week_start_day"  // 0=Sun, 1=Mon
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(ProgressWidgetReceiver.PREFS_NAME, Context.MODE_PRIVATE)

        val wakeHSeek  = findViewById<SeekBar>(R.id.wake_hour_seek)
        val wakeMSeek  = findViewById<SeekBar>(R.id.wake_min_seek)
        val sleepHSeek = findViewById<SeekBar>(R.id.sleep_hour_seek)
        val sleepMSeek = findViewById<SeekBar>(R.id.sleep_min_seek)
        val wakeHVal   = findViewById<TextView>(R.id.wake_hour_val)
        val wakeMVal   = findViewById<TextView>(R.id.wake_min_val)
        val sleepHVal  = findViewById<TextView>(R.id.sleep_hour_val)
        val sleepMVal  = findViewById<TextView>(R.id.sleep_min_val)
        val btnSun     = findViewById<Button>(R.id.btn_week_sun)
        val btnMon     = findViewById<Button>(R.id.btn_week_mon)
        val saveBtn    = findViewById<Button>(R.id.save_button)

        // Load saved values
        wakeHSeek.progress  = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_H, 7)
        wakeMSeek.progress  = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_M, 0)
        sleepHSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_H, 23)
        sleepMSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_M, 0)
        var weekStart       = prefs.getInt(KEY_WEEK_START, 1) // default Monday

        fun fmt(v: Int) = v.toString().padStart(2, '0')
        wakeHVal.text  = fmt(wakeHSeek.progress)
        wakeMVal.text  = fmt(wakeMSeek.progress)
        sleepHVal.text = fmt(sleepHSeek.progress)
        sleepMVal.text = fmt(sleepMSeek.progress)

        fun updateWeekButtons() {
            btnSun.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (weekStart == 0) 0xFFE24B4A.toInt() else 0xFF333333.toInt())
            btnMon.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (weekStart == 1) 0xFFE24B4A.toInt() else 0xFF333333.toInt())
        }
        updateWeekButtons()

        btnSun.setOnClickListener { weekStart = 0; updateWeekButtons() }
        btnMon.setOnClickListener { weekStart = 1; updateWeekButtons() }

        fun listener(tv: TextView) = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { tv.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        wakeHSeek.setOnSeekBarChangeListener(listener(wakeHVal))
        wakeMSeek.setOnSeekBarChangeListener(listener(wakeMVal))
        sleepHSeek.setOnSeekBarChangeListener(listener(sleepHVal))
        sleepMSeek.setOnSeekBarChangeListener(listener(sleepMVal))

        saveBtn.setOnClickListener {
            prefs.edit()
                .putInt(ProgressWidgetReceiver.KEY_WAKE_H,  wakeHSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_WAKE_M,  wakeMSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_SLEEP_H, sleepHSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_SLEEP_M, sleepMSeek.progress)
                .putInt(KEY_WEEK_START, weekStart)
                .apply()
            ProgressWidgetReceiver.updateAllWidgets(this)
            saveBtn.text = "SAVED ✓"
            saveBtn.postDelayed({ saveBtn.text = "SAVE & UPDATE WIDGET" }, 2000)
        }
    }
}
