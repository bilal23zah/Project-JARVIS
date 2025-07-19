package com.you.projectjarvis // Aap yahan "you" ki jagah apna naam likh sakte hain

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var micButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize our views from the layout file
        logTextView = findViewById(R.id.textView_log)
        micButton = findViewById(R.id.button_mic)

        // Set a welcome message
        logTextView.text = "Systems online. All circuits are operational. Welcome back, Sir."

        // Set a listener for the microphone button
        micButton.setOnClickListener {
            // For now, we will just log a message.
            // Later, this will start the voice recognition.
            logTextView.append("\n\nListening...")
        }
    }
}
