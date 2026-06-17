package com.progresswidget

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(ProgressWidgetReceiver.PREFS_NAME, Context.MODE_PRIVATE)

        val wakeHSeek = findViewById<SeekBar>(R.id.wake_hour_seek)
        val wakeMSeek = findViewById<SeekBar>(R.id.wake_min_seek)
        val sleepHSeek = findViewById<SeekBar>(R.id.sleep_hour_seek)
        val sleepMSeek = findViewById<SeekBar>(R.id.sleep_min_seek)
        val wakeHVal = findViewById<TextView>(R.id.wake_hour_val)
        val wakeMVal = findViewById<TextView>(R.id.wake_min_val)
        val sleepHVal = findViewById<TextView>(R.id.sleep_hour_val)
        val sleepMVal = findViewById<TextView>(R.id.sleep_min_val)
        val saveBtn = findViewById<Button>(R.id.save_button)

        // Load saved values
        wakeHSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_H, 7)
        wakeMSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_M, 0)
        sleepHSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_H, 23)
        sleepMSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_M, 0)

        fun fmt(v: Int) = v.toString().padStart(2, '0')

        wakeHVal.text = fmt(wakeHSeek.progress)
        wakeMVal.text = fmt(wakeMSeek.progress)
        sleepHVal.text = fmt(sleepHSeek.progress)
        sleepMVal.text = fmt(sleepMSeek.progress)

        wakeHSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { wakeHVal.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        wakeMSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { wakeMVal.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        sleepHSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { sleepHVal.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        sleepMSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { sleepMVal.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        saveBtn.setOnClickListener {
            prefs.edit()
                .putInt(ProgressWidgetReceiver.KEY_WAKE_H, wakeHSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_WAKE_M, wakeMSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_SLEEP_H, sleepHSeek.progress)
                .putInt(ProgressWidgetReceiver.KEY_SLEEP_M, sleepMSeek.progress)
                .apply()
            ProgressWidgetReceiver.updateAllWidgets(this)
            saveBtn.text = "SAVED ✓"
            saveBtn.postDelayed({ saveBtn.text = "SAVE & UPDATE WIDGET" }, 2000)
        }
    }
}
