package com.rama.mako.managers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserManager

class AppsProvider(private val context: Context) {

    data class AppEntry(
        val packageName: String,
        val label: String,
        val userHandle: UserHandle,
        val activityInfo: LauncherActivityInfo,
        val profileInitial: String?
    ) {
        val isWorkProfile: Boolean = userHandle.hashCode() != 0
        val displayLabel: String = label
    }

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = mutableMapOf<String, Drawable>()
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getAll(): List<AppEntry> {
        return userManager.userProfiles.flatMap { userHandle ->
            val profileInitial = getProfileInitial(userHandle)
            launcherApps.getActivityList(null, userHandle).map { info ->
                AppEntry(
                    packageName = info.applicationInfo.packageName,
                    label = info.label.toString(),
                    userHandle = userHandle,
                    activityInfo = info,
                    profileInitial = profileInitial
                )
            }
        }
    }

    fun launch(app: AppEntry): Boolean {
        return try {
            launcherApps.startMainActivity(
                app.activityInfo.componentName,
                app.userHandle,
                null,
                null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getIcon(app: AppEntry): Drawable {
        val key = "${app.packageName}:${app.userHandle.hashCode()}"
        return iconCache.getOrPut(key) {
            app.activityInfo.getIcon(context.resources.displayMetrics.densityDpi)
        }
    }

    private fun getProfileInitial(userHandle: UserHandle): String? {
        if (userHandle.hashCode() == 0) return null
        return context.packageManager.getUserBadgedLabel("", userHandle).toString()
            .trim()
            .firstOrNull() { it.isLetter() }
            ?.uppercase()
            ?: "E"
    }
}