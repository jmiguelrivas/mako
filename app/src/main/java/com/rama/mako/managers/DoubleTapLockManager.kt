package com.rama.mako.managers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.rama.mako.R
import com.rama.mako.receivers.ScreenLockAdminReceiver
import com.rama.mako.services.ScreenLockAccessibilityService

class DoubleTapLockManager(private val context: Context) {

    companion object {
        const val METHOD_DEVICE_ADMIN = "device_admin"
        const val METHOD_ACCESSIBILITY = "accessibility"
        const val REQUEST_ENABLE_SCREEN_LOCK_ADMIN = 2201
    }

    private val dpm by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent by lazy {
        ComponentName(context, ScreenLockAdminReceiver::class.java)
    }
    private val prefs by lazy { PrefsManager.getInstance(context) }

    fun lock(): Boolean {
        return when (prefs.getDoubleTapLockMethod()) {
            METHOD_DEVICE_ADMIN -> deviceAdminLock()
            METHOD_ACCESSIBILITY -> accessibilityLock()
            else -> false
        }
    }

    private fun deviceAdminLock(): Boolean {
        return runCatching {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                true
            } else false
        }.getOrDefault(false)
    }

    @Suppress("InlinedApi")
    private fun accessibilityLock(): Boolean {
        val service = ScreenLockAccessibilityService.instance
        return service?.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        ) ?: false
    }

    fun getMethod(): String = prefs.getDoubleTapLockMethod()

    fun setMethod(method: String) {
        if (method == METHOD_DEVICE_ADMIN || method == METHOD_ACCESSIBILITY) {
            prefs.setDoubleTapLockMethod(method)
        }
    }

    fun isCurrentMethodAvailable(): Boolean {
        return when (prefs.getDoubleTapLockMethod()) {
            METHOD_DEVICE_ADMIN -> dpm.isAdminActive(adminComponent)
            METHOD_ACCESSIBILITY -> isAccessibilityServiceEnabled()
            else -> false
        }
    }

    fun isMethodAvailable(method: String): Boolean {
        return when (method) {
            METHOD_DEVICE_ADMIN -> dpm.isAdminActive(adminComponent)
            METHOD_ACCESSIBILITY -> isAccessibilityServiceEnabled()
            else -> false
        }
    }

    fun isAccessibilitySupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (!isAccessibilitySupported()) return false
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        ).any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    fun requestPermission(activity: Activity, method: String) {
        when (method) {
            METHOD_DEVICE_ADMIN -> requestDeviceAdmin(activity)
            METHOD_ACCESSIBILITY -> requestAccessibility(activity)
        }
    }

    private fun requestDeviceAdmin(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                activity.getString(R.string.double_tap_sleep_admin_explanation)
            )
        }
        activity.startActivityForResult(intent, REQUEST_ENABLE_SCREEN_LOCK_ADMIN)
    }

    private fun requestAccessibility(activity: Activity) {
        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
