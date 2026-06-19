package com.progresswidget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val KEY_WEEK_START  = "week_start_day"
        const val KEY_GLOBE_PKG   = "shortcut_globe_pkg"
        const val KEY_WEEK_PKG    = "shortcut_week_pkg"
        const val KEY_CAL_PKG     = "shortcut_cal_pkg"
        const val KEY_YEAR_PKG    = "shortcut_year_pkg"

        // Request codes for app picker
        const val REQ_GLOBE = 101
        const val REQ_WEEK  = 102
        const val REQ_CAL   = 103
        const val REQ_YEAR  = 104
    }

    private var pendingPickKey = ""

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

        val globeNameTv = findViewById<TextView>(R.id.globe_app_name)
        val weekNameTv  = findViewById<TextView>(R.id.week_app_name)
        val calNameTv   = findViewById<TextView>(R.id.cal_app_name)
        val yearNameTv  = findViewById<TextView>(R.id.year_app_name)

        val globePickBtn = findViewById<Button>(R.id.globe_app_pick)
        val weekPickBtn  = findViewById<Button>(R.id.week_app_pick)
        val calPickBtn   = findViewById<Button>(R.id.cal_app_pick)
        val yearPickBtn  = findViewById<Button>(R.id.year_app_pick)

        // Load saved values
        wakeHSeek.progress  = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_H, 7)
        wakeMSeek.progress  = prefs.getInt(ProgressWidgetReceiver.KEY_WAKE_M, 0)
        sleepHSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_H, 23)
        sleepMSeek.progress = prefs.getInt(ProgressWidgetReceiver.KEY_SLEEP_M, 0)
        var weekStart = prefs.getInt(KEY_WEEK_START, 1)

        fun fmt(v: Int) = v.toString().padStart(2, '0')
        wakeHVal.text  = fmt(wakeHSeek.progress)
        wakeMVal.text  = fmt(wakeMSeek.progress)
        sleepHVal.text = fmt(sleepHSeek.progress)
        sleepMVal.text = fmt(sleepMSeek.progress)

        // Show saved app names
        fun appLabel(pkg: String): String {
            if (pkg.isEmpty()) return "Not set"
            return try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) { pkg }
        }
        globeNameTv.text = appLabel(prefs.getString(KEY_GLOBE_PKG, "") ?: "")
        weekNameTv.text  = appLabel(prefs.getString(KEY_WEEK_PKG,  "") ?: "")
        calNameTv.text   = appLabel(prefs.getString(KEY_CAL_PKG,   "") ?: "")
        yearNameTv.text  = appLabel(prefs.getString(KEY_YEAR_PKG,  "") ?: "")

        // Week start buttons
        fun updateWeekBtns() {
            btnSun.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (weekStart == 0) 0xFFE24B4A.toInt() else 0xFF333333.toInt())
            btnMon.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (weekStart == 1) 0xFFE24B4A.toInt() else 0xFF333333.toInt())
        }
        updateWeekBtns()
        btnSun.setOnClickListener { weekStart = 0; updateWeekBtns() }
        btnMon.setOnClickListener { weekStart = 1; updateWeekBtns() }

        // Seekbar listeners
        fun listener(tv: TextView) = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, v: Int, u: Boolean) { tv.text = fmt(v) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        wakeHSeek.setOnSeekBarChangeListener(listener(wakeHVal))
        wakeMSeek.setOnSeekBarChangeListener(listener(wakeMVal))
        sleepHSeek.setOnSeekBarChangeListener(listener(sleepHVal))
        sleepMSeek.setOnSeekBarChangeListener(listener(sleepMVal))

        // App picker buttons — launch system app chooser
        fun launchPicker(reqCode: Int, key: String) {
            pendingPickKey = key
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val chooser = Intent.createChooser(intent, "Choose app for this card")
            // Use the app picker pattern
            val pickIntent = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
                putExtra(Intent.EXTRA_INTENT, Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                })
                putExtra(Intent.EXTRA_TITLE, "Choose app")
            }
            try {
                startActivityForResult(pickIntent, reqCode)
            } catch (e: Exception) {
                // Fallback: open all apps
                startActivityForResult(
                    Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                    reqCode)
            }
        }

        globePickBtn.setOnClickListener { launchPicker(REQ_GLOBE, KEY_GLOBE_PKG) }
        weekPickBtn.setOnClickListener  { launchPicker(REQ_WEEK,  KEY_WEEK_PKG)  }
        calPickBtn.setOnClickListener   { launchPicker(REQ_CAL,   KEY_CAL_PKG)   }
        yearPickBtn.setOnClickListener  { launchPicker(REQ_YEAR,  KEY_YEAR_PKG)  }

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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        val pkg = data.component?.packageName
            ?: data.resolveActivity(packageManager)?.packageName
            ?: return

        val key = when (requestCode) {
            REQ_GLOBE -> KEY_GLOBE_PKG
            REQ_WEEK  -> KEY_WEEK_PKG
            REQ_CAL   -> KEY_CAL_PKG
            REQ_YEAR  -> KEY_YEAR_PKG
            else      -> return
        }

        getSharedPreferences(ProgressWidgetReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, pkg).apply()

        // Refresh name labels
        val label = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }

        when (requestCode) {
            REQ_GLOBE -> findViewById<TextView>(R.id.globe_app_name).text = label
            REQ_WEEK  -> findViewById<TextView>(R.id.week_app_name).text  = label
            REQ_CAL   -> findViewById<TextView>(R.id.cal_app_name).text   = label
            REQ_YEAR  -> findViewById<TextView>(R.id.year_app_name).text  = label
        }
    }
}
