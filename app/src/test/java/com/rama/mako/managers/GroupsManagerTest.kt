package com.rama.mako.managers

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupsManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appsProvider = mockk<AppsProvider>()
    private lateinit var manager: GroupsManager

    @Before
    fun setUp() {
        resetCompanionField(PrefsManager::class.java, "INSTANCE")
        resetCompanionField(com.rama.bohio.managers.PrefsManager::class.java, "instance")

        every { appsProvider.getAll() } returns emptyList()
        manager = GroupsManager(context, appsProvider)
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
    fun `first group keeps the requested label`() {
        val id = manager.createGroup("Work")

        val label = PrefsManager.getInstance(context).getGroupLabel(id)

        assertThat(label).isEqualTo("Work")
    }

    @Test
    fun `duplicate label gets a numeric suffix`() {
        manager.createGroup("Work")
        val secondId = manager.createGroup("Work")

        val label = PrefsManager.getInstance(context).getGroupLabel(secondId)

        assertThat(label).isEqualTo("Work 2")
    }

    @Test
    fun `three duplicate labels increment the suffix each time`() {
        manager.createGroup("Work")
        manager.createGroup("Work")
        val thirdId = manager.createGroup("Work")

        val label = PrefsManager.getInstance(context).getGroupLabel(thirdId)

        assertThat(label).isEqualTo("Work 3")
    }

    @Test
    fun `new groups are appended after existing order`() {
        val firstId = manager.createGroup("A")
        val secondId = manager.createGroup("B")

        val prefs = PrefsManager.getInstance(context)
        assertThat(prefs.getGroupOrder(secondId)).isGreaterThan(prefs.getGroupOrder(firstId))
    }

    @Test
    fun `getGroupIds returns groups sorted by order`() {
        val a = manager.createGroup("A")
        val b = manager.createGroup("B")
        val c = manager.createGroup("C")

        assertThat(manager.getGroupIds()).containsExactly(a, b, c).inOrder()
    }

    @Test
    fun `moveGroup swaps order with its neighbor`() {
        val a = manager.createGroup("A")
        val b = manager.createGroup("B")

        manager.moveGroup(a, direction = 1) // move "A" down past "B"

        assertThat(manager.getGroupIds()).containsExactly(b, a).inOrder()
    }

    @Test
    fun `moveGroup past the last position is a no-op`() {
        val a = manager.createGroup("A")
        val b = manager.createGroup("B")

        manager.moveGroup(b, direction = 1) // "B" is already last

        assertThat(manager.getGroupIds()).containsExactly(a, b).inOrder()
    }

    @Test
    fun `deleteGroup reassigns member apps to the fallback group`() {
        val oldGroup = manager.createGroup("Old")
        val newGroup = manager.createGroup("New")

        val app = fakeAppEntry(packageName = "com.example.app")
        every { appsProvider.getAll() } returns listOf(app)

        val prefs = PrefsManager.getInstance(context)
        prefs.setAppGroupId(app.packageName, app.userHandle, oldGroup)

        manager.deleteGroup(oldGroup, newGroupId = newGroup)

        assertThat(prefs.getAppGroupId(app.packageName, app.userHandle)).isEqualTo(newGroup)
        assertThat(manager.getGroupIds()).doesNotContain(oldGroup)
    }

    @Test
    fun `deleteGroup reindexes order with no gaps`() {
        val a = manager.createGroup("A")
        val b = manager.createGroup("B")
        val c = manager.createGroup("C")

        manager.deleteGroup(b, newGroupId = null)

        val prefs = PrefsManager.getInstance(context)
        val orders = listOf(a, c).map { prefs.getGroupOrder(it) }.sorted()
        assertThat(orders).isEqualTo(listOf(0, 1))
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
