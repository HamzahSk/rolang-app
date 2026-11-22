"""Normalize and validate Ro-Lang's Indonesian-to-Filipino offline corpus."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
ANDROID_CORPUS = ROOT / "app/src/main/assets/corpus.json"
SERVER_CORPUS = ROOT / "rolang-server/data/corpus.json"
SAMPLE_STORIES = ROOT / "rolang-server/data/sample-stories.json"
REFERENCE = "id"
TARGET = "fil"
LOCALIZED = (REFERENCE, TARGET)
TOKEN = re.compile(r"[^\W\d_]+(?:['’-][^\W\d_]+)*", re.UNICODE)
REFERENCE_TEXT_REPLACEMENTS = {
    "Nama orang. Nama tetap sama dalam ketiga bahasa ini.":
        "Nama orang. Nama tetap sama dalam bahasa Indonesia dan Tagalog.",
    "28-letter alphabet": "Alfabet 28 huruf",
    "Natural greeting": "Salam yang alami",
    "Inclusive and exclusive we": "Kita inklusif dan kami eksklusif",
    "Question particle ba": "Partikel tanya ba",
    "Two number systems": "Dua sistem bilangan",
    "Verb aspect": "Aspek verba",
    "Possessives": "Bentuk kepemilikan",
    "May, mayroon, and wala": "May, mayroon, dan wala",
    "Aspect, not tense": "Aspek, bukan kala",
    "Three negatives and a linker": "Tiga bentuk negasi dan satu penghubung",
    "Po and opo": "Po dan opo",
    "Nasa and papunta sa": "Nasa dan papunta sa",
}
TARGET_TEXT_REPLACEMENTS = {
    "28-letter alphabet": "Alpabetong may 28 titik",
    "Alfabet 28 huruf": "Alpabetong may 28 titik",
    "Natural greeting": "Likas na pagbati",
    "Salam yang alami": "Likas na pagbati",
    "Inclusive and exclusive we": "Inklusibong tayo at eksklusibong kami",
    "Kita inklusif dan kami eksklusif": "Inklusibong tayo at eksklusibong kami",
    "Question particle ba": "Katagang pananong na ba",
    "Partikel tanya ba": "Katagang pananong na ba",
    "Two number systems": "Dalawang sistema ng bilang",
    "Dua sistem bilangan": "Dalawang sistema ng bilang",
    "Verb aspect": "Aspekto ng pandiwa",
    "Aspek verba": "Aspekto ng pandiwa",
    "Possessives": "Mga anyong paari",
    "Bentuk kepemilikan": "Mga anyong paari",
    "May, mayroon, and wala": "May, mayroon, at wala",
    "May, mayroon, dan wala": "May, mayroon, at wala",
    "Aspect, not tense": "Aspekto, hindi panahunan",
    "Aspek, bukan kala": "Aspekto, hindi panahunan",
    "Three negatives and a linker": "Tatlong anyo ng negasyon at isang pang-angkop",
    "Tiga bentuk negasi dan satu penghubung": "Tatlong anyo ng negasyon at isang pang-angkop",
    "Po and opo": "Po at opo",
    "Po dan opo": "Po at opo",
    "Nasa and papunta sa": "Nasa at papunta sa",
    "Nasa dan papunta sa": "Nasa at papunta sa",
    "Pola dasar": "Batayang padron",
    "Po untuk menghormati": "Po bilang paggalang",
    "Ang dan ng": "Ang at ng",
    "Sa untuk tujuan": "Sa para sa patutunguhan",
    "Ng setelah jumlah": "Ng pagkatapos ng bilang",
    "Kay untuk orang": "Kay para sa tao",
    "Dahil untuk alasan": "Dahil para sa dahilan",
    "Wala untuk ketiadaan": "Wala para sa kawalan",
    "Sa untuk tempat": "Sa para sa lugar",
    "Sina untuk beberapa nama": "Sina para sa maraming pangalan",
    "Ba untuk pertanyaan": "Ba para sa tanong",
    "Si untuk nama orang": "Si para sa pangalan ng tao",
    "Hindi untuk penyangkalan": "Hindi para sa pagtanggi",
    "Petunjuk arah": "Mga direksiyon",
}


def canonical_language(value: str) -> str:
    return TARGET if value in {"tl", TARGET} else value


def normalize(value: Any, language: str | None = None) -> Any:
    """Drop the obsolete localization channel and rename the former Tagalog code."""
    if isinstance(value, list):
        return [normalize(item, language) for item in value]
    if isinstance(value, str):
        replacements = REFERENCE_TEXT_REPLACEMENTS if language == REFERENCE else \
            TARGET_TEXT_REPLACEMENTS if language == TARGET else {}
        return replacements.get(value, value)
    if not isinstance(value, dict):
        return value
    result: dict[str, Any] = {}
    for key, child in value.items():
        if key == "en":
            continue
        normalized_key = TARGET if key == "tl" else key
        child_language = normalized_key if normalized_key in LOCALIZED else language
        normalized_child = normalize(child, child_language)
        if normalized_key == "language" and isinstance(normalized_child, str):
            normalized_child = canonical_language(normalized_child)
        if normalized_key not in result or key == TARGET:
            result[normalized_key] = normalized_child
    return result


def localized_text(value: dict[str, Any], field: str) -> dict[str, str]:
    item = value.get(field)
    if not isinstance(item, dict):
        raise ValueError(f"{field} must be a localized object")
    for code in LOCALIZED:
        if not isinstance(item.get(code), str) or not item[code].strip():
            raise ValueError(f"{field}.{code} must be nonempty")
    return item


def speech_text(unit: dict[str, Any], exercise: dict[str, Any], index: int) -> str:
    answer = localized_text(exercise, "answer")[TARGET]
    if exercise["type"] == "FILL_BLANK" and unit.get("sentences"):
        return unit["sentences"][0][TARGET]
    if exercise["type"] == "SENTENCE_CONSTRUCTION":
        return answer
    if exercise["type"] in {"MULTIPLE_CHOICE", "VOCABULARY_TRANSLATION"}:
        focus = unit.get("focus", {}).get(TARGET, [])
        if focus:
            return focus[0]
    if exercise["type"] == "COMPREHENSION" and unit.get("sentences"):
        return unit["sentences"][min(index, len(unit["sentences"]) - 1)][TARGET]
    return answer


def learning_units(story: dict[str, Any]):
    yield story
    yield from story.get("chapters", [])


def normalize_story(raw: dict[str, Any]) -> dict[str, Any]:
    story = normalize(raw)
    for unit in learning_units(story):
        for index, exercise in enumerate(unit.get("exercises", [])):
            exercise["speech"] = {TARGET: speech_text(unit, exercise, index)}
    return story


def normalize_lexicon(words: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result: dict[tuple[str, str], dict[str, Any]] = {}
    for raw in words:
        if canonical_language(raw.get("language", "")) != TARGET:
            continue
        word = normalize(raw)
        word["language"] = TARGET
        word["word"] = word["word"].lower().replace("’", "'")
        result[(TARGET, word["word"])] = word
    return [result[key] for key in sorted(result)]


def validate_exercise(exercise: dict[str, Any]) -> None:
    localized_text(exercise, "prompt")
    localized_text(exercise, "answer")
    localized_text(exercise, "explanation")
    if set(exercise.get("speech", {})) != {TARGET} or not exercise["speech"][TARGET].strip():
        raise ValueError(f"Exercise {exercise.get('id')} needs Filipino speech text")
    if exercise["type"] in {"MULTIPLE_CHOICE", "GRAMMAR"}:
        for code in LOCALIZED:
            choices = exercise.get("choices", {}).get(code, [])
            if len(choices) < 2 or exercise["answer"][code] not in choices:
                raise ValueError(f"Exercise {exercise['id']} has invalid {code} choices")
    if exercise["type"] == "SENTENCE_CONSTRUCTION":
        for code in LOCALIZED:
            if not exercise.get("tokens", {}).get(code):
                raise ValueError(f"Exercise {exercise['id']} needs {code} tokens")


def validate(corpus: dict[str, Any]) -> None:
    stories = corpus.get("stories", [])
    words = corpus.get("lexicon", [])
    if not stories or len({story["id"] for story in stories}) != len(stories):
        raise ValueError("Corpus needs unique stories")
    if [story.get("order") for story in stories] != list(range(1, len(stories) + 1)):
        raise ValueError("Story order must be contiguous")
    if any(word.get("language") != TARGET for word in words):
        raise ValueError("Only Filipino dictionary entries may be bundled")
    keys = set()
    for word in words:
        key = (word["language"], word["word"])
        if key in keys:
            raise ValueError(f"Duplicate dictionary entry: {key}")
        keys.add(key)
        for field in ("translations", "meanings", "notes"):
            localized_text(word, field)
    for story in stories:
        localized_text(story, "title")
        localized_text(story, "summary")
        for unit in learning_units(story):
            sentences = unit.get("sentences", [])
            if not 1 <= len(sentences) <= 300:
                raise ValueError(f"{unit['id']} has an invalid sentence count")
            for sentence in sentences:
                for code in LOCALIZED:
                    if not isinstance(sentence.get(code), str) or not sentence[code].strip():
                        raise ValueError(f"{unit['id']} has a missing {code} sentence")
                for token in TOKEN.findall(sentence[TARGET]):
                    if (TARGET, token.lower().replace("’", "'")) not in keys:
                        raise ValueError(f"Missing Filipino dictionary entry for {token} in {unit['id']}")
            for expression in unit.get("focus", {}).get(TARGET, []):
                if (TARGET, expression.lower().replace("’", "'")) not in keys:
                    raise ValueError(f"Missing Filipino focus entry for {expression} in {unit['id']}")
            for exercise in unit.get("exercises", []):
                validate_exercise(exercise)
    serialized = json.dumps(corpus, ensure_ascii=False)
    if '"en"' in serialized or '"tl"' in serialized:
        raise ValueError("Legacy language keys remain in the corpus")


def normalize_corpus(raw: dict[str, Any]) -> dict[str, Any]:
    corpus = {
        "version": max(4, int(raw.get("version", 0))),
        "course": "Tagalog A1 untuk penutur bahasa Indonesia",
        "referenceLanguage": REFERENCE,
        "targetLanguage": TARGET,
        "stories": [normalize_story(story) for story in raw["stories"]],
        "lexicon": normalize_lexicon(raw["lexicon"]),
    }
    validate(corpus)
    return corpus


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    corpus = normalize_corpus(json.loads(ANDROID_CORPUS.read_text(encoding="utf-8")))
    write_json(ANDROID_CORPUS, corpus)
    if SERVER_CORPUS.parent.is_dir():
        write_json(SERVER_CORPUS, corpus)
    if SAMPLE_STORIES.is_file():
        samples = json.loads(SAMPLE_STORIES.read_text(encoding="utf-8"))
        cleaned = []
        for raw in samples:
            story = normalize_story(raw)
            story["vocabulary"] = normalize_lexicon(raw.get("vocabulary", []))
            cleaned.append(story)
        write_json(SAMPLE_STORIES, cleaned)
    print(f"Validated {len(corpus['stories'])} units and {len(corpus['lexicon'])} Filipino dictionary entries.")


if __name__ == "__main__":
    main()
