package com.rama.mako.managers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserManager
import java.io.File

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

        // ApplicationInfo.minSdkVersion only exists starting API 24 (N).
        val minSdkVersion: Int
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activityInfo.applicationInfo.minSdkVersion
            } else 0

        val targetSdkVersion: Int
            get() = activityInfo.applicationInfo.targetSdkVersion
    }

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = mutableMapOf<String, Drawable>()
    private val appSizeCache = mutableMapOf<String, Long>()
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getAll(): List<AppEntry> {
        val realApps = userManager.userProfiles.flatMap { userHandle ->
            val profileInitial = getProfileInitial(userHandle)

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
                    profileInitial = profileInitial
                )
            }
        }

        return realApps
    }

    fun launch(app: AppEntry): Boolean {
        return try {
            launcherApps.startMainActivity(
                app.activityInfo!!.componentName,
                app.userHandle,
                null,
                null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openAppDetails(app: AppEntry): Boolean {
        return try {
            launcherApps.startAppDetailsActivity(
                app.activityInfo!!.componentName,
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
            app.activityInfo!!.getIcon(context.resources.displayMetrics.densityDpi)
        }
    }

    // Size on disk of the installed APK(s) (base + splits). Cached since it
    // requires file stat calls and only changes when an app is updated.
    fun getAppSizeBytes(app: AppEntry): Long {
        val key = "${app.packageName}:${app.userHandle.hashCode()}"
        return appSizeCache.getOrPut(key) {
            runCatching {
                val info = app.activityInfo.applicationInfo
                var total = File(info.sourceDir).length()
                info.splitSourceDirs?.forEach { split ->
                    total += File(split).length()
                }
                total
            }.getOrDefault(0L)
        }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null

        val handle = getPrivateProfileHandle() ?: return null
        return userManager.isQuietModeEnabled(handle)
    }

    fun setPrivateSpaceLocked(locked: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false

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
            .firstOrNull { it.isLetter() }
            ?.uppercase()
            ?: "E"
    }
}
