package com.rama.mako.activities.settings

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.rama.bohio.managers.ThemeManager
import com.rama.bohio.R as BohioR
import com.rama.bohio.util.UiActions
import com.rama.bohio.widgets.WdConfirmDialog
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.AppsProvider
import java.util.Locale

class SettingsHiddenAppsController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs
    private val appsProvider get() = activity.appsProvider
    private val iconManager get() = activity.iconManager

    fun setup() {
        UiActions.setupButton(activity, R.id.hidden_apps_button) {
            showHiddenAppsDialog()
        }
    }

    private fun showHiddenAppsDialog() {
        val hiddenApps = getHiddenApps().toMutableList()
        if (hiddenApps.isEmpty()) {
            Toast.makeText(
                activity,
                activity.getString(R.string.toast_no_hidden_apps),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val view = activity.layoutInflater.inflate(R.layout.dialog_hidden_apps, null)
        ThemeManager.applyTheme(activity, view)

        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(true)
            .create()

        val listView = view.findViewById<ListView>(R.id.hidden_app_list)
        val closeBtn = view.findViewById<Button>(R.id.close_button)

        val adapter = object : BaseAdapter() {
            override fun getCount() = hiddenApps.size
            override fun getItem(position: Int) = hiddenApps[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: activity.layoutInflater.inflate(
                    R.layout.list_item_hidden_app,
                    parent,
                    false
                )
                val app = hiddenApps[position]
                view.findViewById<TextView>(R.id.open_app_button).text = getDisplayName(app)
                view.findViewById<ImageView>(R.id.app_icon)
                    .setImageDrawable(iconManager.getIcon(app))

                view.findViewById<View>(R.id.restore_button).setOnClickListener {
                    showRestoreConfirmation(app) {
                        prefs.setAppHidden(app.packageName, app.userHandle, false)
                        hiddenApps.removeAt(position)
                        if (hiddenApps.isEmpty()) {
                            dialog.dismiss()
                        } else {
                            notifyDataSetChanged()
                        }
                    }
                }

                ThemeManager.applyTheme(activity, view)
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            appsProvider.launch(hiddenApps[position])
        }

        UiActions.setClickWithHaptics(closeBtn) { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showRestoreConfirmation(app: AppsProvider.AppEntry, onConfirmed: () -> Unit) {
        val view = WdConfirmDialog(activity)
        ThemeManager.applyTheme(activity, view)

        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .create()

        view.titleView.setText(R.string.h2_restore_app)
        view.messageView.setText(R.string.disclaimer_restore_app)
        view.previewIconView.setImageDrawable(iconManager.getIcon(app))
        view.previewCountView.visibility = View.GONE
        view.previewNamesView.text = getDisplayName(app)
        view.confirmButton.text = activity.getString(R.string.btn_restore)
        view.cancelButton.text = activity.getString(BohioR.string.btn_cancel)

        view.confirmButton.setOnClickListener {
            onConfirmed()
            dialog.dismiss()
        }
        view.cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun getHiddenApps(): List<AppsProvider.AppEntry> {
        return appsProvider.getAll()
            .filter { prefs.isAppHidden(it) }
            .sortedBy { getDisplayName(it).lowercase(Locale.ROOT) }
    }

    private fun getDisplayName(app: AppsProvider.AppEntry): String {
        val baseName = prefs.getCustomName(app.packageName, app.userHandle) ?: app.label
        return if (prefs.hasProfileIndicator() && app.isWorkProfile) {
            "[${app.profileInitial}] $baseName"
        } else {
            baseName
        }
    }
}
