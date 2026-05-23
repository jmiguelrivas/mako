package com.rama.mako.activities.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import com.rama.mako.widgets.WdCheckbox
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.FontManager
import com.rama.mako.managers.PrefsManager
import com.rama.mako.managers.ThemeManager
import java.io.File
import java.io.FileOutputStream
import com.rama.mako.widgets.WdColorPicker
import com.rama.mako.widgets.WdRange

class SettingsAppearanceController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs

    fun setup() {
        setupFontStyle()
        setupTemperatureFormat()
        setupTheme()
        setupCustomTheme()
        setupBackgroundMode()
        setupUiScale()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == activity.FONT_PICK_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val savedPath = copyFontToInternalStorage(uri)
            if (savedPath != null) {
                FontManager.clearCustomCache()
                prefs.setCustomFontPath(savedPath)
                prefs.setFontStyle(PrefsManager.FontStyle.CUSTOM)
                updateCustomFontLabel()
                activity.refreshFont()
            }
        }
    }

    private fun setupFontStyle() {
        val group = activity.findViewById<RadioGroup>(R.id.font_style_group)

        when (prefs.getFontStyle()) {
            PrefsManager.FontStyle.JERSEY_25 -> group.check(R.id.font_jersey)
            PrefsManager.FontStyle.CUSTOM -> group.check(R.id.font_custom)
            else -> group.check(R.id.font_default)
        }

        group.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.font_jersey -> {
                    prefs.setFontStyle(PrefsManager.FontStyle.JERSEY_25)
                    activity.refreshFont()
                }

                R.id.font_default -> {
                    prefs.setFontStyle(PrefsManager.FontStyle.DEFAULT)
                    activity.refreshFont()
                }

                R.id.font_custom -> {
                    prefs.setFontStyle(PrefsManager.FontStyle.CUSTOM)
                    activity.refreshFont()
                }
            }
        }

        // Button to (re-)pick a font file
        activity.findViewById<View>(R.id.font_custom_pick_btn).setOnClickListener {
            openFontPicker()
        }

        updateCustomFontLabel()
    }

    private fun openFontPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "font/ttf", "font/otf", "application/x-font-ttf",
                    "application/x-font-otf", "application/octet-stream"
                )
            )
        }
        activity.startActivityForResult(intent, activity.FONT_PICK_REQUEST)
    }

    private fun copyFontToInternalStorage(uri: Uri): String? {
        return runCatching {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return null
            val dir = File(activity.filesDir, "fonts").also { it.mkdirs() }
            // Preserve extension (.ttf / .otf) for Typeface.createFromFile
            val ext = activity.contentResolver.getType(uri)
                ?.let { if (it.contains("otf")) "otf" else "ttf" } ?: "ttf"
            val dest = File(dir, "custom_font.$ext")
            FileOutputStream(dest).use { out -> inputStream.copyTo(out) }
            dest.absolutePath
        }.getOrNull()
    }

    private fun updateCustomFontLabel() {
        val label = activity.findViewById<TextView>(R.id.font_custom_name_label)
        val path = prefs.getCustomFontPath()
        label.text =
            if (path.isNotBlank()) File(path).name else activity.getString(R.string.filepicker_font_custom_none)
    }

    private fun setupTemperatureFormat() {
        val group = activity.findViewById<RadioGroup>(R.id.temperature_format_group)

        when (prefs.getTemperatureFormat()) {
            PrefsManager.TemperatureFormat.CELSIUS -> group.check(R.id.temperature_celsius)
            PrefsManager.TemperatureFormat.FAHRENHEIT -> group.check(R.id.temperature_fahrenheit)
            else -> group.check(R.id.temperature_system)
        }

        group.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.temperature_celsius -> prefs.setTemperatureFormat(PrefsManager.TemperatureFormat.CELSIUS)
                R.id.temperature_fahrenheit -> prefs.setTemperatureFormat(PrefsManager.TemperatureFormat.FAHRENHEIT)
                else -> prefs.setTemperatureFormat(PrefsManager.TemperatureFormat.DEFAULT)
            }
        }
    }

    private fun setupTheme() {
        val group = activity.findViewById<RadioGroup>(R.id.themes_group)
        val form = activity.findViewById<View>(R.id.themes_form)

        // Show form only if custom is already selected
        form.visibility =
            if (prefs.getTheme() == PrefsManager.Theme.CUSTOM) View.VISIBLE else View.GONE

        when (prefs.getTheme()) {
            PrefsManager.Theme.RAMA -> group.check(R.id.theme_rama)
            PrefsManager.Theme.MAKO -> group.check(R.id.theme_mako)
            PrefsManager.Theme.CATPPUCCIN_MOCHA -> group.check(R.id.theme_catppuccin_mocha)
            PrefsManager.Theme.DRACULA -> group.check(R.id.theme_dracula)
            PrefsManager.Theme.MELANGE -> group.check(R.id.theme_melange)
            PrefsManager.Theme.TOKYO_NIGHT -> group.check(R.id.theme_tokyo_night)
            PrefsManager.Theme.CUSTOM -> group.check(R.id.theme_custom)
            else -> group.check(R.id.theme_mako)
        }

        group.setOnCheckedChangeListener { _, id ->
            val theme = when (id) {
                R.id.theme_rama -> PrefsManager.Theme.RAMA
                R.id.theme_mako -> PrefsManager.Theme.MAKO
                R.id.theme_catppuccin_mocha -> PrefsManager.Theme.CATPPUCCIN_MOCHA
                R.id.theme_dracula -> PrefsManager.Theme.DRACULA
                R.id.theme_melange -> PrefsManager.Theme.MELANGE
                R.id.theme_tokyo_night -> PrefsManager.Theme.TOKYO_NIGHT
                R.id.theme_custom -> PrefsManager.Theme.CUSTOM
                else -> PrefsManager.Theme.MAKO
            }

            // Show/hide the custom form
            form.visibility = if (theme == PrefsManager.Theme.CUSTOM) View.VISIBLE else View.GONE

            if (theme != PrefsManager.Theme.CUSTOM) {
                // For built-in themes: save and apply immediately
                prefs.setTheme(theme)
                activity.recreate()
            } else {
                // For custom: save selection immediately so it persists navigation,
                // then populate fields with current custom palette (or MAKO defaults)
                val previousTheme = prefs.getTheme()
                prefs.setTheme(PrefsManager.Theme.CUSTOM)
                populateCustomFields(ThemeManager.paletteFor(previousTheme, activity))
            }
        }
    }

    private fun populateCustomFields(palette: ThemeManager.Palette) {
        activity.findViewById<WdColorPicker>(R.id.foreground).setColor(palette.foreground)
        activity.findViewById<WdColorPicker>(R.id.collapsible_header)
            .setColor(palette.collapsible_header)
        activity.findViewById<WdColorPicker>(R.id.h1).setColor(palette.h1)
        activity.findViewById<WdColorPicker>(R.id.icons).setColor(palette.icon)
        activity.findViewById<WdColorPicker>(R.id.accent).setColor(palette.accent_1)
        activity.findViewById<WdColorPicker>(R.id.bg_2).setColor(palette.bg_2)
        activity.findViewById<WdColorPicker>(R.id.bg_3).setColor(palette.bg_3)
        activity.findViewById<WdColorPicker>(R.id.bg_1).setColor(palette.bg_1)
        activity.findViewById<WdColorPicker>(R.id.input).setColor(palette.input)
        activity.findViewById<WdColorPicker>(R.id.btn_1).setColor(palette.button_1)
        activity.findViewById<WdColorPicker>(R.id.btn_2).setColor(palette.button_2)
        activity.findViewById<WdColorPicker>(R.id.danger).setColor(palette.danger)
    }

    private fun setupCustomTheme() {
        // Populate fields with current theme palette on open
        val currentPalette = ThemeManager.paletteFor(prefs.getTheme(), activity)
        populateCustomFields(currentPalette)

        val saveButton = activity.findViewById<android.view.View>(R.id.save_custom_theme)
        saveButton.setOnClickListener {
            val fields = mapOf(
                PrefsManager.PrefKeys.APP_THEME_FOREGROUND to activity.findViewById<WdColorPicker>(R.id.foreground),
                PrefsManager.PrefKeys.APP_THEME_COLLAPSIBLE_HEADER to activity.findViewById<WdColorPicker>(
                    R.id.collapsible_header
                ),
                PrefsManager.PrefKeys.APP_THEME_H1 to activity.findViewById<WdColorPicker>(R.id.h1),
                PrefsManager.PrefKeys.APP_THEME_ICON to activity.findViewById<WdColorPicker>(R.id.icons),
                PrefsManager.PrefKeys.APP_THEME_ACCENT_1 to activity.findViewById<WdColorPicker>(R.id.accent),
                PrefsManager.PrefKeys.APP_THEME_BG_1 to activity.findViewById<WdColorPicker>(R.id.bg_1),
                PrefsManager.PrefKeys.APP_THEME_BG_2 to activity.findViewById<WdColorPicker>(R.id.bg_2),
                PrefsManager.PrefKeys.APP_THEME_BG_3 to activity.findViewById<WdColorPicker>(R.id.bg_3),
                PrefsManager.PrefKeys.APP_THEME_INPUT to activity.findViewById<WdColorPicker>(R.id.input),
                PrefsManager.PrefKeys.APP_THEME_BUTTON_1 to activity.findViewById<WdColorPicker>(R.id.btn_1),
                PrefsManager.PrefKeys.APP_THEME_BUTTON_2 to activity.findViewById<WdColorPicker>(R.id.btn_2),
                PrefsManager.PrefKeys.APP_THEME_DANGER to activity.findViewById<WdColorPicker>(R.id.danger),
            )

            fields.forEach { (key, colorPicker) ->
                val color = colorPicker.getColor()
                prefs.setCustomThemeColor(key, color)
            }
            prefs.setTheme(PrefsManager.Theme.CUSTOM)
            activity.recreate()

        }
    }

    private fun setupBackgroundMode() {
        val checkbox = activity.findViewById<WdCheckbox>(R.id.home_background_wallpaper)

        val initialMode = prefs.getHomeBackgroundMode()
        checkbox.setChecked(initialMode == PrefsManager.BackgroundMode.WALLPAPER)

        checkbox.setOnCheckedChangeListener { isChecked ->
            val mode = if (isChecked) {
                PrefsManager.BackgroundMode.WALLPAPER
            } else {
                PrefsManager.BackgroundMode.DEFAULT
            }

            prefs.setHomeBackgroundMode(mode)
            activity.applySettingsBackground()
        }
    }

    private fun setupUiScale() {
        val range = activity.findViewById<WdRange>(R.id.zoom)

        val savedScale = prefs.getUiScale()

        range.onValueChanged = { value ->
            val scale = value.toFloatOrNull() ?: 1f
            if (scale != prefs.getUiScale()) {
                prefs.setUiScale(scale)
                activity.recreate()
            }
        }

        val steps = activity.resources.getStringArray(R.array.ui_scale_steps).toList()
        val matchIndex = steps.indexOfFirst { it.toFloatOrNull() == savedScale }
        if (matchIndex >= 0) {
            range.post {
                val container = range.findViewById<android.widget.LinearLayout>(R.id.container)
                (container?.getChildAt(matchIndex) as? android.widget.Button)?.performClick()
            }
        }
    }
}