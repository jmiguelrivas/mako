package com.rama.mako.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.rama.mako.CsActivity
import com.rama.mako.R
import com.rama.mako.managers.PrefsManager

class LockActivity : CsActivity() {

    private lateinit var pinDisplay: EditText
    private lateinit var easterEggText: TextView
    private lateinit var easterEggCounter: TextView
    private lateinit var buttons: List<Button>
    private lateinit var unlockButton: Button

    private val pinBuilder = StringBuilder()
    private var unlockPressCount = 0
    private var isEasterEggActive = false

    private val easterEggThreshold = 32
    private val easterEggCounterStart = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_lock)

        val root = findViewById<View>(android.R.id.content)
        applyEdgeToEdgePadding(root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHome()
            }
        })

        setupViews()
        setupActions()
        setupKeypad()
        clearPin()
    }

    private fun setupViews() {
        pinDisplay = findViewById(R.id.pin_display)
        easterEggText = findViewById(R.id.easter_egg_text)
        easterEggCounter = findViewById(R.id.easter_egg_counter)
        unlockButton = findViewById(R.id.unlock_button)

        buttons = listOf(
            findViewById(R.id.btn0),
            findViewById(R.id.btn1),
            findViewById(R.id.btn2),
            findViewById(R.id.btn3),
            findViewById(R.id.btn4),
            findViewById(R.id.btn5),
            findViewById(R.id.btn6),
            findViewById(R.id.btn7),
            findViewById(R.id.btn8),
            findViewById(R.id.btn9),
        )
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun setupKeypad() {
        val isRandomized = prefs.getBoolean(
            PrefsManager.FileKeys.SECURITY_KEYPAD_RANDOMIZED,
            true
        )

        val digits = if (isRandomized) (0..9).shuffled() else (0..9).toList()

        buttons.forEachIndexed { index, button ->
            val digit = digits[index]
            button.text = digit.toString()
            button.setOnClickListener {
                appendDigit(digit)
            }
        }
    }

    private fun appendDigit(digit: Int) {
        if (pinBuilder.length >= 10) return
        pinBuilder.append(digit)
        updateDisplay()
    }

    private fun updateDisplay() {
        pinDisplay.setText("*".repeat(pinBuilder.length))
    }

    private fun clearPin() {
        pinBuilder.clear()
        pinDisplay.setText("")
    }

    private fun setupActions() {
        findViewById<View>(R.id.clear_button).setOnClickListener {
            clearPin()
        }

        findViewById<View>(R.id.unlock_button).setOnClickListener {
            if (!isEasterEggActive) {
                unlockPressCount++
                if (unlockPressCount >= easterEggThreshold) {
                    showEasterEgg()
                } else if (unlockPressCount >= easterEggCounterStart) {
                    showEasterEggCounter()
                    validatePin()
                } else {
                    validatePin()
                }
            }
        }

        findViewById<View>(R.id.close_button).setOnClickListener {
            navigateToHome()
        }
    }

    private fun showEasterEgg() {
        isEasterEggActive = true
        easterEggText.visibility = View.VISIBLE
        easterEggCounter.visibility = View.GONE
        unlockButton.isEnabled = false
    }

    private fun showEasterEggCounter() {
        val remaining = easterEggThreshold - unlockPressCount
        easterEggCounter.text = getString(R.string.easter_eggs_counter, remaining)
        easterEggCounter.visibility = View.VISIBLE
    }

    private fun validatePin() {
        val savedPin = prefs.getPin()

        if (savedPin.isEmpty()) {
            finish()
            return
        }

        if (pinBuilder.toString() == savedPin) {
            setResult(RESULT_OK)
            finish()
        } else {
            clearPin()
            setupKeypad()
        }
    }
}