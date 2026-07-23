package us.jmresearch.abcflashcards

import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.util.Log


class MainActivity : AppCompatActivity() {

    private lateinit var randomLetterTextView: TextView
    private lateinit var skipLetterButton: Button
    private lateinit var resetButton: Button
    private lateinit var skippedLettersTextView: TextView
    private var currentLetter: Char = ' '
    private val allLetters = ('A'..'Z').toList() + ('a'..'z').toList()
    private val skippedLetters = mutableSetOf<Char>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        randomLetterTextView = findViewById(R.id.randomLetterTextView)
        skipLetterButton = findViewById(R.id.skipLetterButton)
        resetButton = findViewById(R.id.resetButton)
        skippedLettersTextView = findViewById(R.id.skippedLettersTextView)

        skipLetterButton.setOnClickListener {
            skipCurrentLetter()
            updateSkippedLettersTextView()
        }

        // Set click listener on resetButton to reset skipped letters
        resetButton.setOnClickListener {
            resetSkippedLetters()
            updateSkippedLettersTextView()
        }

        // Set initial random letter
        generateRandomLetter()

        // Set click listener to generate new random letter on tap
        randomLetterTextView.setOnClickListener {
            generateRandomLetter()
        }
    }
    private fun generateRandomLetter() {
        // Generate a random letter (A-Z or a-z)
        val remainingLetters = allLetters.filterNot { skippedLetters.contains(it) }
        if (remainingLetters.isEmpty()) {
            randomLetterTextView.text = "All Done!"
            return
        }
        val randomChar = remainingLetters.random()

        currentLetter = randomChar
        setTextSizeBasedOnScreen()
        randomLetterTextView.text = "$currentLetter ☜"
    }

    private fun resetSkippedLetters() {
        skippedLetters.clear()
        Log.d("MainActivity", "Skipped letters reset")
    }

    private fun skipCurrentLetter() {
        Log.d("MainActivity", "Skipping letter: $currentLetter")
        val currentChar = randomLetterTextView.text.toString().first()
        skippedLetters.add(currentChar)
        generateRandomLetter()
    }

    private fun updateSkippedLettersTextView() {
        val skippedLettersString = "Skipped Letters: ${skippedLetters.sorted().joinToString(", ")}"
        skippedLettersTextView.text = skippedLettersString
    }

    private fun setTextSizeBasedOnScreen() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        // Determine appropriate text size based on screen dimensions
        val textSize = screenWidth.coerceAtMost(screenHeight) / 10 // Adjust the divisor (e.g., / 10) for desired text scaling
        randomLetterTextView.textSize = textSize
    }
}