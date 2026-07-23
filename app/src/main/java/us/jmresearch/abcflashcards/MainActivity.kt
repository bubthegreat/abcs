package us.jmresearch.abcflashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import us.jmresearch.abcflashcards.ui.App
import us.jmresearch.abcflashcards.ui.AppViewModel
import us.jmresearch.abcflashcards.ui.AudioBox
import us.jmresearch.abcflashcards.ui.InkBox

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels { AppViewModel.factory(this) }
    private lateinit var audio: AudioBox
    private lateinit var ink: InkBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AudioBox(this)
        ink = InkBox()
        setContent {
            MaterialTheme {
                Surface { App(vm, audio, ink) }
            }
        }
    }

    override fun onDestroy() {
        audio.shutdown()
        ink.close()
        super.onDestroy()
    }
}
