package com.rama.mako.managers

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import com.rama.bohio.util.IdUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rama.bohio.objects.PrefKeys
import com.rama.bohio.objects.PrefTheme
import com.rama.bohio.managers.PrefsManager as BohioPrefsManager

class PrefsManager private constructor(context: Context) : BohioPrefsManager(context) {
    override val defaultTheme: String = PrefTheme.RAMA

    // Local preference keys
    object FileKeys {
        const val APPS_SEARCH = "apps:search"
        const val APPS_SEARCH_ALWAYS_VISIBLE = "apps:search:always_visible"
        const val APPS_PROFILE_INDICATOR = "apps:profile_indicator"

        const val APPS_ICONS = "apps:icons"
        const val APPS_ICON_SOURCE = "apps:icon_source"
        const val APPS_ICON_PACK_PACKAGE = "apps:icon_pack_package"
        const val APPS_ICONS_OPEN_SETTINGS = "apps:icons:open_settings"

        const val HOME_BACKGROUND_MODE = "home:background_mode"
        const val HOME_DOUBLE_TAP_SLEEP = "home:double_tap_sleep"
        const val HOME_DOUBLE_TAP_LOCK_METHOD = "home:double_tap_lock_method"
        const val HOME_BACKGROUND_MODE_SCREEN_OPACITY_STRENGTH =
            "home:background_mode:screen_opacity_strength"
        const val GROUPS_IDS = "groups:ids"
        const val GROUPS_HEADERS = "groups:headers"
        const val GROUPS_COLLAPSIBLE = "groups:collapsible"
        const val GROUPS_COLLAPSE_ON_HOME_FOCUS = "groups:collapse_on_home_focus"
        const val DATE_VISIBLE = "date:visible"
        const val DATE_YEAR_DAY = "date:year_day"
        const val BATTERY_VISIBLE = "battery:visible"
        const val BATTERY_TEMPERATURE = "battery:temperature"
        const val TEMPERATURE_FORMAT = "temperature:format"
        const val BATTERY_CHARGE_STATUS = "battery:charge_status"
        const val CLOCK_FORMAT = "clock:format"
        const val CLOCK_APP = "clock:app"
        const val DATE_APP = "date:app"
        const val MIGRATION_ICON_SOURCE_RADIO = "migration:icon_source_radio"

        const val SECURITY_KEYPAD_VISIBLE = "security:keypad:visible"
        const val SECURITY_KEYPAD_RANDOMIZED = "security:keypad:randomized"
        const val SECURITY_PIN = "security:pin"

        fun appKey(pkg: String, userHandle: UserHandle): String {
            val userId = userHandle.hashCode()
            return if (userId == 0) "app:$pkg" else "app:$pkg:profile_$userId"
        }

        fun APP_GROUP_ID(pkg: String, userHandle: UserHandle) =
            "${appKey(pkg, userHandle)}:group_id"

        fun APP_CUSTOM_LABEL(pkg: String, userHandle: UserHandle) =
            "${appKey(pkg, userHandle)}:custom_label"

        fun GROUP_LABEL(id: String) = "group:$id:label"
        fun GROUP_VISIBLE(id: String) = "group:$id:visible"
        fun GROUP_EXPANDED(id: String) = "group:$id:expanded"
        fun GROUP_ORDER(id: String) = "group:$id:order"
    }

    object UI {
        const val SEPARATOR = "------"
        const val UNGROUPED_LABEL = "$SEPARATOR Default"
        const val FAVORITES_LABEL = "$SEPARATOR Favorites"
    }

    object SystemIds {
        val UNGROUPED = IdUtils.toBase36Fixed(0)
        val FAVORITES = IdUtils.toBase36Fixed(1)
    }

    object ClockFormat {
        const val NONE = "none"
        const val DEFAULT = "default"
        const val HOUR_12 = "12-hour"
        const val HOUR_24 = "24-hour"
    }

    object IconSource {
        const val NONE = "none"
        const val SYSTEM = "system"
        const val MONOCHROME = "monochrome"
        const val ICON_PACK = "icon_pack"
    }

