package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "PayVoice_TtsManager"

        @Volatile
        private var instance: TtsManager? = null

        fun getInstance(context: Context): TtsManager {
            return instance ?: synchronized(this) {
                instance ?: TtsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<Triple<String, AppPreferences, () -> Unit>>()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousVolume: Int? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            setupUtteranceListener()
            isInitialized = true
            Log.d(TAG, "TTS initialized successfully")

            // Process any items queued while initializing
            synchronized(pendingQueue) {
                while (pendingQueue.isNotEmpty()) {
                    val item = pendingQueue.removeAt(0)
                    speakInternal(item.first, item.second, item.third)
                }
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status $status")
            isInitialized = false
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                restoreVolumeIfNeeded()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                restoreVolumeIfNeeded()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                restoreVolumeIfNeeded()
                Log.e(TAG, "Utterance error: $errorCode for id: $utteranceId")
            }
        })
    }

    fun speak(text: String, preferences: AppPreferences, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            synchronized(pendingQueue) {
                pendingQueue.add(Triple(text, preferences, onComplete))
            }
            initTts()
            return
        }

        speakInternal(text, preferences, onComplete)
    }

    private fun speakInternal(text: String, preferences: AppPreferences, onComplete: () -> Unit) {
        scope.launch {
            _lastSpokenText.value = text
            prepareVolumeAndFocus(preferences)

            tts?.setSpeechRate(preferences.speechRate)
            tts?.setPitch(preferences.speechPitch)

            val repeat = preferences.repeatCount.coerceIn(1, 3)

            for (i in 1..repeat) {
                val speechPhrase = if (i > 1) {
                    "Repeating: $text"
                } else {
                    text
                }

                val utteranceId = UUID.randomUUID().toString()
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }

                tts?.speak(speechPhrase, TextToSpeech.QUEUE_ADD, params, utteranceId)

                if (i < repeat) {
                    // Slight gap between repeats
                    delay(800)
                }
            }
            onComplete()
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            restoreVolumeIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    private fun prepareVolumeAndFocus(preferences: AppPreferences) {
        try {
            // Request audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }

            if (preferences.maxVolumeOverride) {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < (maxVol * 0.85).toInt()) {
                    previousVolume = currentVol
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring volume or audio focus", e)
        }
    }

    private fun restoreVolumeIfNeeded() {
        try {
            val prev = previousVolume
            if (prev != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prev, 0)
                previousVolume = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring volume", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
