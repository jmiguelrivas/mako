package com.rama.mako.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rama.mako.managers.AppsProvider
import com.rama.mako.managers.GroupsManager

class AppUninstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                val pendingResult = goAsync()
                Thread {
                    try {
                        val appsProvider = AppsProvider(context)
                        val groupsManager = GroupsManager(context, appsProvider)
                        groupsManager.healData()
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }
}
