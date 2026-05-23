package com.rama.mako

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.rama.mako.managers.FontManager
import com.rama.mako.managers.ThemeManager
import com.rama.mako.utils.dp
import com.rama.mako.managers.PrefsManager
import com.rama.mako.utils.LocaleHelper
import kotlin.text.toInt
import kotlin.times

abstract class CsActivity : Activity() {

    val prefs by lazy { PrefsManager.getInstance(this) }
    private var lastKnownAppLanguage: String? = null
    private var lastKnownTheme: String? = null
    private var lastKnownUiScale: Float = -1f

    override fun attachBaseContext(newBase: Context) {
        val localeContext = LocaleHelper.wrapContext(newBase)

        val scale = PrefsManager.getInstance(localeContext).getUiScale()

        val context = if (scale != 1f) {
            // createConfigurationContext derives densityDpi from the *base* context
            // each time, so there is no compounding across recreate() calls.
            val config = Configuration(localeContext.resources.configuration)
            config.densityDpi = (localeContext.resources.displayMetrics.densityDpi * scale).toInt()
            localeContext.createConfigurationContext(config)
        } else {
            localeContext
        }

        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastKnownAppLanguage = prefs.getAppLanguage()
        lastKnownTheme = prefs.getTheme()
        lastKnownUiScale = prefs.getUiScale()

        // Allow drawing behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }

        // Allow drawing into display cutout (notch / camera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onResume() {
        super.onResume()

        val currentLanguage = prefs.getAppLanguage()
        if (currentLanguage != lastKnownAppLanguage) {
            lastKnownAppLanguage = currentLanguage
            recreate()
            return
        }

        val currentTheme = prefs.getTheme()
        if (currentTheme != lastKnownTheme) {
            lastKnownTheme = currentTheme
            recreate()
            return
        }

        val currentUiScale = prefs.getUiScale()
        if (currentUiScale != lastKnownUiScale) {
            lastKnownUiScale = currentUiScale
            recreate()
            return
        }

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyTheme(this, root)
    }

    fun refreshFont() {
        val root = findViewById<View>(android.R.id.content)
        FontManager.applyFont(this, root)
    }

    fun applyCurrentTheme(root: View? = null) {
        val target = root ?: findViewById<View>(android.R.id.content)
        ThemeManager.applyTheme(this, target)
        applyNavBarColor()
    }

    private fun applyNavBarColor() {
        val palette = ThemeManager.paletteFor(prefs.getTheme(), this)
        window.navigationBarColor = palette.bg_1
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(palette.bg_1))
    }

    protected fun updateSystemBars(root: View) {
        if (prefs.isSystemBarVisible()) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        root.requestApplyInsets()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val root = findViewById<View>(android.R.id.content)
            updateSystemBars(root)
        }
    }

    protected fun applyEdgeToEdgePadding(root: View) {
        val paddingInline = dp(16)
        val paddingBlock = dp(8)

        root.setOnApplyWindowInsetsListener { view, insets ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                val sysBars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )

                val ime = insets.getInsets(WindowInsets.Type.ime())

                val bottomInset =
                    if (insets.isVisible(WindowInsets.Type.ime()))
                        ime.bottom
                    else
                        sysBars.bottom

                view.setPadding(
                    sysBars.left + paddingInline,
                    sysBars.top + paddingBlock,
                    sysBars.right + paddingInline,
                    bottomInset + paddingBlock
                )

            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft + paddingInline,
                    insets.systemWindowInsetTop + paddingBlock,
                    insets.systemWindowInsetRight + paddingInline,
                    insets.systemWindowInsetBottom + paddingBlock
                )
            }

            insets
        }
    }
}