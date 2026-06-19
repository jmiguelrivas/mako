package com.rama.mako

import android.content.Context
import android.view.WindowManager
import com.rama.bohio.managers.ThemeManager
import com.rama.bohio.activity.BohioActivity
import com.rama.mako.managers.PrefsManager

abstract class CsActivity : BohioActivity() {
    val prefs by lazy { PrefsManager.getInstance(this) }

    override fun attachBaseContext(newBase: Context) {
        PrefsManager.getInstance(newBase.applicationContext)
        super.attachBaseContext(newBase)
    }

    override fun onResume() {
        super.onResume()
        applyWindowBackground()
    }

    fun applyWindowBackground() {
        val palette = ThemeManager.paletteFor(prefs.getTheme(), this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.bg_1))
    }
}
