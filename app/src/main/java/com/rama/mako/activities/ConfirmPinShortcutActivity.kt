package com.rama.mako.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.rama.mako.R

class ConfirmPinShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            intent.action != LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT
        ) {
            finish()
            return
        }

        val launcherApps =
            getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)
        val shortcut = request
            ?.takeIf {
                it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT &&
                    it.isValid
            }
            ?.shortcutInfo

        if (request == null || shortcut == null) {
            finish()
            return
        }

        val label = shortcut.shortLabel?.toString()
            ?.ifBlank { null }
            ?: shortcut.longLabel?.toString()
            ?.ifBlank { null }
            ?: shortcut.`package`

        AlertDialog.Builder(this)
            .setTitle(R.string.h2_add_shortcut)
            .setMessage(label)
            .setPositiveButton(R.string.btn_add_shortcut) { _, _ ->
                val accepted = runCatching {
                    request.isValid && request.accept()
                }.getOrDefault(false)
                val message = if (accepted) {
                    getString(R.string.toast_shortcut_added, label)
                } else {
                    getString(R.string.toast_unable_add_shortcut)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
