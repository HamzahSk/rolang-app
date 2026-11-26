package com.rolang.app.speech

import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FilipinoTtsManagerTest {
    private val locale = Locale("fil", "PH")

    @Before fun resetTts() = ShadowTextToSpeech.reset()

    @After fun cleanUpTts() = ShadowTextToSpeech.reset()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun finishInitialization(status: Int = TextToSpeech.SUCCESS) {
        idle()
        shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).onInitListener.onInit(status)
        idle()
    }

    private fun voice(name: String, language: String = "fil", network: Boolean = false) =
        Voice(
            name,
            Locale(language, if (language == "fil") "PH" else "US"),
            400,
            200,
            network,
            emptySet(),
        )

    @Test
    fun cNativeFilipinoVoiceSupportsPlayStopReplaySequenceAndSettings() {
        ShadowTextToSpeech.addLanguageAvailability(locale)
        ShadowTextToSpeech.addVoice(voice("fil-offline"))
        val manager = FilipinoTtsManager(ApplicationProvider.getApplicationContext<Context>())
        finishInitialization()

        assertEquals(PronunciationAvailability.READY, manager.state.value.availability)
        assertEquals("fil-offline", manager.state.value.voiceName)
        val engine = ShadowTextToSpeech.getLastTextToSpeechInstance()
        val shadow = shadowOf(engine)

        manager.speak("Salamat po.", "word")
        idle()
        assertEquals("Salamat po.", shadow.lastSpokenText)
        assertEquals(TextToSpeech.QUEUE_FLUSH, shadow.queueMode)
        assertFalse(
            manager.state.value.speaking
        ) // Robolectric completes each utterance immediately.

        manager.stop()
        idle()
        assertTrue(shadow.isStopped)
        assertFalse(manager.state.value.speaking)
        manager.replay()
        idle()
        assertEquals("Salamat po.", shadow.lastSpokenText)

        manager.speakSequence(
            "story",
            listOf(SpeechItem("line-1", "Kumusta ka?"), SpeechItem("line-2", "Mabuti ako.")),
        )
        idle()
        assertEquals(listOf("Kumusta ka?", "Mabuti ako."), shadow.spokenTextList.takeLast(2))
        assertFalse(manager.state.value.speaking)

        manager.setSpeechRate(0.2f)
        manager.setPitch(2f)
        idle()
        assertEquals(0.5f, manager.state.value.speechRate)
        assertEquals(1.2f, manager.state.value.pitch)
        manager.close()
        idle()
        assertTrue(shadow.isShutdown)
        assertEquals(PronunciationAvailability.CLOSED, manager.state.value.availability)
    }

    @Test
    fun bFailedNativeInitializationReportsErrorAndDoesNotSpeak() {
        val manager = FilipinoTtsManager(ApplicationProvider.getApplicationContext<Context>())
        finishInitialization(TextToSpeech.ERROR)

        assertEquals(PronunciationAvailability.ERROR, manager.state.value.availability)
        assertNull(manager.state.value.voiceName)
        manager.speak("Salamat", "word")
        idle()
        assertFalse(manager.state.value.speaking)
        assertNotEquals(
            "Salamat",
            shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).lastSpokenText,
        )
        manager.close()
    }

    @Test
    fun aUnavailableFilipinoLanguageIsReportedAsUnsupported() {
        val manager = FilipinoTtsManager(ApplicationProvider.getApplicationContext<Context>())
        finishInitialization()
        assertEquals(PronunciationAvailability.UNSUPPORTED, manager.state.value.availability)
        assertTrue(manager.state.value.message.contains("tidak mendukung"))
        manager.close()
    }
}
