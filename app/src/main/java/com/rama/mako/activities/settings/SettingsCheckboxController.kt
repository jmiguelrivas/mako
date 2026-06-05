package com.rama.mako.activities.settings

import android.app.AlertDialog
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.DoubleTapLockManager
import com.rama.mako.managers.PrefsManager.PrefKeys
import com.rama.mako.widgets.WdCheckbox

class SettingsCheckboxController(private val activity: SettingsActivity) {
    
    private val prefs get() = activity.prefs
    private val lockManager = DoubleTapLockManager(activity)

    private lateinit var doubleTapSleepCheckbox: WdCheckbox
    private lateinit var lockMethodContainer: View
    private lateinit var lockMethodAccessibility: RadioButton
    private var isSyncingDoubleTapSleepCheckbox = false
    private var isSyncingLockMethodGroup = false

    fun setup() {
        bindWdCheckbox(R.id.show_date, PrefKeys.DATE_VISIBLE, false, listOf(R.id.show_year_day))
        bindWdCheckbox(
            R.id.show_search,
            PrefKeys.APPS_SEARCH,
            false,
            listOf(R.id.always_show_search)
        )
        bindWdCheckbox(R.id.always_show_search, PrefKeys.APPS_SEARCH_ALWAYS_VISIBLE, false)
        bindWdCheckbox(
            R.id.show_group_header,
            PrefKeys.GROUPS_HEADERS,
            false,
            listOf(R.id.has_collapsible_groups)
        )
        bindWdCheckbox(R.id.has_collapsible_groups, PrefKeys.GROUPS_COLLAPSIBLE, false)
        bindWdCheckbox(R.id.show_year_day, PrefKeys.DATE_YEAR_DAY, false)
        bindWdCheckbox(
            R.id.show_battery,
            PrefKeys.BATTERY_VISIBLE,
            false,
            listOf(R.id.show_battery_temperature, R.id.show_battery_charge_status)
        )
        bindWdCheckbox(R.id.show_battery_temperature, PrefKeys.BATTERY_TEMPERATURE, false)
        bindWdCheckbox(R.id.show_battery_charge_status, PrefKeys.BATTERY_CHARGE_STATUS, false)
        bindWdCheckbox(R.id.show_system_bar, PrefKeys.SYSTEM_BAR_VISIBLE, false)
        bindWdCheckbox(
            R.id.prevent_home_screen_rotation,
            PrefKeys.SYSTEM_PREVENT_ROTATION,
            false,
            onChange = { isChecked ->
                activity.applyRotationLock(isChecked)
            }
        )
        bindWdCheckbox(R.id.show_profile_indicator, PrefKeys.APPS_PROFILE_INDICATOR, true)
        setupDoubleTapToSleepCheckbox()
        setupLockMethodRadioGroup()

        bindWdCheckbox(
            R.id.lock_settings,
            PrefKeys.SECURITY_KEYPAD_VISIBLE,
            false,
            listOf(R.id.randomized_keypad, R.id.pin_field)
        )
        bindWdCheckbox(
            R.id.randomized_keypad,
            PrefKeys.SECURITY_KEYPAD_RANDOMIZED,
            true,
        )
        bindWdCheckbox(
            R.id.icons_open_settings,
            PrefKeys.APPS_ICONS_OPEN_SETTINGS,
            true,
        )
    }

    fun refresh() {
        val method = prefs.getDoubleTapLockMethod()
        val available = lockManager.isCurrentMethodAvailable()
        val shouldBeOn = prefs.isDoubleTapToSleepEnabled() && available

        if (prefs.isDoubleTapToSleepEnabled() && !available) {
            prefs.setDoubleTapToSleepEnabled(false)
        }

        syncDoubleTapSleepCheckbox(shouldBeOn)
        refreshLockMethodUI()
        updateCheckedRadioButton(method)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode != DoubleTapLockManager.REQUEST_ENABLE_SCREEN_LOCK_ADMIN) return

        val granted = lockManager.isMethodAvailable(DoubleTapLockManager.METHOD_DEVICE_ADMIN)
        prefs.setDoubleTapToSleepEnabled(granted)
        syncDoubleTapSleepCheckbox(granted)

