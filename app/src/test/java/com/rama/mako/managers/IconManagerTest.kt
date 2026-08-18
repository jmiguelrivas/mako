package com.rama.mako.managers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appsProvider = mockk<AppsProvider>()
    private lateinit var manager: IconManager

    @Before
    fun setUp() {
        resetCompanionField(PrefsManager::class.java, "INSTANCE")
        resetCompanionField(com.rama.bohio.managers.PrefsManager::class.java, "instance")

        manager = IconManager(context, appsProvider)
    }

    private fun resetCompanionField(outerClass: Class<*>, fieldName: String) {
        val outerAttempt = runCatching {
            val field = outerClass.getDeclaredField(fieldName).apply { isAccessible = true }
            field.set(null, null)
        }
        if (outerAttempt.isSuccess) return

        val companionField = outerClass.getDeclaredField("Companion").apply { isAccessible = true }
        val companionInstance = companionField.get(null)
        val targetField = companionInstance.javaClass.getDeclaredField(fieldName)
            .apply { isAccessible = true }
        targetField.set(companionInstance, null)
    }

    @Test
    fun `getIcon with system source returns default appsProvider icon and caches it`() {
        val app = fakeAppEntry("com.example.app")
        val sampleDrawable = ColorDrawable(Color.RED)
        every { appsProvider.getIcon(app) } returns sampleDrawable

        val prefs = PrefsManager.getInstance(context)
        prefs.setIconSource(PrefsManager.IconSource.SYSTEM)

        val icon1 = manager.getIcon(app)
        val icon2 = manager.getIcon(app)

        assertThat(icon1).isNotNull()
        assertThat(icon2).isNotNull()
        // Verify appsProvider.getIcon is only called once due to caching
        verify(exactly = 1) { appsProvider.getIcon(app) }
    }

    @Test
    fun `getIcon with monochrome source generates fallback when app lacks native monochrome`() {
        val app = fakeAppEntry("com.example.app")
        val sampleDrawable = ColorDrawable(Color.BLUE)
        every { appsProvider.getIcon(app) } returns sampleDrawable

        val prefs = PrefsManager.getInstance(context)
        prefs.setIconSource(PrefsManager.IconSource.MONOCHROME)

        val monochromeIcon = manager.getIcon(app)
        val cachedIcon = manager.getIcon(app)

        assertThat(monochromeIcon).isNotNull()
        assertThat(cachedIcon).isNotNull()
        verify(exactly = 1) { appsProvider.getIcon(app) }
    }

    @Test
    fun `getIcon with monochrome source invalidates cache when theme changes`() {
        val app = fakeAppEntry("com.example.app")
        val sampleDrawable = ColorDrawable(Color.BLUE)
        every { appsProvider.getIcon(app) } returns sampleDrawable

        val prefs = PrefsManager.getInstance(context)
        prefs.setIconSource(PrefsManager.IconSource.MONOCHROME)

        prefs.setTheme(com.rama.bohio.objects.PrefTheme.DRACULA)
        manager.getIcon(app)

        prefs.setTheme(com.rama.bohio.objects.PrefTheme.MONO_LIGHT)
        manager.getIcon(app)

        verify(exactly = 2) { appsProvider.getIcon(app) }
    }

    private fun fakeAppEntry(packageName: String): AppsProvider.AppEntry =
        AppsProvider.ActivityEntry(
            packageName = packageName,
            label = packageName,
            userHandle = Process.myUserHandle(),
            activityInfo = mockk<LauncherActivityInfo>(relaxed = true),
            profileInitial = null
        )
}
