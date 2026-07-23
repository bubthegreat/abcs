package us.jmresearch.abcflashcards.ui

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * Handwriting recognition via ML Kit digital ink. The en-US model (~20MB)
 * downloads once in the background; [modelReady] flips when usable.
 */
class InkBox {

    private val model: DigitalInkRecognitionModel =
        DigitalInkRecognitionModel.builder(
            DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")!!,
        ).build()

    private val recognizer: DigitalInkRecognizer =
        DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())

    private val _modelReady = MutableStateFlow(false)
    val modelReady: StateFlow<Boolean> = _modelReady

    init {
        val manager = RemoteModelManager.getInstance()
        manager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    _modelReady.value = true
                } else {
                    manager.download(model, DownloadConditions.Builder().build())
                        .addOnSuccessListener { _modelReady.value = true }
                }
            }
    }

    /** Best-candidate text for the drawn strokes, or null when nothing recognizable. */
    suspend fun recognize(ink: Ink): String? {
        if (!_modelReady.value || ink.strokes.isEmpty()) return null
        return try {
            recognizer.recognize(ink).await()
                .candidates.firstOrNull()?.text?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        recognizer.close()
    }
}