        if (!granted) {
            Toast.makeText(
                activity,
                activity.getString(R.string.double_tap_sleep_admin_declined_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupDoubleTapToSleepCheckbox() {
        doubleTapSleepCheckbox = activity.findViewById(R.id.double_tap_sleep)

        val available = lockManager.isCurrentMethodAvailable()
        val shouldBeOn = prefs.isDoubleTapToSleepEnabled() && available

        if (prefs.isDoubleTapToSleepEnabled() && !available) {
            prefs.setDoubleTapToSleepEnabled(false)
        }

        syncDoubleTapSleepCheckbox(shouldBeOn)

        doubleTapSleepCheckbox.setOnCheckedChangeListener { checked ->
            if (isSyncingDoubleTapSleepCheckbox) return@setOnCheckedChangeListener
            if (checked) {
                enableDoubleTapToSleep()
            } else {
                prefs.setDoubleTapToSleepEnabled(false)
            }
            refreshLockMethodUI()
        }
    }

    private fun setupLockMethodRadioGroup() {
        lockMethodContainer = activity.findViewById(R.id.double_tap_lock_method_container)
        lockMethodAccessibility = activity.findViewById(R.id.lock_method_accessibility)

        if (!lockManager.isAccessibilitySupported()) {
            lockMethodAccessibility.isEnabled = false
            lockMethodAccessibility.text =
                activity.getString(R.string.double_tap_lock_method_accessibility) +
                        " (" + activity.getString(R.string.double_tap_lock_method_accessibility_unavailable) + ")"
        }

        updateCheckedRadioButton(prefs.getDoubleTapLockMethod())
        refreshLockMethodUI()

        val adminRadio = activity.findViewById<RadioButton>(R.id.lock_method_device_admin)
        val accessibilityRadio = activity.findViewById<RadioButton>(R.id.lock_method_accessibility)

        fun onMethodSelected(method: String) {
            if (isSyncingLockMethodGroup) return
            if (prefs.getDoubleTapLockMethod() == method) return
            lockManager.setMethod(method)
            updateCheckedRadioButton(method)
            if (prefs.isDoubleTapToSleepEnabled() && !lockManager.isCurrentMethodAvailable()) {
                lockManager.requestPermission(activity, method)
            }
        }

        adminRadio.setOnClickListener { onMethodSelected(DoubleTapLockManager.METHOD_DEVICE_ADMIN) }
        accessibilityRadio.setOnClickListener { onMethodSelected(DoubleTapLockManager.METHOD_ACCESSIBILITY) }

        activity.findViewById<View>(R.id.lock_method_admin_info)
            .setOnClickListener { showMethodInfo(DoubleTapLockManager.METHOD_DEVICE_ADMIN) }
        activity.findViewById<View>(R.id.lock_method_accessibility_info)
            .setOnClickListener { showMethodInfo(DoubleTapLockManager.METHOD_ACCESSIBILITY) }
    }

    private fun showMethodInfo(method: String) {
        val title: String
        val message: String
        when (method) {
            DoubleTapLockManager.METHOD_DEVICE_ADMIN -> {
                title = activity.getString(R.string.double_tap_lock_method_device_admin)
                message = activity.getString(R.string.double_tap_lock_method_admin_info)
            }

            else -> {
                title = activity.getString(R.string.double_tap_lock_method_accessibility)
                message = activity.getString(R.string.double_tap_lock_method_accessibility_info)
            }
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun enableDoubleTapToSleep() {
        prefs.setDoubleTapToSleepEnabled(true)
    }

    private fun refreshLockMethodUI() {
        val isOn = prefs.isDoubleTapToSleepEnabled()
        lockMethodContainer.visibility = if (isOn) View.VISIBLE else View.GONE
    }

    private fun updateCheckedRadioButton(method: String) {
        isSyncingLockMethodGroup = true
        activity.findViewById<RadioButton>(R.id.lock_method_device_admin).isChecked =
            method == DoubleTapLockManager.METHOD_DEVICE_ADMIN
        activity.findViewById<RadioButton>(R.id.lock_method_accessibility).isChecked =
            method == DoubleTapLockManager.METHOD_ACCESSIBILITY
        isSyncingLockMethodGroup = false
    }

    private fun syncDoubleTapSleepCheckbox(isChecked: Boolean) {
        isSyncingDoubleTapSleepCheckbox = true
        doubleTapSleepCheckbox.setChecked(isChecked)
        isSyncingDoubleTapSleepCheckbox = false
    }

    private fun bindWdCheckbox(
        wdCheckboxId: Int,
        key: String,
        defaultValue: Boolean,
        dependentViewIds: List<Int>? = null,
        onChange: ((Boolean) -> Unit)? = null
    ) {
        val checkbox = activity.findViewById<WdCheckbox>(wdCheckboxId)
        val dependents = dependentViewIds?.map { activity.findViewById<View>(it) }

        val isChecked = prefs.getBoolean(key, defaultValue)
        checkbox.setChecked(isChecked)

        dependents?.forEach {
            it.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        checkbox.setOnCheckedChangeListener { checked ->
            prefs.setBoolean(key, checked)
            dependents?.forEach {
                it.visibility = if (checked) View.VISIBLE else View.GONE
            }
            onChange?.invoke(checked)
        }
    }
}
