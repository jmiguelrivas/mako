package com.rama.mako.activities.settings

import android.util.TypedValue
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.rama.bohio.objects.PrefLanguage
import com.rama.mako.R
import com.rama.bohio.R as BohioR
import com.rama.mako.activities.SettingsActivity

class SettingsLanguageController(private val activity: SettingsActivity) {

    private val prefs get() = activity.prefs

    fun setup() {
        val group = activity.findViewById<RadioGroup>(R.id.language_group)
        val codes = activity.resources.getStringArray(R.array.supported_language_codes)
        val labels = activity.resources.getStringArray(R.array.supported_language_labels)
        require(codes.size == labels.size) {
            "supported_language_codes (${codes.size}) and supported_language_labels (${labels.size}) must have the same length"
        }
        val currentLanguage = prefs.getAppLanguage()

        val codeToId = mutableMapOf<String, Int>()

        codes.zip(labels).forEachIndexed { index, (code, label) ->
            val rb = RadioButton(activity).apply {
                id = View.generateViewId()
                text = label
                textSize = 16f
                setTextColor(ContextCompat.getColor(activity, BohioR.color.foreground))
                val params = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
                if (index < codes.size - 1) {
                    val marginBottomPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 8f, resources.displayMetrics
                    ).toInt()
                    params.bottomMargin = marginBottomPx
                }
                layoutParams = params
            }
            codeToId[code] = rb.id
            group.addView(rb)
        }

        codeToId[currentLanguage]?.let { group.check(it) }

        group.setOnCheckedChangeListener { _, checkedId ->
            val language = codeToId.entries
                .firstOrNull { it.value == checkedId }?.key
                ?: PrefLanguage.SYSTEM

            if (language == prefs.getAppLanguage()) {
                return@setOnCheckedChangeListener
            }

            prefs.setAppLanguage(language)
            activity.recreate()
        }
    }
}