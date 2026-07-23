package us.jmresearch.abcflashcards.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val SAMPLE_RATE = 22050

/**
 * All music and sound effects are synthesized — no audio assets to ship.
 * Music is a gentle looping pentatonic melody; effects are short tone runs.
 * Volumes are 0..1 and applied live.
 */
class SoundBox {

    private var musicTrack: AudioTrack? = null
    private var musicVolume = 0.5f
    private var sfxVolume = 0.7f
    private val liveEffects = mutableListOf<AudioTrack>()

    // --- synthesis helpers ---

    private fun tone(freq: Double, ms: Int, amplitude: Double = 0.5): ShortArray {
        val n = SAMPLE_RATE * ms / 1000
        return ShortArray(n) { i ->
            // soft attack/decay envelope so notes don't click
            val t = i.toDouble() / n
            val env = min(1.0, min(t * 12, (1 - t) * 4)).coerceAtLeast(0.0)
            (sin(2 * PI * freq * i / SAMPLE_RATE) * env * amplitude * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun silence(ms: Int) = ShortArray(SAMPLE_RATE * ms / 1000)

    private fun concat(parts: List<ShortArray>): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        parts.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }

    private fun note(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    private fun buildTrack(pcm: ShortArray, loop: Boolean): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        if (loop) track.setLoopPoints(0, pcm.size, -1)
        return track
    }

    // --- music ---

    /** Gentle C-pentatonic lullaby-ish loop, ~9 seconds. */
    private fun buildMelody(): ShortArray {
        // C5 D5 E5 G5 A5 relative to A4: 3, 5, 7, 10, 12; octave down for bass feel
        val seq = listOf(
            3 to 350, 7 to 350, 10 to 350, 12 to 500, 10 to 350, 7 to 350,
            5 to 500, 3 to 350, 5 to 350, 7 to 700, -9 to 350, 3 to 350,
            7 to 350, 10 to 500, 7 to 350, 5 to 350, 3 to 700, -9 to 500,
        )
        val parts = mutableListOf<ShortArray>()
        seq.forEach { (semi, ms) ->
            parts.add(tone(note(semi), ms, amplitude = 0.18))
            parts.add(silence(60))
        }
        return concat(parts)
    }

    fun startMusic() {
        if (musicTrack != null) return
        musicTrack = buildTrack(buildMelody(), loop = true).also {
            it.setVolume(musicVolume)
            if (musicVolume > 0f) it.play()
        }
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        musicTrack?.let { track ->
            track.setVolume(musicVolume)
            if (musicVolume == 0f && track.playState == AudioTrack.PLAYSTATE_PLAYING) track.pause()
            if (musicVolume > 0f && track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
        }
    }

    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
    }

    // --- effects ---

    private fun playEffect(pcm: ShortArray) {
        if (sfxVolume <= 0f) return
        liveEffects.removeAll { t ->
            (t.playState != AudioTrack.PLAYSTATE_PLAYING).also { done -> if (done) t.release() }
        }
        val track = buildTrack(pcm, loop = false)
        track.setVolume(sfxVolume)
        liveEffects.add(track)
        track.play()
    }

    /** Two quick rising notes. */
    fun correct() = playEffect(concat(listOf(tone(note(3), 110), tone(note(10), 160))))

    /** One soft low boop — gentle, not punishing. */
    fun wrong() = playEffect(tone(note(-14), 220, amplitude = 0.35))

    /** Four-note fanfare for stars. */
    fun fanfare() = playEffect(
        concat(listOf(tone(note(3), 130), tone(note(7), 130), tone(note(10), 130), tone(note(15), 320))),
    )

    fun shutdown() {
        musicTrack?.release()
        musicTrack = null
        liveEffects.forEach { it.release() }
        liveEffects.clear()
    }
}