    object TemperatureFormat {
        const val DEFAULT = "default"
        const val CELSIUS = "celsius"
        const val FAHRENHEIT = "fahrenheit"
    }

    object BackgroundMode {
        const val DEFAULT = "default"
        const val WALLPAPER = "wallpaper"
    }

    // Local InitPrefs
    override fun applyAppDefaults(editor: SharedPreferences.Editor) {
        val defaultIds = setOf(
            SystemIds.UNGROUPED,
            SystemIds.FAVORITES
        )

        editor.putStringSet(FileKeys.GROUPS_IDS, defaultIds)

        editor.putString(FileKeys.GROUP_LABEL(SystemIds.UNGROUPED), UI.UNGROUPED_LABEL)
        editor.putBoolean(FileKeys.GROUP_VISIBLE(SystemIds.UNGROUPED), true)
        editor.putBoolean(FileKeys.GROUP_EXPANDED(SystemIds.UNGROUPED), true)
        editor.putInt(FileKeys.GROUP_ORDER(SystemIds.UNGROUPED), 0)

        editor.putString(FileKeys.GROUP_LABEL(SystemIds.FAVORITES), UI.FAVORITES_LABEL)
        editor.putBoolean(FileKeys.GROUP_VISIBLE(SystemIds.FAVORITES), true)
        editor.putBoolean(FileKeys.GROUP_EXPANDED(SystemIds.FAVORITES), true)
        editor.putInt(FileKeys.GROUP_ORDER(SystemIds.FAVORITES), 1)

        editor.putString(FileKeys.CLOCK_FORMAT, ClockFormat.HOUR_24)
        editor.putString(FileKeys.CLOCK_APP, "")
        editor.putString(FileKeys.DATE_APP, "")

        editor.putBoolean(FileKeys.APPS_SEARCH, false)
        editor.putBoolean(FileKeys.APPS_PROFILE_INDICATOR, true)

        editor.putBoolean(FileKeys.APPS_ICONS, false)
        editor.putString(FileKeys.APPS_ICON_SOURCE, IconSource.NONE)
        editor.putString(FileKeys.APPS_ICON_PACK_PACKAGE, "")
        editor.putBoolean(FileKeys.APPS_ICONS_OPEN_SETTINGS, true)

        editor.putString(FileKeys.HOME_BACKGROUND_MODE, BackgroundMode.DEFAULT)
        editor.putBoolean(FileKeys.HOME_DOUBLE_TAP_SLEEP, false)
        editor.putString(FileKeys.HOME_DOUBLE_TAP_LOCK_METHOD, "device_admin")
        editor.putInt(FileKeys.HOME_BACKGROUND_MODE_SCREEN_OPACITY_STRENGTH, 9)

        editor.putBoolean(FileKeys.BATTERY_VISIBLE, true)
        editor.putBoolean(FileKeys.BATTERY_TEMPERATURE, true)
        editor.putString(FileKeys.TEMPERATURE_FORMAT, TemperatureFormat.DEFAULT)
        editor.putBoolean(FileKeys.BATTERY_CHARGE_STATUS, false)

        editor.putBoolean(FileKeys.DATE_VISIBLE, true)
        editor.putBoolean(FileKeys.DATE_YEAR_DAY, true)

        editor.putBoolean(FileKeys.GROUPS_HEADERS, true)
        editor.putBoolean(FileKeys.GROUPS_COLLAPSIBLE, true)
        editor.putBoolean(FileKeys.GROUPS_COLLAPSE_ON_HOME_FOCUS, false)
        editor.putBoolean(FileKeys.SECURITY_KEYPAD_RANDOMIZED, true)
        editor.putBoolean(FileKeys.SECURITY_KEYPAD_VISIBLE, false)

        editor.putBoolean(PrefKeys.SETTINGS_SECTION_CLOCK, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_TEMPERATURE, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_BACKGROUND, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_DATE, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_BATTERY, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_ICONS, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_GROUPS, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_SEARCH, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_DATA, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_APPS, true)
        editor.putBoolean(PrefKeys.SETTINGS_SECTION_SECURITY, true)

        fun migrateLegacyPrefs(sync: Boolean = false) {
            val editor = prefs.edit()
            var hasChanges = false

            if (!prefs.getBoolean(FileKeys.MIGRATION_ICON_SOURCE_RADIO, false)) {
                val iconsEnabled = prefs.getBoolean(FileKeys.APPS_ICONS, false)
                val currentSource = prefs.getString(FileKeys.APPS_ICON_SOURCE, IconSource.SYSTEM)

                val normalizedSource = when (currentSource) {
                    IconSource.NONE -> IconSource.NONE
                    IconSource.MONOCHROME -> IconSource.MONOCHROME
                    IconSource.ICON_PACK -> IconSource.ICON_PACK
                    else -> IconSource.SYSTEM
                }

                val migratedSource = if (iconsEnabled) normalizedSource else IconSource.NONE

                editor.putString(FileKeys.APPS_ICON_SOURCE, migratedSource)
                editor.putBoolean(FileKeys.MIGRATION_ICON_SOURCE_RADIO, true)
                hasChanges = true
            }

            if (hasChanges) {
                if (sync) editor.commit() else editor.apply()
            }
        }

        migrateLegacyPrefs(true)
    }

