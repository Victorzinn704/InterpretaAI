package br.gov.interpretaai.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import br.gov.interpretaai.R

enum class SoundCue { TAP, SWAP, DISCOVERY, CELEBRATE }

class InteractionSounds(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()
    ).build()
    private val ids = mapOf(
        SoundCue.TAP to pool.load(context, R.raw.cue_tap, 1),
        SoundCue.SWAP to pool.load(context, R.raw.cue_swap, 1),
        SoundCue.DISCOVERY to pool.load(context, R.raw.cue_discovery, 1),
        SoundCue.CELEBRATE to pool.load(context, R.raw.cue_celebrate, 1)
    )

    fun play(cue: SoundCue, reducedStimuli: Boolean) {
        if (!reducedStimuli) ids[cue]?.let { pool.play(it, .45f, .45f, 1, 0, 1f) }
    }

    fun release() = pool.release()
}
