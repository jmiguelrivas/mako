package com.rama.mako.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.rama.mako.CsActivity
import com.rama.mako.R
import com.rama.mako.activities.settings.SettingsAppearanceController
import com.rama.mako.activities.settings.SettingsBasicController
import com.rama.mako.activities.settings.SettingsCheckboxController
import com.rama.mako.activities.settings.SettingsClockController
import com.rama.mako.activities.settings.SettingsDateController
import com.rama.mako.activities.settings.SettingsPinController
import com.rama.mako.activities.settings.SettingsGroupsController
import com.rama.mako.activities.settings.SettingsIconsController
import com.rama.mako.activities.settings.SettingsLanguageController
import com.rama.mako.activities.settings.SettingsExtController
import com.rama.mako.managers.AppsProvider
import com.rama.mako.managers.GroupsManager
import com.rama.mako.managers.HomeBackgroundManager
import com.rama.mako.managers.IconManager
import com.rama.mako.managers.PrefsManager

class SettingsActivity : CsActivity() {

    lateinit var appsProvider: AppsProvider
    lateinit var iconManager: IconManager
    lateinit var groupsManager: GroupsManager

    private lateinit var clockController: SettingsClockController
    private lateinit var dateController: SettingsDateController
    private lateinit var checkboxController: SettingsCheckboxController
    private lateinit var appearanceController: SettingsAppearanceController
    internal lateinit var homeBackgroundManager: HomeBackgroundManager
    internal lateinit var settingsRootView: View

    private var isUnlocked = false
    private var isLockScreenShowing = false
    private val LOCK_REQUEST = 1001
    val FONT_PICK_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_settings)

        settingsRootView = findViewById(R.id.settings_root)
        applyEdgeToEdgePadding(settingsRootView)
        applyCurrentTheme(settingsRootView)

        homeBackgroundManager = HomeBackgroundManager(this)
        applySettingsBackground()

        appsProvider = AppsProvider(this)
        iconManager = IconManager(this, appsProvider)
        groupsManager = GroupsManager(this, appsProvider)

        clockController = SettingsClockController(this).also { it.setup() }
        dateController = SettingsDateController(this).also { it.setup() }

        SettingsBasicController(this).setup()
        appearanceController = SettingsAppearanceController(this).also { it.setup() }
        SettingsLanguageController(this).setup()
        SettingsIconsController(this).setup()
        checkboxController = SettingsCheckboxController(this).also { it.setup() }
        SettingsGroupsController(this).setup()
        SettingsPinController(this).setup()

        // ext flavor
        SettingsExtController(this).setup()
    }

    override fun onResume() {
        super.onResume()
        applySettingsBackground()
        checkboxController.refresh()

        // Prevent re-lock if already unlocked or lock screen is active
        if (isUnlocked || isLockScreenShowing) return

        val lockEnabled = prefs.getBoolean(
            PrefsManager.PrefKeys.SECURITY_KEYPAD_VISIBLE,
            false
        )
        val hasPin = prefs.getPin().isNotEmpty()

        if (lockEnabled && hasPin) {
            isLockScreenShowing = true
            startActivityForResult(
                Intent(this, LockActivity::class.java),
                LOCK_REQUEST
            )
        }
    }
    
    fun applySettingsBackground(force: Boolean = false) {
        applyWindowBackground()
        homeBackgroundManager.applyToSettings(settingsRootView, PrefsManager.BackgroundMode.DEFAULT)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCK_REQUEST) {
            isLockScreenShowing = false

            if (resultCode == RESULT_OK) {
                isUnlocked = true
            }
        }

        clockController.onActivityResult(requestCode, resultCode, data)
        checkboxController.onActivityResult(requestCode, resultCode)
        appearanceController.onActivityResult(requestCode, resultCode, data)
    }
}
