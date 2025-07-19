package com.you.projectjarvis // Make sure this package name is correct

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var settingsButton: ImageView
    private lateinit var speechRecognizer: SpeechRecognizer

    // Activity Result Launcher for microphone permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                logTextView.append("\n\nMicrophone permission granted. Ready to receive commands.")
            } else {
                logTextView.append("\n\nMicrophone permission denied. Voice commands will not be available.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        logTextView = findViewById(R.id.textView_log)
        micButton = findViewById(R.id.button_mic)
        settingsButton = findViewById(R.id.button_settings)

        // Setup listeners
        settingsButton.setOnClickListener { showApiKeyDialog() }
        micButton.setOnClickListener { startListening() }
        
        // Setup Speech Recognizer
        setupSpeechRecognizer()

        // Check for API key and permissions on startup
        checkInitialState()
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                logTextView.append("\n\nListening...")
            }
            override fun onResults(results: Bundle?) {
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                if (!spokenText.isNullOrEmpty()) {
                    logTextView.append("\n\nYOU: $spokenText")
                    getResponseFromAI(spokenText)
                }
            }
            override fun onError(error: Int) { 
                val errorMessage = when(error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found. Please try again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
                    SpeechRecognizer.ERROR_SERVER -> "Error from server."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input."
                    else -> "An unknown speech error occurred."
                }
                logTextView.append("\n\n[ERROR] $errorMessage")
            }
            // Other listener methods can be left empty
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        // Check for microphone permission first
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is granted, start listening
                speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
            }
            else -> {
                // Permission is not granted, request it
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun getResponseFromAI(query: String) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            logTextView.append("\n\n[CRITICAL] API Key not set. Cannot contact the AI.")
            return
        }

        logTextView.append("\n\nJ.A.R.V.I.S.: Thinking...")

        lifecycleScope.launch {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-pro",
                    apiKey = apiKey
                )
                val response = generativeModel.generateContent(query)
                // Using \r to overwrite the "Thinking..." message
                logTextView.append("\rJ.A.R.V.I.S.: ${response.text}                ")
            } catch (e: Exception) {
                logTextView.append("\rJ.A.R.V.I.S.: [ERROR] ${e.message}            ")
            }
        }
    }

    private fun checkInitialState() {
        logTextView.text = "Systems online. Welcome back, Sir."
        if (getApiKey() == null) {
            logTextView.append("\n\nNOTICE: API Key not found. Please add it via the settings icon.")
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            logTextView.append("\n\nNOTICE: Microphone access required for voice commands.")
        }
    }

    // --- API Key Dialog and Storage Functions (from previous step) ---
    private fun showApiKeyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set API Key")
        val input = EditText(this).apply { 
            hint = "Paste your Google AI API Key here"
            maxLines = 1
        }
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            val key = input.text.toString()
            if (key.trim().isNotEmpty()) {
                saveApiKey(key.trim())
                Toast.makeText(this, "API Key saved successfully.", Toast.LENGTH_SHORT).show()
                logTextView.text = "API Key has been configured. I am ready, Sir."
            } else {
                Toast.makeText(this, "API Key cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun saveApiKey(apiKey: String) {
        getSharedPreferences("ProjectJARVISPrefs", Context.MODE_PRIVATE).edit().putString("API_KEY", apiKey).apply()
    }

    private fun getApiKey(): String? {
        return getSharedPreferences("ProjectJARVISPrefs", Context.MODE_PRIVATE).getString("API_KEY", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // It's crucial to release the speech recognizer to prevent memory leaks
        speechRecognizer.destroy()
    }
}
