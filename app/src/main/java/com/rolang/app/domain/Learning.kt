package com.rolang.app.domain

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class Language(val code: String, val label: String) {
    INDONESIAN("id", "Indonesia"),
    FILIPINO("fil", "Tagalog");

    companion object {
        /** Accept the legacy Android code while all new records use ISO 639-2 `fil`. */
        fun from(code: String) =
            when (code.lowercase(Locale.ROOT)) {
                "fil",
                "tl" -> FILIPINO
                else -> INDONESIAN
            }
    }
}

data class LanguagePair(val reference: Language, val target: Language)

/** Only this course is exposed today; the pair model remains ready for future courses. */
object Courses {
    val INDONESIAN_TO_FILIPINO = LanguagePair(Language.INDONESIAN, Language.FILIPINO)
    val exposed = listOf(INDONESIAN_TO_FILIPINO)
}

enum class Level(val label: String) {
    BEGINNER("Pemula · A1"),
    INTERMEDIATE("Menengah · B1–B2"),
    ADVANCED("Mahir · C1–C2"),
}

enum class PracticeMode(val label: String) {
    RECOGNITION("Mengenali"),
    RECALL("Tagalog → Indonesia"),
    REVERSE("Indonesia → Tagalog"),
    CONTEXT("Melengkapi kalimat"),
    GRAMMAR("Latihan tata bahasa"),
}

data class WordToken(val text: String, val start: Int, val end: Int)

object Words {
    private val pattern = Regex("[\\p{L}\\p{M}]+(?:['’-][\\p{L}\\p{M}]+)*")

    fun tokens(text: String) =
        pattern
            .findAll(text)
            .map { WordToken(it.value, it.range.first, it.range.last + 1) }
            .toList()

    fun key(word: String) = word.lowercase(Locale.ROOT).replace('’', '\'')

    fun answer(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .replace(Regex("\\s+"), " ")

    fun matches(response: String, translation: String) =
        response.isNotBlank() && translation.split(";", "/").any { answer(response) == answer(it) }
}

enum class Rating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

enum class VocabularyStatus(val label: String) {
    NEW("Baru"),
    LEARNING("Dipelajari"),
    ALMOST_MASTERED("Hampir dikuasai"),
    KNOWN("Dikuasai");

    companion object {
        fun fromLegacy(mastered: Boolean, repetitions: Int) =
            when {
                mastered -> KNOWN
                repetitions >= 3 -> ALMOST_MASTERED
                else -> LEARNING
            }
    }
}

data class Schedule(
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val dueAt: Long = 0,
    val lapses: Int = 0,
    val mastered: Boolean = false,
)

/** A deterministic SM-2-inspired schedule. Again returns in ten minutes. */
object SpacedRepetition {
    const val DAY = 86_400_000L

    fun statusAfterReview(old: VocabularyStatus, next: Schedule, rating: Rating): VocabularyStatus =
        when {
            next.mastered -> VocabularyStatus.KNOWN
            rating == Rating.AGAIN || rating == Rating.HARD -> VocabularyStatus.LEARNING
            next.repetitions >= 3 || old == VocabularyStatus.ALMOST_MASTERED ->
                VocabularyStatus.ALMOST_MASTERED
            else -> VocabularyStatus.LEARNING
        }

    fun next(old: Schedule, rating: Rating, now: Long): Schedule {
        if (rating == Rating.AGAIN)
            return old.copy(
                repetitions = 0,
                intervalDays = 0,
                ease = max(1.3, old.ease - 0.2),
                dueAt = now + 600_000,
                lapses = old.lapses + 1,
                mastered = false,
            )
        val repetitions = old.repetitions + 1
        val ease =
            max(
                1.3,
                old.ease +
                    when (rating) {
                        Rating.HARD -> -0.15
                        Rating.EASY -> 0.15
                        else -> 0.0
                    },
            )
        val days =
            when {
                rating == Rating.HARD -> max(1, (old.intervalDays * 1.2).roundToInt())
                old.repetitions == 0 -> if (rating == Rating.EASY) 4 else 1
                old.repetitions == 1 -> if (rating == Rating.EASY) 8 else 6
                else ->
                    max(
                        old.intervalDays + 1,
                        (old.intervalDays * ease * if (rating == Rating.EASY) 1.3 else 1.0)
                            .roundToInt(),
                    )
            }.coerceAtMost(365)
        return Schedule(
            repetitions,
            days,
            ease,
            now + days * DAY,
            old.lapses,
            repetitions >= 5 && days >= 21 && rating != Rating.HARD,
        )
    }
}
