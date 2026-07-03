package com.rama.mako.managers

import android.content.Context
import com.rama.bohio.util.IdUtils

class GroupsManager(
    context: Context,
    private val appsProvider: AppsProvider
) {

    private val prefs = PrefsManager.getInstance(context)

    // ------------------- Groups -------------------

    fun getGroupIds(): List<String> {
        val ids = prefs.getGroupIds().toMutableSet()

        // Recover orphaned groups from stored metadata.
        var repaired = false
        prefs.getAllKeys()
            .asSequence()
            .filter { it.startsWith("group:") && it.endsWith(":label") }
            .map { it.removePrefix("group:").removeSuffix(":label") }
            .forEach { id ->
                if (ids.add(id)) {
                    repaired = true
                }
            }

        if (repaired) {
            prefs.setGroupIds(ids)
        }

        val needsOrder = ids.filter { !prefs.hasGroupOrder(it) }

        if (needsOrder.isNotEmpty()) {
            val maxExisting = ids
                .filter { prefs.hasGroupOrder(it) }
                .maxOfOrNull { prefs.getGroupOrder(it) }
                ?: -1

            needsOrder
                .sortedBy { prefs.getGroupLabel(it).lowercase() }
                .forEachIndexed { i, id ->
                    prefs.setGroupOrder(id, maxExisting + 1 + i)
                }
        }

        return ids.sortedBy { prefs.getGroupOrder(it) }
    }

    fun createGroup(baseLabel: String): String {
        val id = IdUtils.toBase36Fixed(System.currentTimeMillis())

        val label = generateUniqueLabel(baseLabel)

        val nextOrder = prefs.getGroupIds()
            .maxOfOrNull { prefs.getGroupOrder(it) }
            ?.let { if (it == Int.MAX_VALUE) 0 else it + 1 }
            ?: 0

        prefs.addGroupId(id)
        prefs.setGroupLabel(id, label)
        prefs.setGroupVisible(id, true)
        prefs.setGroupExpanded(id, true)
        prefs.setGroupOrder(id, nextOrder)

        return id
    }

    fun deleteGroup(groupId: String, newGroupId: String?) {
        val allApps = appsProvider.getAll()
        allApps.forEach { app ->
            if (prefs.getAppGroupId(app.packageName, app.userHandle) == groupId) {
                prefs.setAppGroupId(app.packageName, app.userHandle, newGroupId)
            }
        }

        prefs.removeGroupId(groupId)
        reindexOrder()
    }

    fun moveGroup(groupId: String, direction: Int) {
        val ordered = getGroupIds().toMutableList()
        val idx = ordered.indexOf(groupId)
        val swapIdx = idx + direction

        if (swapIdx !in ordered.indices) return

        val swapId = ordered[swapIdx]
        val thisOrder = prefs.getGroupOrder(groupId)
        val swapOrder = prefs.getGroupOrder(swapId)

        prefs.setGroupOrder(groupId, swapOrder)
        prefs.setGroupOrder(swapId, thisOrder)
    }

    // ------------------- Label logic -------------------

    private fun generateUniqueLabel(base: String): String {
        val existing = prefs.getGroupIds()
            .map { prefs.getGroupLabel(it).trim().lowercase() }

        var label = base
        var counter = 1

        while (existing.contains(label.trim().lowercase())) {
            counter++
            label = "$base $counter"
        }

        return label
    }

    private fun reindexOrder() {
        getGroupIds().forEachIndexed { index, id ->
            prefs.setGroupOrder(id, index)
        }
    }
}