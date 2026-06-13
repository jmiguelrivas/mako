package com.rama.mako.activities.settings

import android.content.Intent
import android.provider.Settings
import android.widget.*
import com.rama.mako.R
import com.rama.mako.activities.AboutActivity
import com.rama.mako.activities.MainActivity
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.utils.SettingsUiUtils

class SettingsBasicController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs

    companion object {
        private const val EXPORT_REQUEST = 2001
        private const val IMPORT_REQUEST = 2002
    }

    fun setup() {
        SettingsUiUtils.setupButton(activity, R.id.about_button) {
            activity.startActivity(Intent(activity, AboutActivity::class.java))
        }

        SettingsUiUtils.setupButton(activity, R.id.close_button) {
            activity.finish()
        }

        SettingsUiUtils.setupButton(activity, R.id.activate_button) {
            SettingsUiUtils.openIntent(
                activity,
                Intent(Settings.ACTION_HOME_SETTINGS),
                activity.getString(R.string.toast_unable_open_settings)
            )
        }

        SettingsUiUtils.setupButton(activity, R.id.wallpaper_button) {
            SettingsUiUtils.openIntent(
                activity,
                Intent(Intent.ACTION_SET_WALLPAPER),
                activity.getString(R.string.toast_unable_open_wallpaper_app)
            )
        }

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.reset_button)) {
            activity.startActivity(
                Intent(activity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.change_apps_button)) {
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
        }

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.export_button)) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "prefs_backup.json")
            }
            activity.startActivityForResult(intent, EXPORT_REQUEST)
        }

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.import_button)) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            activity.startActivityForResult(intent, IMPORT_REQUEST)
        }

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.clear_prefs_button)) {
            prefs.clearAllPrefs()
                .onSuccess {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_reset_done),
                        Toast.LENGTH_SHORT
                    ).show()
                    activity.finish()
                    activity.startActivity(
                        Intent(activity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                    )
                }
                .onFailure {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_reset_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != android.app.Activity.RESULT_OK || data?.data == null) return

        when (requestCode) {
            EXPORT_REQUEST -> {
                prefs.exportToUri(activity, data.data!!)
            }
            IMPORT_REQUEST -> {
                val success = prefs.importFromUri(activity, data.data!!)
                if (success) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_import_done),
                        Toast.LENGTH_SHORT
                    ).show()
                    activity.startActivity(
                        Intent(activity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                    )
                } else {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_import_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}