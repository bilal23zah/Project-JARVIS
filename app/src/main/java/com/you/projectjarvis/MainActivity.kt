package com.you.projectjarvis // Make sure this package name is correct

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var logTextView: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var settingsButton: ImageView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

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
        
        // Setup Speech Recognizer and Text-to-Speech
        setupSpeechRecognizer()
        tts = TextToSpeech(this, this)

        // Check for API key and permissions on startup
        checkInitialState()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // CHANGE HERE: Requesting a British English voice.
            val result = tts.setLanguage(Locale.UK) 
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                logTextView.append("\n\n[TTS_ERROR] The desired British accent is not available. Falling back to default.")
                // Fallback to US English if UK is not available
                tts.setLanguage(Locale.US)
            } else {
                 logTextView.append("\n\n[TTS_INFO] British voice module online.")
            }
        } else {
            logTextView.append("\n\n[TTS_ERROR] Voice module initialization failed.")
        }
    }
    
    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { logTextView.append("\n\nListening...") }
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
                    SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't quite catch that, Sir. Please try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything, Sir."
                    else -> "A speech recognition error occurred."
                }
                logTextView.append("\n\n[J.A.R.V.I.S.] $errorMessage")
                speak(errorMessage)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun getResponseFromAI(query: String) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            val errorMsg = "[CRITICAL] API Key not set. Cannot contact the AI."
            logTextView.append("\n\n$errorMsg")
            speak("Sir, my cognitive functions are offline. Please set the API key.")
            return
        }

        logTextView.append("\n\nJ.A.R.V.I.S.: Thinking...")

        lifecycleScope.launch {
            try {
                val generativeModel = GenerativeModel(modelName = "gemini-pro", apiKey = apiKey)
                val response = generativeModel.generateContent(query)
                val responseText = response.text ?: "I am unable to provide a response at this moment."
                logTextView.append("\rJ.A.R.V.I.S.: $responseText                ")
                speak(responseText)
            } catch (e: Exception) {
                val errorMsg = "I seem to have encountered a processing error, Sir."
                logTextView.append("\rJ.A.R.V.I.S.: $errorMsg            ")
                speak(errorMsg)
            }
        }
    }

    private fun checkInitialState() {
        logTextView.text = "Systems online. Welcome back, Sir."
        if (getApiKey() == null) { logTextView.append("\n\nNOTICE: API Key not found.") }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            logTextView.append("\n\nNOTICE: Microphone access required.")
        }
    }

    private fun showApiKeyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set API Key")
        val input = EditText(this).apply { hint = "Paste your Google AI API Key here"; maxLines = 1 }
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            val key = input.text.toString()
            if (key.trim().isNotEmpty()) {
                saveApiKey(key.trim())
                Toast.makeText(this, "API Key saved successfully.", Toast.LENGTH_SHORT).show()
                logTextView.text = "API Key has been configured. I am ready, Sir."
                speak("Cognitive module calibrated. I am online and ready.")
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
        speechRecognizer.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
