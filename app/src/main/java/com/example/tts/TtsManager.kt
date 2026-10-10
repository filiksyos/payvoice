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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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
    private var isInitializing = false
    private val pendingQueue = mutableListOf<Triple<String, AppPreferences, () -> Unit>>()
    private val speechMutex = Mutex()
    private val pendingUtterances = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private var speechGeneration = 0

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
        if (isInitializing || isInitialized) return
        isInitializing = true
        try {
            tts?.shutdown()
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
            handleInitializationFailure()
        }
    }

    override fun onInit(status: Int) {
        // Post the callback so even a synchronous initialization failure is handled
        // after the TextToSpeech constructor has assigned the engine instance.
        scope.launch {
            if (!isInitializing) return@launch
            finishInitialization(status)
        }
    }

    private fun finishInitialization(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: run {
                handleInitializationFailure()
                return
            }
            var result = engine.setLanguage(Locale.US)
            if (result < TextToSpeech.LANG_AVAILABLE) {
                result = engine.setLanguage(Locale.getDefault())
            }
            if (result < TextToSpeech.LANG_AVAILABLE) {
                Log.e(TAG, "TTS has no usable voice data. Enable a speech engine and install voice data in Android text-to-speech settings.")
                handleInitializationFailure()
                return
            }

            setupUtteranceListener()
            isInitializing = false
            isInitialized = true
            Log.i(TAG, "TTS initialized successfully")

            // Process any items queued while initializing
            val queuedItems = synchronized(pendingQueue) {
                pendingQueue.toList().also { pendingQueue.clear() }
            }
            for (item in queuedItems) {
                speakInternal(item.first, item.second, item.third)
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status $status. Check that a speech engine is installed and enabled in Android text-to-speech settings.")
            handleInitializationFailure()
        }
    }

    private fun handleInitializationFailure() {
        isInitializing = false
        isInitialized = false
        tts?.shutdown()
        tts = null
        val queuedItems = synchronized(pendingQueue) {
            pendingQueue.toList().also { pendingQueue.clear() }
        }
        // Release resources held by callers waiting for speech (such as wake locks).
        for (item in queuedItems) {
            item.third()
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.i(TAG, "Speech started: $utteranceId")
                scope.launch {
                    if (pendingUtterances.containsKey(utteranceId)) _isSpeaking.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
                Log.i(TAG, "Speech completed: $utteranceId")
                finishUtterance(utteranceId, true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                finishUtterance(utteranceId, false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                finishUtterance(utteranceId, false)
                Log.e(TAG, "Utterance error: $errorCode for id: $utteranceId")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                finishUtterance(utteranceId, false)
            }
        })
    }

    private fun finishUtterance(utteranceId: String?, successful: Boolean) {
        scope.launch {
            pendingUtterances[utteranceId]?.complete(successful)
        }
    }

    fun speak(text: String, preferences: AppPreferences, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            Log.i(TAG, "Speech queued while TTS initializes")
            synchronized(pendingQueue) {
                pendingQueue.add(Triple(text, preferences, onComplete))
            }
            initTts()
            return
        }

        speakInternal(text, preferences, onComplete)
    }

    private fun speakInternal(text: String, preferences: AppPreferences, onComplete: () -> Unit) {
        val generation = speechGeneration
        scope.launch {
            try {
                speechMutex.withLock {
                    if (generation != speechGeneration) return@withLock
                    _lastSpokenText.value = text
                    prepareVolumeAndFocus(preferences)

                    try {
                        tts?.setSpeechRate(preferences.speechRate)
                        tts?.setPitch(preferences.speechPitch)
                        val repeat = preferences.repeatCount.coerceIn(1, 3)

                        for (i in 1..repeat) {
                            if (generation != speechGeneration) break
                            val speechPhrase = if (i > 1) "Repeating: $text" else text
                            val utteranceId = UUID.randomUUID().toString()
                            val completion = CompletableDeferred<Boolean>()
                            pendingUtterances[utteranceId] = completion
                            val params = Bundle().apply {
                                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                            }

                            try {
                                val result = tts?.speak(speechPhrase, TextToSpeech.QUEUE_ADD, params, utteranceId)
                                    ?: TextToSpeech.ERROR
                                if (result != TextToSpeech.SUCCESS) {
                                    Log.e(TAG, "Speech rejected by engine: status $result, id $utteranceId")
                                    break
                                }
                                Log.i(TAG, "Speech accepted by engine: $utteranceId")

                                // Keep the caller's wake lock until playback actually ends.
                                val successful = withTimeoutOrNull(45_000L) { completion.await() }
                                if (successful != true) {
                                    Log.w(TAG, "Speech stopped, failed, or timed out: $utteranceId")
                                    if (successful == null) tts?.stop()
                                    break
                                }
                            } finally {
                                pendingUtterances.remove(utteranceId)
                            }

                            if (i < repeat) delay(800)
                        }
                    } finally {
                        _isSpeaking.value = false
                        restoreVolumeIfNeeded()
                    }
                }
            } finally {
                onComplete()
            }
        }
    }

    fun stop() {
        speechGeneration++
        pendingUtterances.values.forEach { it.complete(false) }
        val queuedItems = synchronized(pendingQueue) {
            pendingQueue.toList().also { pendingQueue.clear() }
        }
        queuedItems.forEach { it.third() }
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
                    .build()
                val focusResult = audioManager.requestAudioFocus(focusRequest)
                Log.i(TAG, "Audio focus request result: $focusResult")
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
        stop()
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isInitializing = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
