package com.rama.mako.widgets

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import com.rama.mako.R
import com.rama.bohio.R as BohioR

class WdPinField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pinField: EditText
    private val toggleBtn: FrameLayout
    private val toggleIcon: ImageView
    private val saveBtn: FrameLayout

    private var isVisible = false

    // Callback for save action
    var onPinSaved: ((String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.wd_pin_field, this, true)

        pinField = findViewById(R.id.pin_field)
        toggleBtn = findViewById(R.id.toggle_visibility)
        toggleIcon = findViewById(R.id.toggle_visibility_img)
        saveBtn = findViewById(R.id.save_changes)

        pinField.isSaveEnabled = false
        pinField.id = generateViewId()

        setupToggle()
        setupSave()
    }

    private fun setupToggle() {
        toggleBtn.setOnClickListener {
            isVisible = !isVisible

            if (isVisible) {
                pinField.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_VARIATION_NORMAL
                toggleIcon.setImageResource(BohioR.drawable.px_eye_cross)
            } else {
                pinField.inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_VARIATION_PASSWORD
                toggleIcon.setImageResource(BohioR.drawable.px_eye)
            }

            // Keep cursor at end after toggling
            pinField.setSelection(pinField.text.length)
        }
    }

    private fun setupSave() {
        saveBtn.setOnClickListener {
            val pin = pinField.text.toString()

            // Trigger callback
            onPinSaved?.invoke(pin)
        }
    }

    // Optional helper to get PIN externally
    fun getPin(): String = pinField.text.toString()

    // Optional helper to set PIN externally
    fun setPin(value: String) {
        pinField.setText(value)
    }
}