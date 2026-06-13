package com.rama.mako.activities.settings

import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.PrefsManager
import com.rama.mako.managers.ThemeManager
import com.rama.mako.utils.SettingsUiUtils

class SettingsClockController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs
    private val appsProvider get() = activity.appsProvider
    private val iconManager get() = activity.iconManager

    fun setup() {
        setupClockFormat()
        setupClockAppButton()
    }

    private fun setupClockFormat() {
        val group = activity.findViewById<RadioGroup>(R.id.clock_format_group)

        when {
            prefs.getClockFormat() == PrefsManager.ClockFormat.NONE -> group.check(R.id.clock_none)
            prefs.getClockFormat() == PrefsManager.ClockFormat.HOUR_24 -> group.check(R.id.clock_24)
            prefs.getClockFormat() == PrefsManager.ClockFormat.HOUR_12 -> group.check(R.id.clock_12)
            else -> group.check(R.id.clock_system)
        }

        group.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.clock_none -> prefs.setClockFormat(PrefsManager.ClockFormat.NONE)
                R.id.clock_system -> prefs.setClockFormat(PrefsManager.ClockFormat.DEFAULT)
                R.id.clock_24 -> prefs.setClockFormat(PrefsManager.ClockFormat.HOUR_24)
                R.id.clock_12 -> prefs.setClockFormat(PrefsManager.ClockFormat.HOUR_12)
            }
        }
    }

    private fun setupClockAppButton() {
        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.set_clock_app_button)) {
            showAppPickerDialog()
        }
    }

    private fun showAppPickerDialog() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_pick_clock_app, null)
        ThemeManager.applyTheme(activity, dialogView)

        val dialog = android.app.Dialog(activity).apply {
            setContentView(dialogView)
            setCancelable(true)
        }

        val listView = dialogView.findViewById<ListView>(R.id.app_list)
        val closeBtn = dialogView.findViewById<Button>(R.id.close_button)
        val apps = appsProvider.getAll().sortedBy { it.label.lowercase() }

        val adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(position: Int) = apps[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: activity.layoutInflater.inflate(
                    R.layout.list_item_app, parent, false
                )
                val app = apps[position]
                view.findViewById<TextView>(R.id.open_app_button).text = app.label
                view.findViewById<ImageView>(R.id.app_icon)
                    .setImageDrawable(iconManager.getIcon(app))
                ThemeManager.applyTheme(parent.context, view)
                return view
            }
        }

        listView.adapter = adapter

        listView.setOnItemClickListener { _, itemView, position, _ ->
            itemView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            val selectedApp = apps[position]
            prefs.setClockApp(selectedApp.packageName)
            Toast.makeText(
                activity,
                activity.getString(R.string.toast_clock_app_selected, selectedApp.label),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        SettingsUiUtils.setClickWithHaptics(closeBtn) { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}