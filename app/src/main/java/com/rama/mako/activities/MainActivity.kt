package com.rama.mako.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ListView
import android.widget.TextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.rama.mako.CsActivity
import com.rama.mako.R
import com.rama.mako.managers.AppListManager
import com.rama.mako.managers.AppsProvider
import com.rama.mako.managers.BatteryManager
import com.rama.mako.managers.ClockManager
import com.rama.mako.managers.HomeBackgroundManager
import com.rama.mako.managers.DoubleTapLockManager
import com.rama.mako.managers.PrefsManager
import com.rama.mako.managers.ThemeManager

class MainActivity : CsActivity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryText: TextView
    private lateinit var listView: ListView

    private lateinit var clockManager: ClockManager
    private lateinit var batteryManager: BatteryManager
    private lateinit var appListManager: AppListManager
    private lateinit var appsProvider: AppsProvider

    private lateinit var homeBackgroundManager: HomeBackgroundManager
    private lateinit var rootView: View

    private lateinit var searchField: EditText
    private lateinit var searchIcon: FrameLayout
    private lateinit var clearBtn: FrameLayout
    private var isSearchBarAlwaysVisible = false

    private var isSearchExpanded = false
    private var isProgrammaticSearchUpdate = false
    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var searchDebounceRunnable: Runnable? = null
    private var resumeRefreshRunnable: Runnable? = null
    private var currentSearchQuery: String = ""
    private var wallpaperReceiverRegistered = false
    private var lastAppliedBackgroundMode: String? = null
    private var lastAppliedWallpaperSignature: Int? = null
    private var isDoubleTapToSleepEnabled = false
    private lateinit var doubleTapGestureDetector: GestureDetector
    private lateinit var doubleTapLockManager: DoubleTapLockManager
    private var lastAppliedTheme: String? = null

    companion object {
        private const val WALLPAPER_CHANGED_ACTION = "android.intent.action.WALLPAPER_CHANGED"
    }

    private val wallpaperChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == WALLPAPER_CHANGED_ACTION) {
                applyHomeBackground()
            }
        }
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_F10 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.getInstance(this).initPrefs()
        setContentView(R.layout.view_home)

        rootView = findViewById(R.id.root)
        applyEdgeToEdgePadding(rootView)
        initDoubleTapToSleep()
        applyCurrentTheme(rootView)
        rootView.isFocusableInTouchMode = false
        rootView.requestFocus()
        val palette = ThemeManager.paletteFor(prefs.getTheme())

        homeBackgroundManager = HomeBackgroundManager(this)
        applyHomeBackground(force = true)

        timeText = findViewById(R.id.time)
        dateText = findViewById(R.id.date)
        batteryText = findViewById(R.id.battery)
        listView = findViewById(R.id.app_list)

        clockManager = ClockManager(timeText, dateText, this)
        clockManager.start()
        timeText.setOnClickListener { openSystemClock() }
        dateText.setOnClickListener { openDateApp() }

        batteryManager = BatteryManager(
            context = this,
            callback = { status -> batteryText.text = status },
        )
        batteryManager.register()

        appsProvider = AppsProvider(this)
        appListManager = AppListManager(
            this,
            listView,
            appsProvider
        ) {
            if (isSearchExpanded) {
                collapseSearch()
            }
        }
        appListManager.setup()

        timeText.setTextColor(palette.h1)

        val appLayout = findViewById<LinearLayout>(R.id.apps_layout)
        appLayout.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        initSearchbar()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (appListManager.handleBackPress()) return

                if (isSearchBarAlwaysVisible) {
                    searchField.clearFocus()
                } else if (isSearchExpanded) {
                    collapseSearch()
                } else {
                    finish()
                }
            }
        })

    }

    private fun initDoubleTapToSleep() {
        doubleTapLockManager = DoubleTapLockManager(this)
        doubleTapGestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    return lockScreenOnDoubleTap()
                }
            }
        )
    }

    private fun initSearchbar() {
        searchField = findViewById(R.id.search_field)
        searchIcon = findViewById(R.id.search_icon)
        clearBtn = findViewById(R.id.clear_field)

        searchField.visibility = View.GONE
        clearBtn.visibility = View.GONE

        searchIcon.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            if (isSearchExpanded) {
                collapseSearch()
            } else {
                expandSearch()
            }
        }

        searchField.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticSearchUpdate) return

                val query = s.toString()

                searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }

                searchDebounceRunnable = Runnable {
                    currentSearchQuery = query
                    appListManager.filter(currentSearchQuery)
                }
                searchDebounceHandler.postDelayed(searchDebounceRunnable!!, 300)

                clearBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        clearBtn.setOnClickListener {
            currentSearchQuery = ""
            searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
            isProgrammaticSearchUpdate = true
            searchField.text.clear()
            isProgrammaticSearchUpdate = false
            appListManager.filter("")
            clearBtn.visibility = View.GONE
        }
    }

    private fun expandSearch() {
        isSearchExpanded = true

        searchField.visibility = View.VISIBLE
        if (!isSearchBarAlwaysVisible)
            searchField.requestFocus()

        val scaleX = ObjectAnimator.ofFloat(searchField, "scaleX", 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(searchField, "scaleY", 0.8f, 1f)
        val alpha = ObjectAnimator.ofFloat(searchField, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300
            interpolator = OvershootInterpolator(1.5f)
            start()
        }

        val imm =
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchField, 0)
    }

    private fun collapseSearch(clearQuery: Boolean = true, hideKeyboard: Boolean = true) {
        isSearchExpanded = false

        searchField.visibility = View.GONE
        clearBtn.visibility = View.GONE
        searchField.clearFocus()

        if (clearQuery) {
            currentSearchQuery = ""
            searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
            isProgrammaticSearchUpdate = true
            searchField.text.clear()
            isProgrammaticSearchUpdate = false
            appListManager.filter("")
        }

        if (hideKeyboard) {
            val imm =
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(searchField.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        applyHomeBackground(force = true)
        unregisterWallpaperReceiverIfNeeded()
        syncSettings()
        schedulePostResumeRefresh()

        if (isSearchBarAlwaysVisible)
            expandSearch()
    }

    override fun onPause() {
        super.onPause()
        unregisterWallpaperReceiverIfNeeded()
        clearPendingResumeRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterWallpaperReceiverIfNeeded()
        clearPendingResumeRefresh()

        searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }

        batteryManager.unregister()
        clockManager.stop()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isDoubleTapToSleepEnabled) {
            doubleTapGestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
    
    private fun syncSettings() {
        val searchVisible = prefs.isSearchVisible()

        isDoubleTapToSleepEnabled = prefs.isDoubleTapToSleepEnabled()
        isSearchBarAlwaysVisible = prefs.isSearchBarAlwaysVisible()
        timeText.visibility =
            if (prefs.getClockFormat() != PrefsManager.ClockFormat.NONE) View.VISIBLE else View.GONE
        findViewById<View>(R.id.date_row).visibility =
            if (prefs.isDateVisible()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.battery_row).visibility =
            if (prefs.isBatteryVisible()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.searchbar).visibility =
            if (searchVisible) View.VISIBLE else View.GONE
        searchIcon.visibility =
            if (searchVisible && !isSearchBarAlwaysVisible) View.VISIBLE else View.GONE
    }

    private fun lockScreenOnDoubleTap(): Boolean {
        if (!isDoubleTapToSleepEnabled) return false
        if (isSearchExpanded || appListManager.isInMultiSelectMode()) return false

        if (!doubleTapLockManager.isCurrentMethodAvailable()) {
            val msg = when (doubleTapLockManager.getMethod()) {
                DoubleTapLockManager.METHOD_ACCESSIBILITY ->
                    getString(R.string.double_tap_sleep_failed_toast)
                else ->
                    getString(R.string.double_tap_sleep_enable_admin_toast)
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }

        if (!doubleTapLockManager.lock()) {
            Toast.makeText(this, getString(R.string.double_tap_sleep_failed_toast), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    // --- Open date app ---
    private fun openDateApp() {
        val packageName = prefs.getDateApp()
        if (packageName.isNotEmpty()) {
            val app = appsProvider.getAll().firstOrNull { it.packageName == packageName }
            if (app != null) {
                if (!appsProvider.launch(app)) {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_unable_launch_app),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

    // --- Open system clock safely ---
    private fun openSystemClock() {
        val packageName = prefs.getClockApp()
        if (packageName.isNotEmpty()) {
            val app = appsProvider.getAll().firstOrNull { it.packageName == packageName }
            if (app != null) {
                if (!appsProvider.launch(app)) {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_unable_launch_app),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
        val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    // The only place in the app where FLAG_SHOW_WALLPAPER is ever set.
    // All other activities get bg_1 via CsActivity.applyWindowBackground().
    private fun applyHomeBackground(force: Boolean = false) {
        val mode = prefs.getHomeBackgroundMode()
        val wallpaperSignature = homeBackgroundManager.getWallpaperSignature()
        val theme = prefs.getTheme()

        if (!force &&
            mode == lastAppliedBackgroundMode &&
            wallpaperSignature == lastAppliedWallpaperSignature &&
            theme == lastAppliedTheme
        ) return

        if (mode == PrefsManager.BackgroundMode.WALLPAPER) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(homeBackgroundManager.createWallpaperOverlayDrawable())
            window.navigationBarColor = Color.TRANSPARENT
            rootView.setBackgroundColor(Color.TRANSPARENT)
        } else {
            applyWindowBackground()
            homeBackgroundManager.applyTo(rootView, mode)
        }

        lastAppliedBackgroundMode = mode
        lastAppliedWallpaperSignature = wallpaperSignature
        lastAppliedTheme = theme
    }

    private fun schedulePostResumeRefresh() {
        clearPendingResumeRefresh()

        resumeRefreshRunnable = Runnable {
            if (isFinishing || isDestroyed) return@Runnable
            appListManager.refresh()
            batteryManager.forceUpdate()
        }

        rootView.post(resumeRefreshRunnable)
    }

    private fun clearPendingResumeRefresh() {
        resumeRefreshRunnable?.let {
            rootView.removeCallbacks(it)
        }
        resumeRefreshRunnable = null
    }

    private fun registerWallpaperReceiverIfNeeded() {
        if (wallpaperReceiverRegistered) return

        val filter = IntentFilter(WALLPAPER_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                wallpaperChangedReceiver,
                filter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(wallpaperChangedReceiver, filter)
        }

        wallpaperReceiverRegistered = true
    }

    private fun unregisterWallpaperReceiverIfNeeded() {
        if (!wallpaperReceiverRegistered) return

        runCatching { unregisterReceiver(wallpaperChangedReceiver) }
        wallpaperReceiverRegistered = false
    }
}
