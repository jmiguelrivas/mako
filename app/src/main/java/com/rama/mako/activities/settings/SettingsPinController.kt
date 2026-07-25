package com.rama.mako.activities.settings

import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.PrefsManager.FileKeys
import com.rama.bohio.widgets.WdCheckbox
import com.rama.mako.widgets.WdPinField

class SettingsPinController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs

    fun setup() {
        setupRandomizedKeypadToggle()
        setupPinField()
    }

    private fun setupRandomizedKeypadToggle() {
        val checkbox = activity.findViewById<WdCheckbox>(R.id.randomized_keypad)
        val isRandomized = prefs.getBoolean(FileKeys.SECURITY_KEYPAD_RANDOMIZED, true)
        checkbox.setChecked(isRandomized)

        checkbox.setOnCheckedChangeListener { checked ->
            prefs.setBoolean(FileKeys.SECURITY_KEYPAD_RANDOMIZED, checked)
        }
    }

    private fun setupPinField() {
        val pinField = activity.findViewById<WdPinField>(R.id.pin_field_widget)

        pinField.onPinSaved = { pin ->
            if (pin.isNotEmpty()) {
                android.app.AlertDialog.Builder(activity)
                    .setTitle(R.string.dialog_save_pin_title)
                    .setMessage(R.string.dialog_save_pin_warning)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        prefs.setPin(pin)
                        android.widget.Toast.makeText(
                            activity,
                            activity.getString(R.string.toast_pin_saved),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            } else {
                prefs.clearPin()
                activity.findViewById<WdCheckbox>(R.id.lock_settings).setChecked(false)
                android.widget.Toast.makeText(
                    activity,
                    activity.getString(R.string.toast_pin_removed),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}