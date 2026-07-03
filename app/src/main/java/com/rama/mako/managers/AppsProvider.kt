package com.rama.mako.managers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserManager

class AppsProvider(private val context: Context) {

    data class AppEntry(
        val packageName: String,
        val label: String,
        val userHandle: UserHandle,
        val activityInfo: LauncherActivityInfo?,
        val shortcutInfo: ShortcutInfo?,
        val profileInitial: String?
    ) {
        val isWorkProfile: Boolean = userHandle.hashCode() != 0
        val displayLabel: String = label

        val isPwaShortcut: Boolean = shortcutInfo != null
    }

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = mutableMapOf<String, Drawable>()
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getAll(): List<AppEntry> {
        val realApps = userManager.userProfiles.flatMap { userHandle ->
            val profileInitial = getProfileInitial(userHandle)

            // A locked Private Space (or a quiet-mode work profile) has its user
            // stopped; querying it can throw instead of just returning empty.
            val activities = try {
                launcherApps.getActivityList(null, userHandle)
            } catch (e: SecurityException) {
                emptyList()
            } catch (e: IllegalStateException) {
                emptyList()
            }

            activities.map { info ->
                AppEntry(
                    packageName = info.applicationInfo.packageName,
                    label = info.label.toString(),
                    userHandle = userHandle,
                    activityInfo = info,
                    shortcutInfo = null,
                    profileInitial = profileInitial
                )
            }
        }

        val realPackageNames = realApps.map { it.packageName }.toSet()
        val pwaShortcuts = getPinnedPwaShortcuts(realPackageNames)

        return realApps + pwaShortcuts
    }

    private fun getPinnedPwaShortcuts(excludePackages: Set<String>): List<AppEntry> {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()

        return userManager.userProfiles.flatMap { userHandle ->
            val profileInitial = getProfileInitial(userHandle)
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)

            val shortcuts = try {
                launcherApps.getShortcuts(query, userHandle) ?: emptyList()
            } catch (e: SecurityException) {
                emptyList()
            } catch (e: IllegalStateException) {
                // Thrown if the user isn't unlocked yet (e.g. before first unlock).
                emptyList()
            }

            shortcuts
                .filter { it.`package` !in excludePackages }
                .map { shortcut ->
                    AppEntry(
                        packageName = shortcut.`package`,
                        label = (shortcut.shortLabel ?: shortcut.longLabel
                        ?: shortcut.id).toString(),
                        userHandle = userHandle,
                        activityInfo = null,
                        shortcutInfo = shortcut,
                        profileInitial = profileInitial
                    )
                }
        }
    }

    fun launch(app: AppEntry): Boolean {
        return try {
            val shortcut = app.shortcutInfo
            if (shortcut != null) {
                launcherApps.startShortcut(shortcut, null, null)
            } else {
                launcherApps.startMainActivity(
                    app.activityInfo!!.componentName,
                    app.userHandle,
                    null,
                    null
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getIcon(app: AppEntry): Drawable {
        val key = "${app.packageName}:${app.userHandle.hashCode()}:${app.shortcutInfo?.id ?: ""}"
        return iconCache.getOrPut(key) {
            val shortcut = app.shortcutInfo
            if (shortcut != null) {
                launcherApps.getShortcutIconDrawable(
                    shortcut,
                    context.resources.displayMetrics.densityDpi
                ) ?: context.packageManager.defaultActivityIcon
            } else {
                app.activityInfo!!.getIcon(context.resources.displayMetrics.densityDpi)
            }
        }
    }

    fun registerCallback(callback: LauncherApps.Callback) {
        launcherApps.registerCallback(callback)
    }

    fun unregisterCallback(callback: LauncherApps.Callback) {
        launcherApps.unregisterCallback(callback)
    }

    fun getPrivateProfileHandle(): UserHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null

        return userManager.userProfiles.firstOrNull { handle ->
            runCatching {
                launcherApps.getLauncherUserInfo(handle)?.userType ==
                        UserManager.USER_TYPE_PROFILE_PRIVATE
            }.getOrDefault(false)
        }
    }

    fun hasPrivateSpace(): Boolean = getPrivateProfileHandle() != null

    fun isPrivateSpaceLocked(): Boolean? {
        val handle = getPrivateProfileHandle() ?: return null
        return userManager.isQuietModeEnabled(handle)
    }

    fun setPrivateSpaceLocked(locked: Boolean): Boolean {
        val handle = getPrivateProfileHandle() ?: return false
        return try {
            userManager.requestQuietModeEnabled(locked, handle)
        } catch (e: SecurityException) {
            false
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
