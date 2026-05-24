package com.rama.mako.activities.settings

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.generateViewId
import android.view.ViewGroup
import android.widget.*
import com.rama.mako.R
import com.rama.mako.activities.SettingsActivity
import com.rama.mako.managers.ThemeManager
import com.rama.mako.utils.SettingsUiUtils

class SettingsGroupsController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs
    private val groupsManager get() = activity.groupsManager

    fun setup() {
        val container = activity.findViewById<LinearLayout>(R.id.groups)

        fun render() {
            container.removeAllViews()
            groupsManager.getGroupIds().forEach { id ->
                val label = prefs.getGroupLabel(id)
                addGroupRow(id, label, container)
            }
        }

        render()

        SettingsUiUtils.setClickWithHaptics(activity.findViewById(R.id.add_group)) {
            groupsManager.createGroup(activity.getString(R.string.new_group_header))
            render()
        }
    }

    private fun addGroupRow(groupId: String, groupLabel: String, container: LinearLayout) {
        val row = activity.layoutInflater.inflate(R.layout.list_item_group, container, false)
        ThemeManager.applyTheme(activity, row)

        val name = row.findViewById<EditText>(R.id.group_name)
        val delete = row.findViewById<FrameLayout>(R.id.delete_group)
        val toggle = row.findViewById<FrameLayout>(R.id.toggle_visibility)
        val toggleIcon = row.findViewById<ImageView>(R.id.toggle_visibility_img)
        val saveButton = row.findViewById<FrameLayout>(R.id.save_changes_button)
        val ascend = row.findViewById<FrameLayout>(R.id.ascend_group)
        val descend = row.findViewById<FrameLayout>(R.id.descend_group)

        name.setText(groupLabel)
        name.tag = groupId
        name.setSaveEnabled(false)
        name.id = generateViewId()

        fun updateIcon() {
            toggleIcon.setImageResource(
                if (prefs.isGroupVisible(groupId)) R.drawable.icon_eye
                else R.drawable.icon_eye_cross
            )
        }

        updateIcon()

        SettingsUiUtils.setClickWithHaptics(toggle) {
            val newValue = !prefs.isGroupVisible(groupId)
            prefs.setGroupVisible(groupId, newValue)
            updateIcon()
        }

        val originalText = name.text.toString()

        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString() ?: ""
                saveButton.visibility =
                    if (currentText != originalText && currentText.isNotBlank()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        SettingsUiUtils.setClickWithHaptics(saveButton) {
            val newLabel = name.text.toString().trim()
            if (newLabel.isNotEmpty()) {
                val id = name.tag as String
                prefs.setGroupLabel(id, newLabel)

                Toast.makeText(
                    activity,
                    activity.getString(R.string.toast_group_label_updated),
                    Toast.LENGTH_SHORT
                ).show()

                (container.parent as View).post {
                    setup()
                }
            }
            saveButton.visibility = View.GONE
        }

        SettingsUiUtils.setClickWithHaptics(delete) {
            val dialogView = activity.layoutInflater.inflate(R.layout.dialog_groups_delete, null)

            val dialog = android.app.Dialog(activity).apply {
                setContentView(dialogView)
                setCancelable(true)
            }

            val yes = dialogView.findViewById<Button>(R.id.yes_button)
            val no = dialogView.findViewById<Button>(R.id.no_button)
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.groups)

            val currentGroupId = name.tag as String
            val targetGroups = groupsManager.getGroupIds().filter { it != currentGroupId }
            var selectedGroupId: String? = null

            val radioIdToGroupId = mutableMapOf<Int, String>()

            targetGroups.forEachIndexed { index, targetId ->
                val radio = RadioButton(activity).apply {
                    id = View.generateViewId()
                    text = prefs.getGroupLabel(targetId)
                }

                radioIdToGroupId[radio.id] = targetId
                radioGroup.addView(radio)

                if (index == 0) {
                    radio.isChecked = true
                    selectedGroupId = targetId
                }
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedGroupId = radioIdToGroupId[checkedId]
            }

            SettingsUiUtils.setClickWithHaptics(yes) {
                if (selectedGroupId == null) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_select_target_group),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    groupsManager.deleteGroup(currentGroupId, selectedGroupId!!)
                    container.removeView(row)
                    dialog.dismiss()
                }
            }

            SettingsUiUtils.setClickWithHaptics(no) { dialog.dismiss() }

            ThemeManager.applyTheme(activity, dialogView)
            dialog.show()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        SettingsUiUtils.setClickWithHaptics(ascend) {
            groupsManager.moveGroup(groupId, -1)
            (container.parent as View).post { setup() }
        }

        SettingsUiUtils.setClickWithHaptics(descend) {
            groupsManager.moveGroup(groupId, +1)
            (container.parent as View).post { setup() }
        }

        container.addView(row)
    }
}