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
            if (pin.length >= 1) {
                prefs.setPin(pin)
                android.widget.Toast.makeText(
                    activity,
                    activity.getString(R.string.toast_pin_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    activity,
                    activity.getString(R.string.toast_pin_too_short),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}