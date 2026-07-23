package us.jmresearch.abcflashcards.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

/**
 * All sound in one place: TTS fallback, parent-recorded clips, and recording.
 * Recordings live in filesDir/recordings/<itemId>.m4a.
 */
class AudioBox(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    fun recordingFile(itemId: String): File = File(recordingsDir, "$itemId.m4a")

    fun hasRecording(itemId: String): Boolean = recordingFile(itemId).exists()

    /** Play the parent recording if it exists, else speak the fallback text. */
    fun play(itemId: String, fallbackText: String) {
        stopPlayback()
        val file = recordingFile(itemId)
        if (file.exists()) {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); if (player == it) player = null }
                prepare()
                start()
            }
        } else if (ttsReady) {
            tts?.speak(fallbackText, TextToSpeech.QUEUE_FLUSH, null, itemId)
        }
    }

    fun startRecording(itemId: String): Boolean {
        stopRecording()
        stopPlayback()
        return try {
            @Suppress("DEPRECATION")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile(itemId).absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            recordingFile(itemId).delete()
            false
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // stop() throws if nothing was captured; discard silently
        }
        recorder?.release()
        recorder = null
    }

    fun deleteRecording(itemId: String) {
        stopPlayback()
        recordingFile(itemId).delete()
    }

    private fun stopPlayback() {
        player?.release()
        player = null
    }

    fun shutdown() {
        stopRecording()
        stopPlayback()
        tts?.shutdown()
        tts = null
    }
}
