package com.rolang.app.speech

import android.speech.tts.TextToSpeech
import org.junit.Assert.*
import org.junit.Test

class FilipinoVoiceSelectorTest {
    @Test
    fun `offline Filipino Philippines voice is preferred without an English fallback`() {
        val selected =
            preferredFilipinoVoice(
                listOf(
                    FilipinoVoiceOption("english", "en", "US", false, 500, 100),
                    FilipinoVoiceOption("fil-network", "fil", "PH", true, 500, 100),
                    FilipinoVoiceOption("fil-offline-other", "fil", "", false, 500, 100),
                    FilipinoVoiceOption("fil-offline-ph", "fil", "PH", false, 300, 200),
                )
            )
        assertEquals("fil-offline-ph", selected?.name)
        assertFalse(selected!!.networkRequired)
    }

    @Test
    fun `legacy tl voice is recognized as Filipino but unrelated voices are rejected`() {
        assertTrue(isFilipinoLanguage("tl"))
        assertTrue(isFilipinoLanguage("fil"))
        assertFalse(isFilipinoLanguage("en"))
        assertNull(
            preferredFilipinoVoice(
                listOf(
                    FilipinoVoiceOption("english", "en", "US", false, 500, 100),
                    FilipinoVoiceOption("indonesian", "id", "ID", false, 500, 100),
                )
            )
        )
    }

    @Test
    fun `selected offline Filipino voice is accepted when an engine does not enumerate voices`() {
        val selected = FilipinoVoiceOption("engine-default-fil", "fil", "PH", false, 300, 200)
        assertEquals(selected, verifiedOfflineFilipinoVoice(emptyList(), selected))
        assertNull(verifiedOfflineFilipinoVoice(emptyList(), selected.copy(language = "en")))
        assertNull(verifiedOfflineFilipinoVoice(emptyList(), selected.copy(networkRequired = true)))
    }

    @Test
    fun `Android language results distinguish missing data from unsupported language`() {
        assertEquals(
            PronunciationAvailability.MISSING_DATA,
            availabilityFromLanguageStatus(TextToSpeech.LANG_MISSING_DATA),
        )
        assertEquals(
            PronunciationAvailability.UNSUPPORTED,
            availabilityFromLanguageStatus(TextToSpeech.LANG_NOT_SUPPORTED),
        )
        assertNull(availabilityFromLanguageStatus(TextToSpeech.LANG_COUNTRY_AVAILABLE))
    }
}
