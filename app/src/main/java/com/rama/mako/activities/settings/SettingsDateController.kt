package com.rama.mako.activities.settings

import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.PrefsManager
import com.rama.bohio.managers.ThemeManager
import com.rama.bohio.util.UiActions

class SettingsDateController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs
    private val appsProvider get() = activity.appsProvider
    private val iconManager get() = activity.iconManager

    fun setup() {
        setupDateFormat()
        setupDateAppButton()
    }

    private fun setupDateFormat() {
        val group = activity.findViewById<RadioGroup>(R.id.date_format_group)
        val yearDayCheckbox = activity.findViewById<View>(R.id.show_year_day)
        val yearWeekCheckbox = activity.findViewById<View>(R.id.show_year_week)

        fun updateYearDayVisibility(format: String) {
            if (format != PrefsManager.DateFormat.NONE) {
                yearWeekCheckbox.visibility = View.VISIBLE
                yearDayCheckbox.visibility = View.VISIBLE
            } else {
                yearWeekCheckbox.visibility = View.GONE
                yearDayCheckbox.visibility = View.GONE
            }
        }

        when (prefs.getDateFormat()) {
            PrefsManager.DateFormat.NONE -> group.check(R.id.date_none)
            PrefsManager.DateFormat.YMD -> group.check(R.id.radio_YMD)
            PrefsManager.DateFormat.MDY -> group.check(R.id.radio_MDY)
            PrefsManager.DateFormat.DMY -> group.check(R.id.radio_DMY)
            else -> group.check(R.id.date_system)
        }
        updateYearDayVisibility(prefs.getDateFormat())

        group.setOnCheckedChangeListener { _, id ->
            val format = when (id) {
                R.id.date_none -> PrefsManager.DateFormat.NONE
                R.id.date_system -> PrefsManager.DateFormat.DEFAULT
                R.id.radio_YMD -> PrefsManager.DateFormat.YMD
                R.id.radio_MDY -> PrefsManager.DateFormat.MDY
                R.id.radio_DMY -> PrefsManager.DateFormat.DMY
                else -> return@setOnCheckedChangeListener
            }
            prefs.setDateFormat(format)
            updateYearDayVisibility(format)
        }
    }

    private fun setupDateAppButton() {
        UiActions.setClickWithHaptics(activity.findViewById(R.id.set_date_app_button)) {
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
            prefs.setDateApp(selectedApp.packageName)
            Toast.makeText(
                activity,
                activity.getString(R.string.toast_app_selected, selectedApp.label),
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        UiActions.setClickWithHaptics(closeBtn) { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