    private val encryptedPrefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun addGroupId(id: String) {
        val updated = getGroupIds().toMutableSet()
        updated.add(id)
        setGroupIds(updated)
    }

    fun removeGroupId(id: String) {
        val updated = getGroupIds().toMutableSet()
        updated.remove(id)
        setGroupIds(updated)
    }

    fun getAppGroupId(pkg: String, userHandle: UserHandle): String {
        return prefs.getString(FileKeys.APP_GROUP_ID(pkg, userHandle), null)
            ?: SystemIds.UNGROUPED.also {
                prefs.edit().putString(FileKeys.APP_GROUP_ID(pkg, userHandle), it).apply()
            }
    }

    fun setAppGroupId(pkg: String, userHandle: UserHandle, groupId: String?) {
        val key = FileKeys.APP_GROUP_ID(pkg, userHandle)
        if (groupId != null) {
            prefs.edit().putString(key, groupId).apply()
        } else {
            prefs.edit().remove(key).apply()
        }
    }

    fun getGroupIds(): Set<String> =
        prefs.getStringSet(FileKeys.GROUPS_IDS, emptySet()) ?: emptySet()

    fun setGroupIds(ids: Set<String>) =
        prefs.edit().putStringSet(FileKeys.GROUPS_IDS, ids).apply()

    fun getGroupLabel(id: String): String =
        prefs.getString(FileKeys.GROUP_LABEL(id), "") ?: ""

    fun setGroupLabel(id: String, value: String) =
        prefs.edit().putString(FileKeys.GROUP_LABEL(id), value).apply()

    fun isGroupVisible(id: String): Boolean =
        prefs.getBoolean(FileKeys.GROUP_VISIBLE(id), false)

    fun setGroupVisible(id: String, value: Boolean) =
        prefs.edit().putBoolean(FileKeys.GROUP_VISIBLE(id), value).apply()

    fun isGroupExpanded(id: String): Boolean =
        prefs.getBoolean(FileKeys.GROUP_EXPANDED(id), false)

    fun setGroupExpanded(id: String, value: Boolean) =
        prefs.edit().putBoolean(FileKeys.GROUP_EXPANDED(id), value).apply()

    fun setGroupsExpanded(ids: Set<String>, expanded: Boolean) {
        val editor = prefs.edit()
        ids.forEach { editor.putBoolean(FileKeys.GROUP_EXPANDED(it), expanded) }
        editor.apply()
    }

    fun hasGroupOrder(id: String): Boolean =
        prefs.contains(FileKeys.GROUP_ORDER(id))

    fun getGroupOrder(id: String): Int =
        prefs.getInt(FileKeys.GROUP_ORDER(id), Int.MAX_VALUE)

