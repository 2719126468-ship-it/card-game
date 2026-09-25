package com.example.hellocard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val sounds = mutableMapOf<String, Int>()
    private var bgmPlayer: MediaPlayer? = null
    private var currentBgm = ""

    fun loadAll() {
        sounds["draw"]    = soundPool.load(context, R.raw.draw, 1)
        sounds["summon"]  = soundPool.load(context, R.raw.summon, 1)
        sounds["attack"]  = soundPool.load(context, R.raw.attack, 1)
        sounds["destroy"] = soundPool.load(context, R.raw.destroy, 1)
        sounds["effect"]  = soundPool.load(context, R.raw.effect, 1)
        sounds["damage"]  = soundPool.load(context, R.raw.damage, 1)
        sounds["heal"]    = soundPool.load(context, R.raw.heal, 1)
        sounds["turn"]    = soundPool.load(context, R.raw.turn, 1)
        sounds["win"]     = soundPool.load(context, R.raw.win, 1)
        sounds["lose"]    = soundPool.load(context, R.raw.lose, 1)
        sounds["click"]   = soundPool.load(context, R.raw.click, 1)
        sounds["error"]   = soundPool.load(context, R.raw.error, 1)
    }

    fun play(name: String, volume: Float = 1f) {
        val id = sounds[name] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun playBgm(name: String) {
        if (currentBgm == name && bgmPlayer?.isPlaying == true) return
        stopBgm()
        val resId = when (name) {
            "menu"   -> R.raw.bgm_menu
            "battle" -> R.raw.bgm_battle
            "win"    -> R.raw.bgm_win
            else -> return
        }
        bgmPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(0.4f, 0.4f)
            start()
        }
        currentBgm = name
    }

    fun stopBgm() {
        bgmPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        bgmPlayer = null
        currentBgm = ""
    }

    fun pauseBgm() { bgmPlayer?.takeIf { it.isPlaying }?.pause() }
    fun resumeBgm() { bgmPlayer?.takeIf { !it.isPlaying }?.start() }

    fun release() {
        stopBgm()
        soundPool.release()
    }
}
