package com.example

import android.content.Context
import android.media.AudioManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.tts.TtsManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TtsManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = AppPreferences(maxVolumeOverride = false)
    private lateinit var manager: TtsManager

    @After
    fun tearDown() {
        if (::manager.isInitialized) manager.shutdown()
    }

    @Test
    fun requestsDuringInitializationReuseTheEngine() {
        manager = TtsManager(context)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()

        manager.speak("First payment", preferences)
        manager.speak("Second payment", preferences)

        assertSame(engine, ShadowTextToSpeech.getLastTextToSpeechInstance())
    }

    @Test
    fun failedInitializationReleasesPendingRequestsAndAllowsRetry() {
        manager = TtsManager(context)
        val failedEngine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        var completed = 0
        manager.speak("Payment", preferences) { completed++ }

        shadowOf(failedEngine).onInitListener.onInit(1)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, completed)
        assertTrue(shadowOf(failedEngine).isShutdown)

        manager.speak("Next payment", preferences)
        assertNotSame(failedEngine, ShadowTextToSpeech.getLastTextToSpeechInstance())
    }

    @Test
    fun successfulInitializationSpeaksQueuedPayments() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.US)
        manager = TtsManager(context)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        manager.speak("Received 500 Birr", preferences)

        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Received 500 Birr", shadowOf(engine).lastSpokenText)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertNotNull(shadowOf(audioManager).lastAudioFocusRequest)
    }

    @Test
    fun completionWaitsForPlaybackRatherThanSubmission() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.US)
        manager = TtsManager(context)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS)
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idle()
        var completed = 0

        manager.speak("Received 10 Birr", preferences) { completed++ }
        mainLooper.runOneTask()
        assertEquals(0, completed)

        mainLooper.idle()
        assertEquals(1, completed)
    }

    @Test
    fun stopCancelsQueuedSpeechAndReleasesCallers() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.US)
        manager = TtsManager(context)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS)
        val mainLooper = shadowOf(Looper.getMainLooper())
        mainLooper.idle()
        var completed = 0
        manager.speak("First payment", preferences) { completed++ }
        manager.speak("Second payment", preferences) { completed++ }

        manager.stop()
        mainLooper.idle()

        assertEquals(2, completed)
        assertEquals(null, shadowOf(engine).lastSpokenText)
    }

    @Test
    fun missingVoiceDataReleasesPendingRequests() {
        manager = TtsManager(context)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        var completed = 0
        manager.speak("Payment", preferences) { completed++ }

        shadowOf(engine).onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, completed)
        assertTrue(shadowOf(engine).isShutdown)
    }
}
