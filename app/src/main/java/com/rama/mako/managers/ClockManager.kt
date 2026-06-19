package com.rama.mako.managers

import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import com.rama.bohio.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Calendar

class ClockManager(
    private val timeTextView: TextView,
    private val dateTextView: TextView,
    context: android.content.Context
) {
    private val prefs = PrefsManager.getInstance(context)
    private val handler = Handler(Looper.getMainLooper())
    private val calendar = Calendar.getInstance()

    private val runnable = object : Runnable {
        override fun run() {
            val clockFormatPref = prefs.getClockFormat()

            calendar.timeInMillis = System.currentTimeMillis()
            val locale = LocaleHelper.getCurrentLocale(dateTextView.resources.configuration)

            // --- Clock ---
            if (clockFormatPref != PrefsManager.ClockFormat.NONE) {
                timeTextView.visibility = View.VISIBLE

                val use24h = when (clockFormatPref) {
                    PrefsManager.ClockFormat.HOUR_24 -> true
                    PrefsManager.ClockFormat.HOUR_12 -> false
                    else -> DateFormat.is24HourFormat(timeTextView.context)
                }

                val pattern = if (use24h) "HH:mm" else "hh:mm a"
                val formatter = SimpleDateFormat(pattern, locale)

                timeTextView.text = formatter.format(calendar.time)
            } else {
                timeTextView.visibility = View.GONE
            }

            // --- Date ---
            if (prefs.isDateVisible()) {
                dateTextView.visibility = View.VISIBLE

                val dateFormat = DateFormat.getDateFormat(dateTextView.context)
                val weekday = calendar.getDisplayName(
                    Calendar.DAY_OF_WEEK,
                    Calendar.LONG,
                    locale
                ).orEmpty()

                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                val yearDay = if (prefs.isYearDayVisible()) "$dayOfYear/$totalDays" else null

                val parts = listOfNotNull(weekday, dateFormat.format(calendar.time), yearDay)
                dateTextView.text = parts.joinToString(" :: ").uppercase(locale)
            } else {
                dateTextView.visibility = View.GONE
            }

            handler.postDelayed(this, 1000)
        }
    }

    fun start() = handler.post(runnable)
    fun stop() = handler.removeCallbacks(runnable)
}