    fun setGroupOrder(id: String, value: Int) =
        prefs.edit().putInt(FileKeys.GROUP_ORDER(id), value).apply()

    fun isSearchVisible(): Boolean =
        prefs.getBoolean(FileKeys.APPS_SEARCH, false)

    fun isSearchBarAlwaysVisible(): Boolean =
        prefs.getBoolean(FileKeys.APPS_SEARCH_ALWAYS_VISIBLE, false)

    fun hasIconsVisible(): Boolean =
        getIconSource() != IconSource.NONE

    fun hasProfileIndicator(): Boolean =
        prefs.getBoolean(FileKeys.APPS_PROFILE_INDICATOR, true)

    fun hasIconsOpenSettings(): Boolean =
        prefs.getBoolean(FileKeys.APPS_ICONS_OPEN_SETTINGS, true)

    fun getIconSource(): String {
        return when (prefs.getString(FileKeys.APPS_ICON_SOURCE, IconSource.NONE)) {
            IconSource.NONE -> IconSource.NONE
            IconSource.MONOCHROME -> IconSource.MONOCHROME
            IconSource.ICON_PACK -> IconSource.ICON_PACK
            IconSource.SYSTEM -> IconSource.SYSTEM
            else -> IconSource.NONE
        }
    }

    fun setIconSource(source: String) {
        val normalized = when (source) {
            IconSource.NONE -> IconSource.NONE
            IconSource.MONOCHROME -> IconSource.MONOCHROME
            IconSource.ICON_PACK -> IconSource.ICON_PACK
            else -> IconSource.SYSTEM
        }
        prefs.edit().putString(FileKeys.APPS_ICON_SOURCE, normalized).apply()
    }

    fun getIconPackPackage(): String =
        prefs.getString(FileKeys.APPS_ICON_PACK_PACKAGE, "") ?: ""

    fun setIconPackPackage(packageName: String) =
        prefs.edit().putString(FileKeys.APPS_ICON_PACK_PACKAGE, packageName).apply()

    fun hasGroupHeaders(): Boolean =
        prefs.getBoolean(FileKeys.GROUPS_HEADERS, false)

    fun hasCollapsibleGroups(): Boolean =
        prefs.getBoolean(FileKeys.GROUPS_COLLAPSIBLE, false)

    fun shouldCollapseGroupsOnHomeFocus(): Boolean =
        prefs.getBoolean(FileKeys.GROUPS_COLLAPSE_ON_HOME_FOCUS, false)

    fun isDoubleTapToSleepEnabled(): Boolean =
        prefs.getBoolean(FileKeys.HOME_DOUBLE_TAP_SLEEP, false)

    fun setDoubleTapToSleepEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(FileKeys.HOME_DOUBLE_TAP_SLEEP, enabled).apply()

    fun getDoubleTapLockMethod(): String =
        prefs.getString(FileKeys.HOME_DOUBLE_TAP_LOCK_METHOD, "device_admin") ?: "device_admin"

    fun setDoubleTapLockMethod(method: String) =
        prefs.edit().putString(FileKeys.HOME_DOUBLE_TAP_LOCK_METHOD, method).apply()

    fun getClockFormat(): String =
        prefs.getString(FileKeys.CLOCK_FORMAT, "") ?: ""

    fun setClockFormat(format: String) =
        prefs.edit().putString(FileKeys.CLOCK_FORMAT, format).apply()

    fun getClockApp(): String =
        prefs.getString(FileKeys.CLOCK_APP, "") ?: ""

    fun setClockApp(appId: String) =
        prefs.edit().putString(FileKeys.CLOCK_APP, appId).apply()

    fun getDateApp(): String =
        prefs.getString(FileKeys.DATE_APP, "") ?: ""

    fun setDateApp(appId: String) =
        prefs.edit().putString(FileKeys.DATE_APP, appId).apply()

    // SETTINGS - DATE

    fun isDateVisible(): Boolean =
        prefs.getBoolean(FileKeys.DATE_VISIBLE, false)

