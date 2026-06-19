package com.rama.mako.managers

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import com.rama.bohio.managers.ThemeManager

class HomeBackgroundManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PrefsManager.getInstance(appContext)
    private val wallpaperManager by lazy { WallpaperManager.getInstance(appContext) }

    fun applyTo(view: View, modeOverride: String? = null) {
        val mode = modeOverride ?: prefs.getHomeBackgroundMode()
        view.background = createBackgroundDrawable(mode)
    }

    fun applyToSettings(view: View, modeOverride: String? = null) {
        applyTo(view, modeOverride)
    }

    fun createWallpaperOverlayDrawable(): Drawable {
        val strength = prefs.getHomeBackgroundScreenOpacityStrength() // 0..9
        val alpha = (strength * 0x99) / 9
        return ColorDrawable(ColorUtils.setAlphaComponent(Color.BLACK, alpha))
    }

    fun createBackgroundDrawable(mode: String): Drawable {
        val palette = ThemeManager.paletteFor(prefs.getTheme(), appContext)
        return ColorDrawable(palette.bg_1)
    }

    fun getWallpaperSignature(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return runCatching {
            wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()
    }
}