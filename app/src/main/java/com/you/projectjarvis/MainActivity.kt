package com.you.projectjarvis // Make sure this package name is correct

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ai.client.generativeai.GenerativeModel

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var settingsButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize our views
        logTextView = findViewById(R.id.textView_log)
        micButton = findViewById(R.id.button_mic)
        settingsButton = findViewById(R.id.button_settings)

        // Set initial welcome message
        logTextView.text = "Systems online. Welcome back, Sir. Please set your API Key in settings."

        // Set listener for the microphone button
        micButton.setOnClickListener {
            startVoiceRecognition()
        }

        // Set listener for the settings button
        settingsButton.setOnClickListener {
            showApiKeyDialog()
        }

        // Check if API key exists, if not, prompt the user.
        if (getApiKey() == null) {
            logTextView.append("\n\nNOTICE: API Key not found. Please add it via the settings icon.")
        }
    }

    private fun startVoiceRecognition() {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "API Key is not set. Please set it in settings.", Toast.LENGTH_LONG).show()
            logTextView.append("\n\n[ERROR] Cannot proceed without a valid API Key.")
            return
        }

        // This is where the voice recognition and AI call will happen.
        // For now, it's a placeholder.
        logTextView.append("\n\nListening... (Functionality to be added)")
        // --- Placeholder for AI logic ---
        // val generativeModel = GenerativeModel(modelName = "gemini-pro", apiKey = apiKey)
        // ... rest of the AI code will go here ...
    }

    private fun showApiKeyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set API Key")

        val input = EditText(this)
        input.hint = "Paste your Google AI API Key here"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val key = input.text.toString()
            if (key.isNotEmpty()) {
                saveApiKey(key)
                Toast.makeText(this, "API Key saved successfully.", Toast.LENGTH_SHORT).show()
                logTextView.text = "API Key has been configured. I am ready, Sir."
            } else {
                Toast.makeText(this, "API Key cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun saveApiKey(apiKey: String) {
        val sharedPref = getSharedPreferences("ProjectJARVISPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("API_KEY", apiKey)
            apply()
        }
    }

    private fun getApiKey(): String? {
        val sharedPref = getSharedPreferences("ProjectJARVISPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("API_KEY", null)
    }
}