    fun isYearDayVisible(): Boolean =
        prefs.getBoolean(FileKeys.DATE_YEAR_DAY, false)

    fun isBatteryVisible(): Boolean =
        prefs.getBoolean(FileKeys.BATTERY_VISIBLE, false)

    fun isBatteryTemperatureVisible(): Boolean =
        prefs.getBoolean(FileKeys.BATTERY_TEMPERATURE, false)

    fun getTemperatureFormat(): String {
        return when (prefs.getString(FileKeys.TEMPERATURE_FORMAT, TemperatureFormat.DEFAULT)) {
            TemperatureFormat.CELSIUS -> TemperatureFormat.CELSIUS
            TemperatureFormat.FAHRENHEIT -> TemperatureFormat.FAHRENHEIT
            else -> TemperatureFormat.DEFAULT
        }
    }

    fun setTemperatureFormat(format: String) {
        val normalized = when (format) {
            TemperatureFormat.CELSIUS -> TemperatureFormat.CELSIUS
            TemperatureFormat.FAHRENHEIT -> TemperatureFormat.FAHRENHEIT
            else -> TemperatureFormat.DEFAULT
        }
        prefs.edit().putString(FileKeys.TEMPERATURE_FORMAT, normalized).apply()
    }

    fun isBatteryChargeStatusVisible(): Boolean =
        prefs.getBoolean(FileKeys.BATTERY_CHARGE_STATUS, false)

    fun getHomeBackgroundScreenOpacityStrength(): Int {
        return prefs.getInt(FileKeys.HOME_BACKGROUND_MODE_SCREEN_OPACITY_STRENGTH, 9)
    }

    fun setHomeBackgroundScreenOpacityStrength(strength: Int) {
        prefs.edit().putInt(FileKeys.HOME_BACKGROUND_MODE_SCREEN_OPACITY_STRENGTH, strength).apply()
    }

    fun getHomeBackgroundMode(): String {
        return when (prefs.getString(FileKeys.HOME_BACKGROUND_MODE, BackgroundMode.DEFAULT)) {
            BackgroundMode.WALLPAPER -> BackgroundMode.WALLPAPER
            else -> BackgroundMode.DEFAULT
        }
    }

    fun setHomeBackgroundMode(mode: String) {
        val normalized = when (mode) {
            BackgroundMode.WALLPAPER -> BackgroundMode.WALLPAPER
            else -> BackgroundMode.DEFAULT
        }

        prefs.edit().putString(FileKeys.HOME_BACKGROUND_MODE, normalized).apply()
    }

    fun getPin(): String =
        encryptedPrefs.getString(FileKeys.SECURITY_PIN, "") ?: ""

    fun setPin(pin: String) =
        encryptedPrefs.edit().putString(FileKeys.SECURITY_PIN, pin).apply()

    fun clearPin() =
        encryptedPrefs.edit().remove(FileKeys.SECURITY_PIN).apply()

    fun isLockEnabled(): Boolean =
        prefs.getBoolean(FileKeys.SECURITY_KEYPAD_VISIBLE, false)

    fun isKeypadRandomized(): Boolean =
        prefs.getBoolean(FileKeys.SECURITY_KEYPAD_RANDOMIZED, true)

    fun getCustomName(pkg: String, userHandle: UserHandle): String? =
        prefs.getString(FileKeys.APP_CUSTOM_LABEL(pkg, userHandle), null)
            ?.takeIf { it.isNotEmpty() }

    fun setCustomName(pkg: String, userHandle: UserHandle, name: String) =
        prefs.edit().putString(FileKeys.APP_CUSTOM_LABEL(pkg, userHandle), name).apply()

    fun clearCustomName(pkg: String, userHandle: UserHandle) =
        prefs.edit().remove(FileKeys.APP_CUSTOM_LABEL(pkg, userHandle)).apply()

    companion object {
        @Volatile
        private var INSTANCE: PrefsManager? = null

        fun getInstance(context: Context): PrefsManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrefsManager(context.applicationContext).also {
                    INSTANCE = it
                    register(it)
                }
            }
    }
}