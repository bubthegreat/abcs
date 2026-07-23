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

    /** NES-style pulse wave with duty cycle. */
    private fun pulseSample(freq: Double, i: Int, duty: Double): Double {
        val phase = (freq * i / SAMPLE_RATE) % 1.0
        return if (phase < duty) 1.0 else -1.0
    }

    /** NES-style triangle wave (bass channel). */
    private fun triSample(freq: Double, i: Int): Double {
        val phase = (freq * i / SAMPLE_RATE) % 1.0
        return 4.0 * kotlin.math.abs(phase - 0.5) - 1.0
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

    // --- music: 8-bit chiptunes ---
    // A song is a melody line (pulse wave) over a bass line (triangle wave).
    // Notes are (semitone from A4 or null for rest, length in beat units).

    private class Chiptune(
        val name: String,
        val unitMs: Int,
        val duty: Double,
        val melody: List<Pair<Int?, Int>>,
        val bass: List<Pair<Int?, Int>>,
    )

    private val songs = listOf(
        // Bright I-vi-IV-V runner in C.
        Chiptune(
            name = "Sunny Run", unitMs = 110, duty = 0.25,
            melody = listOf(
                3 to 1, 7 to 1, 10 to 1, 15 to 1, 10 to 1, 7 to 1, 3 to 1, 7 to 1,
                0 to 1, 3 to 1, 7 to 1, 12 to 1, 7 to 1, 3 to 1, 0 to 1, 3 to 1,
                1 to 1, 5 to 1, 8 to 1, 13 to 1, 8 to 1, 5 to 1, 1 to 1, 5 to 1,
                5 to 1, 7 to 1, 8 to 1, 10 to 2, 7 to 2, 3 to 2,
                3 to 1, 7 to 1, 10 to 1, 15 to 1, 19 to 2, 15 to 1, 10 to 1,
                17 to 1, 15 to 1, 13 to 1, 12 to 1, 10 to 2, 8 to 1, 7 to 1,
                8 to 1, 10 to 1, 12 to 1, 8 to 1, 5 to 2, 7 to 2,
                3 to 2, null to 1, 3 to 1, 3 to 4,
            ),
            bass = listOf(
                -21 to 4, -21 to 4, -12 to 4, -12 to 4,
                -16 to 4, -16 to 4, -14 to 4, -14 to 4,
                -21 to 4, -21 to 4, -12 to 4, -12 to 4,
                -16 to 4, -14 to 4, -21 to 4, -21 to 4,
            ),
        ),
        // Bouncy arcade tune in A minor.
        Chiptune(
            name = "Pixel Bounce", unitMs = 100, duty = 0.5,
            melody = listOf(
                0 to 1, 3 to 1, 7 to 1, 12 to 1, 10 to 1, 7 to 1, 3 to 1, 7 to 1,
                null to 1, 8 to 1, 12 to 1, 15 to 1, 12 to 1, 8 to 1, 5 to 1, 8 to 1,
                7 to 1, 10 to 1, 14 to 1, 10 to 1, 7 to 1, 4 to 1, 7 to 1, 10 to 1,
                12 to 2, 10 to 1, 8 to 1, 7 to 2, 3 to 2,
                0 to 1, 3 to 1, 7 to 1, 12 to 1, 15 to 2, 12 to 2,
                13 to 1, 12 to 1, 10 to 1, 8 to 1, 7 to 1, 5 to 1, 3 to 1, 1 to 1,
                0 to 2, 7 to 2, 0 to 2, null to 2,
            ),
            bass = listOf(
                -24 to 2, -12 to 2, -24 to 2, -12 to 2,
                -28 to 2, -16 to 2, -28 to 2, -16 to 2,
                -26 to 2, -14 to 2, -26 to 2, -14 to 2,
                -24 to 2, -12 to 2, -29 to 2, -26 to 2,
                -24 to 2, -12 to 2, -28 to 2, -16 to 2,
                -26 to 2, -14 to 2, -24 to 2, -24 to 2,
            ),
        ),
        // Heroic quest theme in G with rests for punch.
        Chiptune(
            name = "Star Quest", unitMs = 130, duty = 0.125,
            melody = listOf(
                10 to 1, null to 1, 10 to 1, 14 to 1, 17 to 2, 14 to 1, 10 to 1,
                8 to 1, null to 1, 8 to 1, 12 to 1, 15 to 2, 12 to 1, 8 to 1,
                7 to 1, 10 to 1, 14 to 1, 19 to 1, 17 to 2, 14 to 2,
                12 to 1, 14 to 1, 15 to 1, 14 to 1, 10 to 2, null to 2,
                10 to 1, 14 to 1, 17 to 1, 22 to 2, 19 to 1, 17 to 1, 14 to 1,
                15 to 1, 12 to 1, 8 to 1, 5 to 1, 7 to 2, 10 to 2,
            ),
            bass = listOf(
                -14 to 4, -14 to 4, -16 to 4, -16 to 4,
                -17 to 4, -12 to 4, -14 to 4, -14 to 4,
                -14 to 4, -19 to 4, -16 to 4, -14 to 4,
            ),
        ),
    )

    private var songIndex = 0

    val currentSongName: String
        get() = songs[songIndex].name

    private fun renderSong(song: Chiptune): ShortArray {
        fun lineSamples(line: List<Pair<Int?, Int>>): Int =
            line.sumOf { it.second } * SAMPLE_RATE * song.unitMs / 1000
        val total = maxOf(lineSamples(song.melody), lineSamples(song.bass))
        val out = ShortArray(total)

        fun renderLine(line: List<Pair<Int?, Int>>, isBass: Boolean) {
            var pos = 0
            // repeat the shorter line until it fills the track
            while (pos < total) {
                for ((semi, units) in line) {
                    val len = units * SAMPLE_RATE * song.unitMs / 1000
                    if (semi != null) {
                        val freq = note(semi)
                        val noteEnd = (pos + len).coerceAtMost(total)
                        for (i in pos until noteEnd) {
                            val t = (i - pos).toDouble() / len
                            val env = min(1.0, min(t * 30, (1 - t) * 6)).coerceAtLeast(0.0)
                            val sample = if (isBass) {
                                triSample(freq, i) * 0.14
                            } else {
                                pulseSample(freq, i, song.duty) * 0.09
                            }
                            val mixed = out[i] + (sample * env * Short.MAX_VALUE).toInt()
                            out[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                    pos += len
                    if (pos >= total) break
                }
            }
        }

        renderLine(song.melody, isBass = false)
        renderLine(song.bass, isBass = true)
        return out
    }

    fun startMusic() {
        if (musicTrack != null) return
        musicTrack = buildTrack(renderSong(songs[songIndex]), loop = true).also {
            it.setVolume(musicVolume)
            if (musicVolume > 0f) it.play()
        }
    }

    /** Switch to the next chiptune, keeping volume and play state. */
    fun nextSong(): String {
        songIndex = (songIndex + 1) % songs.size
        musicTrack?.release()
        musicTrack = null
        startMusic()
        return currentSongName
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
