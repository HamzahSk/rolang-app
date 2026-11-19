"""Author and validate Ro-Lang's complete, offline Tagalog A1 curriculum."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/corpus.json"
LANGS = ("en", "id", "tl")
UNITS = []


def t(en, id_, tl): return {"en": en, "id": id_, "tl": tl}
def n(title, en, id_, tl): return {"title": t(title, title, title), "text": t(en, id_, tl)}
def q(en, id_, tl, a_en, a_id, a_tl):
    return {"type": "COMPREHENSION", "prompt": t(en, id_, tl),
            "answer": t(a_en, a_id, a_tl),
            "explanation": t("The answer is stated in the learning text.",
                             "Jawabannya disebutkan dalam teks pembelajaran.",
                             "Nasa teksto ng aralin ang sagot.")}


def generated_exercises(id_, kind, sentences, focus, note, authored):
    """Build a complete, content-derived practice set for every offline unit."""
    result = []
    for index, item in enumerate(authored):
        result.append({"id": f"{id_}_comprehension_{index + 1}", **item})
    if kind != "LESSON" and not authored:
        result.append({
            "id": f"{id_}_comprehension_1", "type": "COMPREHENSION",
            "prompt": t("What happens first in this story?", "Apa yang terjadi pertama dalam cerita ini?",
                        "Ano ang unang nangyayari sa kuwentong ito?"),
            "answer": t(*sentences[0]),
            "explanation": t("The opening sentence introduces the first event.",
                             "Kalimat pembuka memperkenalkan peristiwa pertama.",
                             "Ipinakikilala ng pambungad na pangungusap ang unang pangyayari.")})

    focus = list(focus)
    if focus:
        correct = focus[0]
        alternatives = (focus[1:] + focus[:1])[:3]
        choices = [correct] + alternatives
        result.append({
            "id": f"{id_}_choice", "type": "MULTIPLE_CHOICE",
            "prompt": t(f'Which option means “{correct[2]}”?', f'Mana arti “{correct[2]}”?',
                        f'Alin ang kahulugan ng “{correct[2]}”?'),
            "choices": {lang: [value[i] for value in choices] for i, lang in enumerate(LANGS)},
            "answer": t(*correct),
            "explanation": t(f'“{correct[2]}” means “{correct[0]}”.',
                             f'“{correct[2]}” berarti “{correct[1]}”.',
                             f'Ang “{correct[2]}” ay nangangahulugang “{correct[2]}”.')})
        result.append({
            "id": f"{id_}_vocabulary", "type": "VOCABULARY_TRANSLATION",
            "prompt": t(f'Type the meaning of “{correct[2]}”.', f'Ketik arti “{correct[2]}”.',
                        f'Isulat ang kahulugan ng “{correct[2]}”.'),
            "answer": t(*correct),
            "explanation": t(f'The focus expression is “{correct[0]}”.',
                             f'Ekspresi fokusnya adalah “{correct[1]}”.',
                             f'Ang pokus na parirala ay “{correct[2]}”.')})

    first = sentences[0]
    blank_answers = []
    blank_prompts = []
    for i, sentence in enumerate(first):
        words = sentence.rstrip(".!?").split()
        answer = words[-1].strip(",:;“”\"")
        blank_answers.append(answer)
        blank_prompts.append(sentence.replace(answer, "____", 1))
    result.append({
        "id": f"{id_}_blank", "type": "FILL_BLANK", "prompt": t(*blank_prompts),
        "answer": t(*blank_answers),
        "explanation": t("The missing word completes the example sentence.",
                         "Kata yang hilang melengkapi kalimat contoh.",
                         "Kinukumpleto ng nawawalang salita ang halimbawang pangungusap.")})

    if kind == "LESSON":
        grammar_title = note["title"] if note else t("Lesson pattern", "Pola pelajaran", "Padron ng aralin")
        result.append({
            "id": f"{id_}_grammar", "type": "GRAMMAR",
            "prompt": t("Which topic explains the main pattern in this lesson?",
                        "Topik mana yang menjelaskan pola utama pelajaran ini?",
                        "Aling paksa ang nagpapaliwanag sa pangunahing padron ng araling ito?"),
            "choices": {
                "en": [grammar_title["en"], "Word order", "Informal spelling"],
                "id": [grammar_title["id"], "Urutan kata", "Ejaan informal"],
                "tl": [grammar_title["tl"], "Ayos ng salita", "Di-pormal na baybay"],
            },
            "answer": grammar_title,
            "explanation": t("This is the grammar or usage focus explained in the lesson.",
                             "Ini adalah fokus tata bahasa atau penggunaan yang dijelaskan dalam pelajaran.",
                             "Ito ang pokus sa balarila o gamit na ipinaliwanag sa aralin.")})
        result.append({
            "id": f"{id_}_sentence", "type": "SENTENCE_CONSTRUCTION",
            "prompt": t("Put the words in order to rebuild the example.",
                        "Susun kata-kata untuk membentuk kembali contoh.",
                        "Ayusin ang mga salita upang mabuo muli ang halimbawa."),
            "answer": t(*first),
            "tokens": {lang: list(reversed(first[i].replace(".", "").replace(",", "").split()))
                       for i, lang in enumerate(LANGS)},
            "explanation": t("This order matches the complete example sentence.",
                             "Urutan ini sesuai dengan kalimat contoh lengkap.",
                             "Tugma ang ayos na ito sa buong halimbawang pangungusap.")})
    return result


def u(id_, kind, family, category, image, title, summary, sentences, focus=(), note=None,
      exercises=(), speakers=(), pronunciation=None):
    order = len(UNITS) + 1
    assert not speakers or len(speakers) == len(sentences)
    UNITS.append({"id": id_, "family": family, "category": category, "level": "BEGINNER",
        "order": order, "kind": kind, "image": image, "title": t(*title), "summary": t(*summary),
        "objectives": [t(f"Use the language in unit {order} independently.",
                         f"Menggunakan materi unit {order} secara mandiri.",
                         f"Gamitin nang mag-isa ang wika sa yunit {order}.")],
        "notes": [note] if note else [], "pronunciation": t(*(pronunciation or ("", "", ""))),
        "focus": {lang: [x[i] for x in focus] for i, lang in enumerate(LANGS)},
        "sentences": [t(*x) for x in sentences], "speakers": list(speakers),
        "exercises": generated_exercises(id_, kind, sentences, focus, note, exercises)})


u("a1_01_sounds", "LESSON", "foundations", "Foundations", "sounds",
  ("Filipino sounds and spelling", "Bunyi dan ejaan Filipino", "Mga tunog at baybay ng Filipino"),
  ("The alphabet, five vowels, stress, and the sound ng.", "Alfabet, lima vokal, tekanan, dan bunyi ng.", "Ang alpabeto, limang patinig, diin, at tunog na ng."), [
  ("Filipino has five clear vowels: a, e, i, o, and u.", "Filipino memiliki lima vokal yang jelas: a, e, i, o, dan u.", "May limang malinaw na patinig ang Filipino: a, e, i, o, at u."),
  ("The letters ng make one sound in the word ngayon.", "Huruf ng menjadi satu bunyi dalam kata ngayon.", "Iisang tunog ang ng sa salitang ngayon."),
  ("Stress can change meaning, so listen and repeat slowly.", "Tekanan dapat mengubah makna, jadi dengarkan dan ulangi perlahan.", "Maaaring magbago ang kahulugan dahil sa diin, kaya makinig at umulit nang dahan-dahan."),
  ("Read aloud: ako, bahay, gabi, pagkain, and salamat.", "Baca dengan lantang: ako, bahay, gabi, pagkain, dan salamat.", "Basahin nang malakas: ako, bahay, gabi, pagkain, at salamat."),
], [("I", "saya", "ako"), ("house", "rumah", "bahay"), ("food", "makanan", "pagkain"), ("thank you", "terima kasih", "salamat")],
  n("28-letter alphabet", "The modern alphabet is A B C D E F G H I J K L M N Ñ NG O P Q R S T U V W X Y Z. Filipino spelling is mostly phonetic.",
    "Alfabet modern terdiri atas A B C D E F G H I J K L M N Ñ NG O P Q R S T U V W X Y Z. Ejaan Filipino sebagian besar fonetis.",
    "Ang makabagong alpabeto ay A B C D E F G H I J K L M N Ñ NG O P Q R S T U V W X Y Z. Karaniwang ayon sa tunog ang baybay."),
  [q("Which letters form one sound in ngayon?", "Huruf apa yang menjadi satu bunyi dalam ngayon?", "Aling mga titik ang iisang tunog sa ngayon?", "Ng.", "Ng.", "Ng.")],
  pronunciation=("A is like a in father; i like ee; u like oo. Ng is /ŋ/ as in singer and may begin a word.",
                 "A seperti a pada apa; i seperti i pada ini; u seperti u pada buku. Ng berbunyi /ŋ/ dan dapat berada di awal kata.",
                 "Ang ng ay /ŋ/ at maaari itong nasa unahan ng salita."))

u("a1_02_greetings", "DIALOGUE", "greetings", "Greetings & introductions", "greetings",
  ("Good morning, neighbor!", "Selamat pagi, tetangga!", "Magandang umaga, kapitbahay!"),
  ("A warm, natural first conversation.", "Percakapan pertama yang hangat dan alami.", "Isang magiliw at natural na unang usapan."), [
  ("Good morning! How are you?", "Selamat pagi! Apa kabar?", "Magandang umaga! Kumusta ka?"),
  ("I'm fine, thank you. And you?", "Saya baik, terima kasih. Kamu?", "Mabuti naman ako, salamat. Ikaw?"),
  ("I'm fine too. My name is Liza.", "Saya juga baik. Nama saya Liza.", "Mabuti rin ako. Liza ang pangalan ko."),
  ("I'm Arif. Nice to meet you.", "Saya Arif. Senang bertemu denganmu.", "Ako si Arif. Ikinagagalak kitang makilala."),
  ("See you later! Take care!", "Sampai nanti! Hati-hati!", "Mamaya ulit! Ingat!"),
], [("good morning", "selamat pagi", "magandang umaga"), ("how are you", "apa kabar", "kumusta ka"),
    ("nice to meet you", "senang bertemu denganmu", "ikinagagalak kitang makilala"), ("take care", "hati-hati", "ingat")],
  n("Natural greeting", "Kumusta is fully Filipino despite its Spanish history. Naman and rin make answers sound conversational.",
    "Kumusta sudah sepenuhnya menjadi kata Filipino. Naman dan rin membuat jawaban terdengar alami.",
    "Ganap nang salitang Filipino ang kumusta. Ginagawang natural ng naman at rin ang sagot."),
  [q("What is Liza's name?", "Siapa nama perempuan itu?", "Ano ang pangalan ng babae?", "Liza.", "Liza.", "Liza.")],
  ("Liza", "Arif", "Liza", "Arif", "Liza"))

u("a1_03_pronouns_questions", "LESSON", "foundations", "Pronouns & questions", "greetings",
  ("Pronouns, sentences, and questions", "Pronomina, kalimat, dan pertanyaan", "Panghalip, pangungusap, at tanong"),
  ("Introduce people and ask six essential questions.", "Perkenalkan orang dan ajukan enam pertanyaan penting.", "Magpakilala at gamitin ang anim na mahalagang tanong."), [
  ("I am Nia, and she is Mara.", "Saya Nia dan dia Mara.", "Ako si Nia, at siya si Mara."),
  ("We are classmates, including you.", "Kita teman sekelas, termasuk kamu.", "Magkaklase tayo, kasama ka."),
  ("We are siblings, but you are not included.", "Kami bersaudara, tetapi kamu tidak termasuk.", "Magkapatid kami, pero hindi ka kasama."),
  ("Who is your teacher? Where is the school?", "Siapa gurumu? Di mana sekolahnya?", "Sino ang guro mo? Saan ang paaralan?"),
  ("What is your name? Why are you happy?", "Siapa namamu? Mengapa kamu senang?", "Ano ang pangalan mo? Bakit masaya ka?"),
  ("When is class, and how do you go there?", "Kapan kelasnya dan bagaimana kamu pergi ke sana?", "Kailan ang klase, at paano ka pumupunta roon?"),
], [("I", "saya", "ako"), ("you", "kamu", "ikaw"), ("he or she", "dia", "siya"),
    ("we including you", "kita", "tayo"), ("we excluding you", "kami", "kami"), ("they", "mereka", "sila")],
  n("Inclusive and exclusive we", "Tayo includes the listener; kami excludes the listener. Siya can mean he or she.",
    "Tayo mencakup lawan bicara; kami tidak. Siya dapat berarti dia laki-laki atau perempuan.",
    "Kasama ang kausap sa tayo; hindi sa kami. Maaaring lalaki o babae ang siya."),
  [q("Which pronoun includes the listener?", "Pronomina mana yang mencakup lawan bicara?", "Aling panghalip ang kasama ang kausap?", "Tayo.", "Tayo.", "Tayo.")])

u("a1_04_meet_joel", "DIALOGUE", "greetings", "Greetings & introductions", "greetings",
  ("Meeting Joel at school", "Bertemu Joel di sekolah", "Pagkilala kay Joel sa paaralan"),
  ("Two students exchange personal information.", "Dua siswa bertukar informasi pribadi.", "Nagpapalitan ng personal na impormasyon ang dalawang mag-aaral."), [
  ("Hi! What's your name?", "Hai! Siapa namamu?", "Hi! Ano ang pangalan mo?"),
  ("My name is Joel. And you?", "Nama saya Joel. Kamu?", "Joel ang pangalan ko. Ikaw?"),
  ("I'm Sari. Where are you from?", "Saya Sari. Kamu berasal dari mana?", "Ako si Sari. Taga-saan ka?"),
  ("I'm from Cebu, but I live in Manila now.", "Saya dari Cebu, tetapi sekarang tinggal di Manila.", "Taga-Cebu ako, pero nakatira ako ngayon sa Maynila."),
  ("Are you a student?", "Apakah kamu seorang siswa?", "Estudyante ka ba?"),
  ("Yes. We are classmates now!", "Ya. Sekarang kita teman sekelas!", "Oo. Magkaklase na tayo!"),
], [("what is your name", "siapa namamu", "ano ang pangalan mo"), ("where are you from", "kamu dari mana", "taga-saan ka"),
    ("student", "siswa", "estudyante"), ("classmates", "teman sekelas", "magkaklase")],
  n("Question particle ba", "Put ba after the first predicate or focused phrase: Estudyante ka ba? Answer with oo or hindi.",
    "Letakkan ba setelah predikat atau frasa fokus pertama: Estudyante ka ba? Jawab dengan oo atau hindi.",
    "Ilagay ang ba pagkatapos ng unang panaguri: Estudyante ka ba? Sagutin ng oo o hindi."),
  [q("Where does Joel live?", "Di mana Joel tinggal?", "Saan nakatira si Joel?", "In Manila.", "Di Manila.", "Sa Maynila.")],
  ("Sari", "Joel", "Sari", "Joel", "Sari", "Joel"))

u("a1_05_numbers_time", "LESSON", "time", "Numbers, dates & time", "time",
  ("Numbers, days, dates, and time", "Angka, hari, tanggal, dan waktu", "Mga bilang, araw, petsa, at oras"),
  ("Count and arrange a meeting.", "Berhitung dan membuat janji.", "Bumilang at magtakda ng pagkikita."), [
  ("I have two brothers and one sister.", "Saya memiliki dua saudara laki-laki dan satu saudara perempuan.", "May dalawa akong kapatid na lalaki at isang kapatid na babae."),
  ("Today is Monday, June three.", "Hari ini Senin, tiga Juni.", "Lunes ngayon, ikatlo ng Hunyo."),
  ("My birthday is on December twelve.", "Ulang tahun saya tanggal dua belas Desember.", "Ang kaarawan ko ay sa ikalabindalawa ng Disyembre."),
  ("It is seven o'clock in the morning.", "Sekarang pukul tujuh pagi.", "Alas-siyete ng umaga ngayon."),
  ("Let's meet on Friday at five in the afternoon.", "Mari bertemu hari Jumat pukul lima sore.", "Magkita tayo sa Biyernes nang alas-singko ng hapon."),
], [("one", "satu", "isa"), ("two", "dua", "dalawa"), ("three", "tiga", "tatlo"),
    ("ten", "sepuluh", "sampu"), ("what time", "pukul berapa", "anong oras")],
  n("Two number systems", "Native forms such as isa, dalawa, and tatlo are common for counting. Spanish-based forms are usual for clock time.",
    "Bentuk asli seperti isa, dalawa, dan tatlo umum untuk berhitung. Bentuk berbasis Spanyol lazim untuk jam.",
    "Karaniwan sa bilang ang isa, dalawa, at tatlo. Karaniwan sa oras ang mga anyong mula sa Espanyol."),
  [q("How do you say seven o'clock?", "Bagaimana mengatakan pukul tujuh?", "Paano sabihin ang ikapitong oras?", "Alas-siyete.", "Alas-siyete.", "Alas-siyete.")])

u("a1_06_morning", "STORY", "routine", "Everyday life", "time",
  ("Lina's busy morning", "Pagi Lina yang sibuk", "Ang abalang umaga ni Lina"),
  ("A student gets ready and reaches school on time.", "Seorang siswa bersiap dan tiba di sekolah tepat waktu.", "Naghahanda ang mag-aaral at dumarating sa paaralan sa tamang oras."), [
  ("Lina wakes up at six in the morning.", "Lina bangun pukul enam pagi.", "Gumigising si Lina nang alas-sais ng umaga."),
  ("She takes a bath, gets dressed, and eats bread with an egg.", "Dia mandi, berpakaian, dan makan roti dengan telur.", "Naliligo siya, nagbibihis, at kumakain ng tinapay na may itlog."),
  ("Her father prepares coffee, but Lina drinks water.", "Ayahnya menyiapkan kopi, tetapi Lina minum air.", "Naghahanda ng kape ang tatay niya, pero umiinom ng tubig si Lina."),
  ("At seven, she leaves the house and rides a bus.", "Pukul tujuh, dia meninggalkan rumah dan naik bus.", "Nang alas-siyete, umaalis siya sa bahay at sumasakay ng bus."),
  ("She arrives before eight and is happy because she is not late.", "Dia tiba sebelum pukul delapan dan senang karena tidak terlambat.", "Dumarating siya bago mag-alas-otso at masaya dahil hindi siya huli."),
], [("wake up", "bangun", "gumising"), ("take a bath", "mandi", "maligo"), ("eat", "makan", "kumain"),
    ("drink", "minum", "uminom"), ("leave", "berangkat", "umalis"), ("arrive", "tiba", "dumating")],
  n("Verb aspect", "Tagalog forms show completed, ongoing, or contemplated action. Time words add calendar time.",
    "Bentuk Tagalog menunjukkan tindakan selesai, berlangsung, atau akan dilakukan. Kata waktu menambahkan waktu kalender.",
    "Ipinapakita ng pandiwa kung naganap, nagaganap, o magaganap ang kilos. Nililinaw ng salitang pamanahon ang oras."),
  [q("Why is Lina happy?", "Mengapa Lina senang?", "Bakit masaya si Lina?", "She is not late.", "Dia tidak terlambat.", "Hindi siya huli.")])

u("a1_07_family", "LESSON", "family", "Family & relationships", "family",
  ("Family and possession", "Keluarga dan kepemilikan", "Pamilya at pagmamay-ari"),
  ("Talk about relatives and ownership.", "Bicarakan kerabat dan kepemilikan.", "Pag-usapan ang kamag-anak at pagmamay-ari."), [
  ("This is my mother, Rosa, and that is your father.", "Ini ibu saya, Rosa, dan itu ayahmu.", "Ito ang nanay ko, si Rosa, at iyan ang tatay mo."),
  ("Her older brother is a nurse.", "Kakak laki-lakinya seorang perawat.", "Nars ang kuya niya."),
  ("Our grandmother lives in the province.", "Nenek kami tinggal di provinsi.", "Nakatira sa probinsiya ang lola namin."),
  ("Their child is kind and cheerful.", "Anak mereka baik dan ceria.", "Mabait at masayahin ang anak nila."),
  ("Whose bag is this? It is Carlo's bag.", "Tas siapa ini? Ini tas Carlo.", "Kaninong bag ito? Bag ito ni Carlo."),
], [("mother", "ibu", "nanay"), ("father", "ayah", "tatay"), ("older brother", "kakak laki-laki", "kuya"),
    ("older sister", "kakak perempuan", "ate"), ("grandmother", "nenek", "lola"), ("grandfather", "kakek", "lolo")],
  n("Possessives", "Use ko, mo, niya, namin, natin, ninyo, and nila after a noun. Natin includes the listener; namin does not.",
    "Gunakan ko, mo, niya, namin, natin, ninyo, dan nila setelah nomina. Natin mencakup lawan bicara; namin tidak.",
    "Gamitin ang ko, mo, niya, namin, natin, ninyo, at nila pagkatapos ng pangngalan. Kasama ang kausap sa natin; hindi sa namin."))

u("a1_08_family_lunch", "STORY", "family", "Family & relationships", "family",
  ("Sunday lunch with the family", "Makan siang Minggu bersama keluarga", "Pananghalian ng pamilya sa Linggo"),
  ("Mara introduces her family to a new friend.", "Mara memperkenalkan keluarganya kepada teman baru.", "Ipinakikilala ni Mara ang pamilya niya sa bagong kaibigan."), [
  ("On Sunday, Mara invites her friend Dito to lunch.", "Pada hari Minggu, Mara mengundang temannya Dito untuk makan siang.", "Sa Linggo, inaanyayahan ni Mara ang kaibigan niyang si Dito sa pananghalian."),
  ("Her mother cooks soup, and her father grills fish.", "Ibunya memasak sup dan ayahnya memanggang ikan.", "Nagluluto ng sabaw ang nanay niya, at nag-iihaw ng isda ang tatay niya."),
  ("Her younger sister sets the table with plates and spoons.", "Adik perempuannya menata meja dengan piring dan sendok.", "Inihahanda ng nakababatang kapatid niya ang mesa na may mga plato at kutsara."),
  ("Everyone eats together and tells funny stories.", "Semua makan bersama dan menceritakan kisah lucu.", "Sabay-sabay kumakain ang lahat at nagkukuwento ng nakakatawang kuwento."),
  ("Before going home, Dito thanks Mara's family.", "Sebelum pulang, Dito berterima kasih kepada keluarga Mara.", "Bago umuwi, nagpapasalamat si Dito sa pamilya ni Mara."),
], [("lunch", "makan siang", "pananghalian"), ("cook", "memasak", "magluto"), ("fish", "ikan", "isda"),
    ("plate", "piring", "plato"), ("spoon", "sendok", "kutsara")], exercises=[q("What does Mara's father cook?", "Apa yang dimasak ayah Mara?", "Ano ang niluluto ng tatay ni Mara?", "Fish.", "Ikan.", "Isda.")])

u("a1_09_objects_school", "LESSON", "school", "Objects & school", "classroom",
  ("Everyday objects and locations", "Benda sehari-hari dan lokasi", "Mga bagay at kinalalagyan"),
  ("Name objects and say where they are.", "Sebutkan benda dan lokasinya.", "Pangalanan ang bagay at sabihin kung nasaan."), [
  ("There is a notebook inside the bag.", "Ada buku catatan di dalam tas.", "May kuwaderno sa loob ng bag."),
  ("The pencil is beside the book.", "Pensil berada di samping buku.", "Nasa tabi ng aklat ang lapis."),
  ("The chair is in front of the desk.", "Kursi berada di depan meja.", "Nasa harap ng mesa ang silya."),
  ("There is no phone in the box.", "Tidak ada ponsel di dalam kotak.", "Walang telepono sa loob ng kahon."),
  ("Please open the window and close the door.", "Tolong buka jendela dan tutup pintu.", "Pakibuksan ang bintana at isara ang pinto."),
], [("book", "buku", "aklat"), ("notebook", "buku catatan", "kuwaderno"), ("pencil", "pensil", "lapis"),
    ("chair", "kursi", "silya"), ("table", "meja", "mesa"), ("window", "jendela", "bintana")],
  n("May, mayroon, and wala", "May comes before a noun. Mayroon can stand alone. Wala expresses absence.",
    "May muncul sebelum nomina. Mayroon dapat berdiri sendiri. Wala menyatakan ketiadaan.",
    "Nauuna sa pangngalan ang may. Maaaring mag-isa ang mayroon. Nagsasaad ng kawalan ang wala."))

u("a1_10_school_dialogue", "DIALOGUE", "school", "School life", "classroom",
  ("A missing notebook", "Buku catatan yang hilang", "Ang nawawalang kuwaderno"),
  ("Classmates look for an object before class.", "Teman sekelas mencari benda sebelum kelas.", "Naghahanap ng gamit ang magkaklase bago ang klase."), [
  ("Rina, where is my blue notebook?", "Rina, di mana buku catatan biru saya?", "Rina, nasaan ang asul kong kuwaderno?"),
  ("Is that notebook on the teacher's desk yours?", "Apakah buku catatan di meja guru itu milikmu?", "Iyo ba ang kuwadernong iyon sa mesa ng guro?"),
  ("No, mine has a small star.", "Bukan, milik saya memiliki bintang kecil.", "Hindi, may maliit na bituin ang kuwaderno ko."),
  ("Wait, there is something under your chair.", "Tunggu, ada sesuatu di bawah kursimu.", "Sandali, may bagay sa ilalim ng silya mo."),
  ("Here it is! Thank you for helping me.", "Ini dia! Terima kasih telah membantu saya.", "Heto na! Salamat sa pagtulong sa akin."),
], [("where is", "di mana", "nasaan"), ("mine", "milik saya", "akin"), ("yours", "milikmu", "iyo"),
    ("wait", "tunggu", "sandali"), ("here it is", "ini dia", "heto na")], speakers=("Bimo", "Rina", "Bimo", "Rina", "Bimo"),
  exercises=[q("Where is the notebook?", "Di mana buku catatannya?", "Nasaan ang kuwaderno?", "Under the chair.", "Di bawah kursi.", "Sa ilalim ng silya.")])

u("a1_11_verbs", "LESSON", "grammar", "Verbs & routines", "time",
  ("Common verbs and basic aspect", "Verba umum dan aspek dasar", "Karaniwang pandiwa at payak na aspekto"),
  ("Talk about completed, ongoing, and future actions.", "Bicarakan tindakan selesai, berlangsung, dan mendatang.", "Pag-usapan ang naganap, nagaganap, at magaganap na kilos."), [
  ("I studied Tagalog yesterday.", "Saya belajar Tagalog kemarin.", "Nag-aral ako ng Tagalog kahapon."),
  ("I am studying Tagalog now.", "Saya sedang belajar Tagalog sekarang.", "Nag-aaral ako ng Tagalog ngayon."),
  ("I will study again tomorrow.", "Saya akan belajar lagi besok.", "Mag-aaral ulit ako bukas."),
  ("Paolo ate breakfast at home.", "Paolo sarapan di rumah.", "Kumain ng almusal si Paolo sa bahay."),
  ("He eats lunch at school every day and will eat dinner at home.", "Dia makan siang di sekolah setiap hari dan akan makan malam di rumah.", "Kumakain siya ng pananghalian sa paaralan araw-araw at kakain ng hapunan sa bahay."),
], [("study", "belajar", "mag-aral"), ("studied", "telah belajar", "nag-aral"), ("is studying", "sedang belajar", "nag-aaral"),
    ("will study", "akan belajar", "mag-aaral"), ("ate", "telah makan", "kumain"), ("will eat", "akan makan", "kakain")],
  n("Aspect, not tense", "Verb forms primarily show completed, ongoing, or contemplated action. Kahapon, ngayon, and bukas clarify time.",
    "Bentuk verba terutama menunjukkan tindakan selesai, berlangsung, atau akan dilakukan. Kahapon, ngayon, dan bukas memperjelas waktu.",
    "Pangunahing ipinapakita ng pandiwa kung naganap, nagaganap, o magaganap ang kilos. Nililinaw ng kahapon, ngayon, at bukas ang panahon."))

u("a1_12_negation_description", "LESSON", "grammar", "Negation & description", "friends",
  ("Negation and descriptions", "Negasi dan deskripsi", "Pagtanggi at paglalarawan"),
  ("Use hindi, wala, huwag, adjectives, and linkers.", "Gunakan hindi, wala, huwag, adjektiva, dan penghubung.", "Gamitin ang hindi, wala, huwag, pang-uri, at pang-angkop."), [
  ("The room is not dirty; it is clean and bright.", "Ruangan itu tidak kotor; ruangan itu bersih dan terang.", "Hindi marumi ang silid; malinis at maliwanag ito."),
  ("There is no hot water today.", "Tidak ada air panas hari ini.", "Walang mainit na tubig ngayon."),
  ("Do not open the red box.", "Jangan buka kotak merah itu.", "Huwag buksan ang pulang kahon."),
  ("Ana has a beautiful small garden.", "Ana memiliki taman kecil yang indah.", "May maganda at maliit na hardin si Ana."),
  ("The new student is very quiet but friendly.", "Siswa baru itu sangat pendiam tetapi ramah.", "Napakatahimik pero palakaibigan ang bagong estudyante."),
], [("not", "tidak", "hindi"), ("none", "tidak ada", "wala"), ("do not", "jangan", "huwag"),
    ("clean", "bersih", "malinis"), ("beautiful", "indah", "maganda"), ("small", "kecil", "maliit")],
  n("Three negatives and a linker", "Hindi negates predicates, wala expresses absence, and huwag gives a negative command. Use na or -ng to link descriptions.",
    "Hindi menegasikan predikat, wala menyatakan ketiadaan, dan huwag membentuk perintah negatif. Gunakan na atau -ng untuk menghubungkan deskripsi.",
    "Itinatanggi ng hindi ang panaguri, nagsasaad ng kawalan ang wala, at ginagamit ang huwag sa utos. Gamitin ang na o -ng bilang pang-angkop."))

u("a1_13_food", "LESSON", "food", "Food & drink", "food",
  ("Food, drinks, and polite ordering", "Makanan, minuman, dan memesan dengan sopan", "Pagkain, inumin, at magalang na pag-order"),
  ("State preferences, quantities, and prices.", "Nyatakan kesukaan, jumlah, dan harga.", "Sabihin ang gusto, dami, at presyo."), [
  ("I like rice, chicken, vegetables, and mangoes.", "Saya suka nasi, ayam, sayuran, dan mangga.", "Gusto ko ng kanin, manok, gulay, at mangga."),
  ("I do not like very salty food.", "Saya tidak suka makanan yang sangat asin.", "Ayoko ng napakaalat na pagkain."),
  ("May I have one bowl of soup, please?", "Boleh minta satu mangkuk sup?", "Pahingi po ng isang mangkok ng sabaw."),
  ("I would like cold water and hot coffee, please.", "Saya ingin air dingin dan kopi panas.", "Gusto ko po ng malamig na tubig at mainit na kape."),
  ("How much is the meal? It is one hundred twenty pesos.", "Berapa harga makanannya? Harganya seratus dua puluh peso.", "Magkano po ang pagkain? Isang daan at dalawampung piso po."),
], [("rice", "nasi", "kanin"), ("chicken", "ayam", "manok"), ("vegetables", "sayuran", "gulay"),
    ("water", "air", "tubig"), ("coffee", "kopi", "kape"), ("please give me", "boleh minta", "pahingi po"), ("how much", "berapa harga", "magkano")],
  n("Po and opo", "Po adds respect inside a sentence. Opo is the respectful form of oo.",
    "Po menambahkan rasa hormat di dalam kalimat. Opo adalah bentuk hormat dari oo.",
    "Nagdaragdag ng paggalang ang po. Magalang na anyo ng oo ang opo."))

u("a1_14_cafe", "DIALOGUE", "food", "Food & ordering", "food",
  ("Lunch at a small café", "Makan siang di kafe kecil", "Pananghalian sa maliit na kapihan"),
  ("A customer orders and asks for the bill.", "Pelanggan memesan dan meminta tagihan.", "Um-order ang mamimili at humingi ng bayarin."), [
  ("Good afternoon. What would you like?", "Selamat siang. Anda ingin apa?", "Magandang hapon po. Ano ang gusto ninyo?"),
  ("One chicken meal and a glass of water, please.", "Satu paket ayam dan segelas air.", "Isang pagkaing may manok at isang basong tubig po."),
  ("Would you like rice with that?", "Apakah Anda ingin nasi juga?", "Gusto ninyo ng kanin?"),
  ("Yes, but no spicy sauce.", "Ya, tetapi tanpa saus pedas.", "Opo, pero walang maanghang na sarsa."),
  ("Anything else?", "Ada tambahan lain?", "May iba pa?"),
  ("That's all. May I have the bill later?", "Itu saja. Boleh minta tagihannya nanti?", "Iyon lang po. Pahingi ng bayarin mamaya."),
], [("what would you like", "Anda ingin apa", "ano ang gusto ninyo"), ("anything else", "ada tambahan lain", "may iba pa"),
    ("that's all", "itu saja", "iyon lang"), ("bill", "tagihan", "bayarin")], speakers=("Server", "Customer", "Server", "Customer", "Server", "Customer"),
  exercises=[q("What does the customer not want?", "Apa yang tidak diinginkan pelanggan?", "Ano ang ayaw ng mamimili?", "Spicy sauce.", "Saus pedas.", "Maanghang na sarsa.")])

u("market_beginner", "STORY", "food", "Food & shopping", "food",
  ("The morning market", "Pasar pagi", "Ang palengke sa umaga"),
  ("Maya shops, compares prices, and helps a vendor.", "Maya berbelanja, membandingkan harga, dan membantu pedagang.", "Namimili si Maya, naghahambing ng presyo, at tumutulong sa tindera."), [
  ("Maya walks to the market in the morning.", "Maya berjalan ke pasar pada pagi hari.", "Naglalakad si Maya papunta sa palengke sa umaga."),
  ("She needs tomatoes, onions, bananas, and fresh fish for lunch.", "Dia membutuhkan tomat, bawang, pisang, dan ikan segar untuk makan siang.", "Kailangan niya ng kamatis, sibuyas, saging, at sariwang isda para sa pananghalian."),
  ("She asks, How much is one kilo of tomatoes?", "Dia bertanya, berapa harga satu kilo tomat?", "Nagtatanong siya, Magkano po ang isang kilo ng kamatis?"),
  ("The vendor says eighty pesos, so Maya buys one kilo.", "Pedagang berkata delapan puluh peso, jadi Maya membeli satu kilo.", "Sabi ng tindera ay walumpung piso, kaya bumibili si Maya ng isang kilo."),
  ("A bag falls, and Maya picks up the vegetables.", "Sebuah tas jatuh dan Maya memungut sayuran.", "Nahuhulog ang bag, at pinupulot ni Maya ang mga gulay."),
  ("The vendor thanks her and gives her two ripe mangoes.", "Pedagang berterima kasih dan memberinya dua mangga matang.", "Nagpapasalamat ang tindera at binibigyan siya ng dalawang hinog na mangga."),
], [("market", "pasar", "palengke"), ("one kilo", "satu kilo", "isang kilo"), ("vendor", "pedagang", "tindera"),
    ("buy", "membeli", "bumili"), ("ripe", "matang", "hinog")], exercises=[q("How much are the tomatoes?", "Berapa harga tomatnya?", "Magkano ang kamatis?", "Eighty pesos.", "Delapan puluh peso.", "Walumpung piso.")])

u("a1_16_places", "LESSON", "travel", "Places & directions", "travel",
  ("Places and directions", "Tempat dan petunjuk arah", "Mga lugar at direksiyon"),
  ("Find places and give a simple route.", "Temukan tempat dan berikan rute sederhana.", "Hanapin ang lugar at magbigay ng payak na ruta."), [
  ("The bank is beside the pharmacy.", "Bank berada di samping apotek.", "Katabi ng botika ang bangko."),
  ("The bus stop is across from the park.", "Halte bus berada di seberang taman.", "Tapat ng parke ang hintuan ng bus."),
  ("Go straight and turn left at the church.", "Jalan lurus dan belok kiri di gereja.", "Dumiretso at kumaliwa sa simbahan."),
  ("Then turn right at the second street.", "Kemudian belok kanan di jalan kedua.", "Pagkatapos, kumanan sa ikalawang kalye."),
  ("The hospital is near the large hotel. It is not far.", "Rumah sakit dekat hotel besar. Tidak jauh.", "Malapit sa malaking otel ang ospital. Hindi ito malayo."),
], [("beside", "di samping", "katabi"), ("across from", "di seberang", "tapat"), ("straight", "lurus", "diretso"),
    ("left", "kiri", "kaliwa"), ("right", "kanan", "kanan"), ("near", "dekat", "malapit")],
  n("Nasa and papunta sa", "Use nasa for a specific location and papunta sa for movement toward a place.",
    "Gunakan nasa untuk lokasi tertentu dan papunta sa untuk gerak menuju tempat.",
    "Gamitin ang nasa sa tiyak na lugar at papunta sa sa kilos tungo sa lugar."))

u("a1_17_directions", "DIALOGUE", "travel", "Traveling", "travel",
  ("Finding the bus terminal", "Mencari terminal bus", "Paghahanap sa terminal ng bus"),
  ("A traveler asks for help.", "Seorang pelancong meminta bantuan.", "Humihingi ng tulong ang biyahero."), [
  ("Excuse me, where is the bus terminal?", "Permisi, di mana terminal bus?", "Makikiraan po, nasaan ang terminal ng bus?"),
  ("Go straight until the traffic light.", "Jalan lurus sampai lampu lalu lintas.", "Dumiretso kayo hanggang sa ilaw-trapiko."),
  ("Do I turn left there?", "Apakah saya belok kiri di sana?", "Kakaliwa ba ako roon?"),
  ("No. Turn right, then cross the bridge.", "Tidak. Belok kanan, lalu seberangi jembatan.", "Hindi po. Kumanan kayo, tapos tumawid sa tulay."),
  ("Is the terminal far?", "Apakah terminalnya jauh?", "Malayo pa ba ang terminal?"),
  ("It is close, beside the supermarket.", "Dekat, di samping supermarket.", "Malapit na, katabi ng supermarket."),
], [("excuse me", "permisi", "makikiraan po"), ("until", "sampai", "hanggang"), ("cross", "menyeberang", "tumawid")],
  speakers=("Traveler", "Local", "Traveler", "Local", "Traveler", "Local"),
  exercises=[q("What is beside the terminal?", "Apa yang berada di samping terminal?", "Ano ang katabi ng terminal?", "The supermarket.", "Supermarket.", "Ang supermarket.")])

u("a1_18_friendship", "STORY", "friends", "Friendship & simple romance", "friends",
  ("Coffee after class", "Kopi setelah kelas", "Kape pagkatapos ng klase"),
  ("A gentle story about friendship and a new feeling.", "Cerita ringan tentang persahabatan dan perasaan baru.", "Magaan na kuwento tungkol sa pagkakaibigan at bagong damdamin."), [
  ("Aya and Tomas study together every Tuesday.", "Aya dan Tomas belajar bersama setiap Selasa.", "Magkasamang nag-aaral sina Aya at Tomas tuwing Martes."),
  ("Tomas is quiet, but he always helps Aya with difficult lessons.", "Tomas pendiam, tetapi selalu membantu Aya dalam pelajaran sulit.", "Tahimik si Tomas, pero lagi niyang tinutulungan si Aya sa mahihirap na aralin."),
  ("After class, Aya invites him to drink coffee.", "Setelah kelas, Aya mengajaknya minum kopi.", "Pagkatapos ng klase, inaanyayahan siya ni Aya na magkape."),
  ("They talk about music, family, and their dreams.", "Mereka berbicara tentang musik, keluarga, dan impian mereka.", "Nag-uusap sila tungkol sa musika, pamilya, at mga pangarap nila."),
  ("Aya says, I like spending time with you.", "Aya berkata, saya suka menghabiskan waktu bersamamu.", "Sinasabi ni Aya, Gusto kong kasama ka."),
  ("Tomas likes Aya too, but they remain good friends and take things slowly.", "Tomas juga menyukai Aya, tetapi mereka tetap berteman baik dan menjalaninya perlahan.", "Gusto rin ni Tomas si Aya, pero nananatili silang mabuting magkaibigan at hindi nagmamadali."),
], [("together", "bersama", "magkasama"), ("dream", "impian", "pangarap"), ("I like you", "saya menyukaimu", "gusto kita"),
    ("good friends", "teman baik", "mabuting magkaibigan")],
  n("Gusto kita", "Gusto kita can express romantic interest. It is less intense than mahal kita, I love you.",
    "Gusto kita dapat menyatakan rasa suka romantis. Ungkapan ini tidak seintens mahal kita, saya mencintaimu.",
    "Maaaring magpahayag ng romantikong pagtingin ang gusto kita. Hindi ito kasingtindi ng mahal kita."))

u("a1_19_phone", "DIALOGUE", "routine", "Daily conversation", "friends",
  ("A quick phone call", "Panggilan telepon singkat", "Isang maikling tawag"),
  ("Friends change an evening plan.", "Teman-teman mengubah rencana malam.", "Binabago ng magkaibigan ang plano sa gabi."), [
  ("Hello, Bea. Are you at home?", "Halo, Bea. Apakah kamu di rumah?", "Hello, Bea. Nasa bahay ka ba?"),
  ("Yes, but I'm cooking dinner. Why?", "Ya, tetapi saya sedang memasak makan malam. Mengapa?", "Oo, pero nagluluto ako ng hapunan. Bakit?"),
  ("Do you want to watch a movie at seven?", "Apakah kamu ingin menonton film pukul tujuh?", "Gusto mo bang manood ng pelikula nang alas-siyete?"),
  ("I can't at seven. Can we meet at eight?", "Saya tidak dapat pukul tujuh. Dapatkah kita bertemu pukul delapan?", "Hindi ako puwede nang alas-siyete. Puwede ba tayong magkita nang alas-otso?"),
  ("Sure. I'll buy the tickets online.", "Tentu. Saya akan membeli tiket secara daring.", "Sige. Bibili ako ng mga tiket online."),
  ("Great. Send me a message when you leave.", "Bagus. Kirimi saya pesan saat kamu berangkat.", "Ayos. Padalhan mo ako ng mensahe kapag aalis ka na."),
], [("movie", "film", "pelikula"), ("can", "dapat", "puwede"), ("send me", "kirimi saya", "padalhan mo ako")],
  speakers=("Rafi", "Bea", "Rafi", "Bea", "Rafi", "Bea"), exercises=[q("Why can't Bea meet at seven?", "Mengapa Bea tidak dapat bertemu pukul tujuh?", "Bakit hindi puwede si Bea nang alas-siyete?", "She is cooking dinner.", "Dia memasak makan malam.", "Nagluluto siya ng hapunan.")])

u("a1_20_final", "STORY", "review", "A1 final reading", "friends",
  ("A welcome day in Manila", "Hari penyambutan di Manila", "Isang masayang araw sa Maynila"),
  ("A complete A1 story brings the course together.", "Cerita A1 lengkap menyatukan seluruh kursus.", "Pinagsasama-sama ng buong kuwentong A1 ang kurso."), [
  ("Sari is an Indonesian student who lives in Manila for three months.", "Sari adalah siswa Indonesia yang tinggal di Manila selama tiga bulan.", "Isang estudyanteng Indones si Sari na nakatira sa Maynila nang tatlong buwan."),
  ("On Monday, her classmate Joel meets her at the bus stop.", "Pada hari Senin, teman sekelasnya Joel menemuinya di halte bus.", "Sa Lunes, sinasalubong siya ng kaklase niyang si Joel sa hintuan ng bus."),
  ("They go to school together, and Joel shows her the library and classroom.", "Mereka pergi ke sekolah bersama dan Joel menunjukkan perpustakaan serta ruang kelas.", "Magkasama silang pumupunta sa paaralan, at ipinapakita ni Joel ang aklatan at silid-aralan."),
  ("At lunch, Sari orders rice, vegetables, and cold water in Tagalog.", "Saat makan siang, Sari memesan nasi, sayuran, dan air dingin dalam bahasa Tagalog.", "Sa pananghalian, umuorder si Sari ng kanin, gulay, at malamig na tubig sa Tagalog."),
  ("After class, they buy fruit at the market and ask for directions to the park.", "Setelah kelas, mereka membeli buah di pasar dan meminta petunjuk ke taman.", "Pagkatapos ng klase, bumibili sila ng prutas sa palengke at nagtatanong ng direksiyon papunta sa parke."),
  ("Sari makes a few mistakes, but people understand and help her.", "Sari membuat beberapa kesalahan, tetapi orang-orang memahami dan membantunya.", "Nagkakamali nang kaunti si Sari, pero nauunawaan at tinutulungan siya ng mga tao."),
  ("Tomorrow she will study again, speak more, and meet new friends.", "Besok dia akan belajar lagi, lebih banyak berbicara, dan bertemu teman baru.", "Bukas ay mag-aaral siyang muli, magsasalita pa, at makikipagkilala sa mga bagong kaibigan."),
], [("for three months", "selama tiga bulan", "nang tatlong buwan"), ("ask for directions", "meminta petunjuk", "magtanong ng direksiyon"),
    ("make mistakes", "membuat kesalahan", "magkamali"), ("people understand", "orang-orang memahami", "nauunawaan ng mga tao")],
  exercises=[q("What does Sari order?", "Apa yang dipesan Sari?", "Ano ang inorder ni Sari?", "Rice, vegetables, and water.", "Nasi, sayuran, dan air.", "Kanin, gulay, at tubig.")])

# Additional controlled concepts. Aliases separated by ~ share a contextual A1 gloss.
WORDS = r"""
alphabet|alfabet|alpabeto|noun
vowel~vowels|vokal|patinig~mga patinig|noun
stress|tekanan|diin|noun
sound~sounds|bunyi|tunog~mga tunog|noun
spelling|ejaan|baybay|noun
clear|jelas|malinaw|adjective
change|mengubah|magbago|verb
listen|mendengarkan|makinig|verb
repeat|mengulangi|umulit|verb
slowly|perlahan|dahan-dahan|adverb
read|membaca|basahin|verb
aloud|dengan lantang|nang malakas|adverb
fine~good|baik|mabuti|adjective
also~too|juga|rin~din|adverb
name|nama|pangalan|noun
meet|bertemu|makilala~magkita~magkita tayo|verb
later|nanti|mamaya|adverb
take care|hati-hati|ingat|expression
classmate~classmates|teman sekelas|kaklase~magkaklase|noun
sibling~siblings|saudara|kapatid~magkapatid|noun
included|termasuk|kasama|adjective
teacher|guru|guro|noun
school|sekolah|paaralan|noun
happy|senang|masaya|adjective
class~lesson~lessons|kelas~pelajaran|klase~aralin~mga aralin|noun
student|siswa|estudyante|noun
live~lives|tinggal|nakatira|verb
now~today|sekarang~hari ini|ngayon|adverb
one~first|satu~pertama|isa~isang~unang|numeral
two~second|dua~kedua|dalawa~dalawang~ikalawang|numeral
three~third|tiga~ketiga|tatlo~ikatlo|numeral
five|lima|lima~limang|numeral
six|enam|anim|numeral
seven|tujuh|pito|numeral
eight|delapan|walo|numeral
ten|sepuluh|sampu|numeral
twelve|dua belas|labindalawa~ikalabindalawa|numeral
twenty|dua puluh|dalawampu~dalawampung|numeral
eighty|delapan puluh|walumpu~walumpung|numeral
hundred|seratus|daan|numeral
brother|saudara laki-laki|kapatid na lalaki|noun
sister|saudara perempuan|kapatid na babae|noun
birthday|ulang tahun|kaarawan|noun
o'clock|pukul|alas|marker
Friday|Jumat|Biyernes|noun
Monday|Senin|Lunes|noun
Tuesday|Selasa|Martes|noun
Sunday|Minggu|Linggo|noun
June|Juni|Hunyo|noun
December|Desember|Disyembre|noun
wake up~wakes up|bangun|gumising~gumigising|verb
take a bath|mandi|maligo~naliligo|verb
get dressed|berpakaian|magbihis~nagbibihis|verb
eat~eats~ate~will eat|makan~telah makan~akan makan|kumain~kumakain~kakain|verb
drink~drinks|minum|uminom~umiinom|verb
prepare|menyiapkan|maghanda~naghahanda|verb
leave~leaves|berangkat~meninggalkan|umalis~umaalis|verb
ride~rides|naik|sumakay~sumasakay|verb
arrive~arrives|tiba|dumating~dumarating|verb
late|terlambat|huli|adjective
mother|ibu|nanay|noun
father|ayah|tatay|noun
older brother|kakak laki-laki|kuya|noun
older sister|kakak perempuan|ate|noun
grandmother|nenek|lola|noun
grandfather|kakek|lolo|noun
nurse|perawat|nars|noun
province|provinsi|probinsiya|noun
child|anak|anak|noun
kind|baik hati|mabait|adjective
cheerful|ceria|masayahin|adjective
whose|milik siapa|kanino~kaninong|pronoun
invite|mengundang|mag-anyaya~inaanyayahan|verb
lunch|makan siang|pananghalian|noun
cook|memasak|magluto~nagluluto|verb
grill|memanggang|mag-ihaw~nag-iihaw|verb
soup|sup|sabaw|noun
fish|ikan|isda|noun
younger|lebih muda|nakababata~nakababatang|adjective
set~prepare|menata~menyiapkan|ihanda~inihahanda|verb
plate~plates|piring|plato~mga plato|noun
spoon~spoons|sendok|kutsara~mga kutsara|noun
together|bersama|magkasama~sabay-sabay|adverb
tell~tells|menceritakan|magkuwento~nagkukuwento|verb
funny|lucu|nakakatawa~nakakatawang|adjective
thank~thanks|berterima kasih|magpasalamat~nagpapasalamat|verb
notebook|buku catatan|kuwaderno~kuwadernong|noun
inside|di dalam|loob|preposition
pencil|pensil|lapis|noun
beside|di samping|tabi~katabi|preposition
book|buku|aklat|noun
chair|kursi|silya|noun
in front of|di depan|harap|preposition
table~desk|meja|mesa|noun
phone|ponsel|telepono|noun
box|kotak|kahon|noun
open|membuka|buksan~pakibuksan|verb
close|menutup|isara|verb
window|jendela|bintana|noun
blue|biru|asul~asul na|adjective
mine|milik saya|akin|pronoun
yours|milikmu|iyo|pronoun
small|kecil|maliit~maliitang|adjective
star|bintang|bituin|noun
wait|tunggu|sandali|expression
something~thing|sesuatu~benda|bagay|noun
under|di bawah|ilalim|preposition
here it is|ini dia|heto na|expression
help~helping|membantu|tumulong~pagtulong~tinutulungan|verb
study~studied~is studying~will study|belajar~telah belajar~sedang belajar~akan belajar|mag-aral~nag-aral~nag-aaral~mag-aaral|verb
yesterday|kemarin|kahapon|adverb
tomorrow|besok|bukas|adverb
again|lagi|ulit~muli|adverb
breakfast|sarapan|almusal|noun
every day|setiap hari|araw-araw|adverb
dinner|makan malam|hapunan|noun
dirty|kotor|marumi|adjective
clean|bersih|malinis|adjective
bright|terang|maliwanag|adjective
hot|panas|mainit|adjective
do not|jangan|huwag|adverb
red|merah|pula~pulang|adjective
beautiful|indah|maganda|adjective
garden|taman|hardin|noun
very|sangat|napaka~napakaalat~napakatahimik|adverb
quiet|pendiam|tahimik|adjective
friendly|ramah|palakaibigan|adjective
new|baru|bago~bagong|adjective
rice|nasi|kanin|noun
chicken|ayam|manok|noun
vegetable~vegetables|sayur~sayuran|gulay~mga gulay|noun
mango~mangoes|mangga|mangga|noun
salty|asin|maalat|adjective
bowl|mangkuk|mangkok|noun
cold|dingin|malamig|adjective
water|air|tubig|noun
coffee|kopi|kape|noun
how much|berapa harga|magkano|expression
peso~pesos|peso|piso|noun
customer|pelanggan|mamimili|noun
what would you like|Anda ingin apa|ano ang gusto ninyo|expression
glass|gelas|baso|noun
spicy|pedas|maanghang|adjective
sauce|saus|sarsa|noun
anything else|tambahan lain|may iba pa|expression
that's all|itu saja|iyon lang|expression
bill|tagihan|bayarin|noun
market|pasar|palengke|noun
need|memerlukan|kailangan|verb
tomato~tomatoes|tomat|kamatis|noun
onion~onions|bawang|sibuyas|noun
banana~bananas|pisang|saging|noun
fresh|segar|sariwa~sariwang|adjective
one kilo|satu kilo|isang kilo|expression
vendor|pedagang|tindera|noun
buy~buys~will buy|membeli~akan membeli|bumili~bumibili~bibili|verb
fall~falls|jatuh|mahulog~nahuhulog|verb
pick up|memungut|pulutin~pinupulot|verb
ripe|matang|hinog|adjective
pharmacy|apotek|botika|noun
bank|bank|bangko|noun
across from|di seberang|tapat|preposition
park|taman|parke|noun
bus stop|halte bus|hintuan ng bus|noun
straight|lurus|diretso|adverb
turn left|belok kiri|kumaliwa~kakaliwa|verb
church|gereja|simbahan|noun
turn right|belok kanan|kumanan|verb
street|jalan|kalye|noun
hospital|rumah sakit|ospital|noun
near|dekat|malapit|adjective
large|besar|malaki~malaking|adjective
hotel|hotel|otel|noun
far|jauh|malayo|adjective
excuse me|permisi|makikiraan po|expression
bus terminal|terminal bus|terminal ng bus|noun
until|sampai|hanggang|preposition
traffic light|lampu lalu lintas|ilaw-trapiko|noun
cross|menyeberang|tumawid|verb
bridge|jembatan|tulay|noun
supermarket|supermarket|supermarket|noun
always|selalu|lagi|adverb
difficult|sulit|mahirap~mahihirap|adjective
music|musik|musika|noun
dream~dreams|impian|pangarap~mga pangarap|noun
I like you|saya menyukaimu|gusto kita|expression
good friends|teman baik|mabuting magkaibigan|expression
remain|tetap|manatili~nananatili|verb
movie|film|pelikula|noun
can|dapat|puwede|auxiliary
ticket~tickets|tiket|tiket~mga tiket|noun
online|daring|online|adverb
send me|kirimi saya|padalhan mo ako|expression
message|pesan|mensahe|noun
for three months|selama tiga bulan|nang tatlong buwan|expression
show|menunjukkan|ipakita~ipinapakita|verb
library|perpustakaan|aklatan|noun
classroom|ruang kelas|silid-aralan|noun
order|memesan|umorder~umuorder|verb
ask for directions|meminta petunjuk|magtanong ng direksiyon|expression
make mistakes|membuat kesalahan|magkamali~nagkakamali|expression
people understand|orang-orang memahami|nauunawaan ng mga tao|expression
speak|berbicara|magsalita~magsasalita|verb
meet new friends|berkenalan dengan teman baru|makipagkilala sa mga bagong kaibigan~makikipagkilala sa mga bagong kaibigan|expression
"""

# Contextual coverage for inflected words, clitics, particles, and names that appear in readings.
TL_EXTRA = r"""
a|letter a|huruf a|noun
e|letter e|huruf e|noun
i|letter i|huruf i|noun
o|letter o|huruf o|noun
u|letter u|huruf u|noun
ako|I|saya|pronoun
ka|you|kamu|pronoun
ko|my; I|milik saya; saya|pronoun
kong|my; that I|milik saya; saya|pronoun
mo|your; you|milikmu; kamu|pronoun
kayo|you; you all|Anda; kalian|pronoun
ninyo|your; you all|milik Anda; kalian|pronoun
niya|his; her|miliknya|pronoun
niyang|his; her; that he or she|miliknya; dia|pronoun
namin|our, excluding listener|milik kami|pronoun
sila|they|mereka|pronoun
silang|they, linked|mereka|pronoun
siyang|he or she, linked|dia|pronoun
tayong|we including listener, linked|kita|pronoun
akin|me; mine|saya; milik saya|pronoun
ba|question marker|partikel tanya|marker
bang|question marker after a vowel|partikel tanya setelah vokal|marker
po|respect marker|partikel hormat|marker
naman|contrastive or softening particle|partikel pelembut atau pembanding|particle
pa|still; yet; more|masih; lagi|particle
lang|only|hanya|particle
nang|adverb or time linker|penghubung keterangan atau waktu|marker
nasa|located at|berada di|preposition
roon|there|di sana|adverb
saan|where|di mana|adverb
sino|who|siapa|pronoun
kailan|when|kapan|adverb
bakit|why|mengapa|adverb
paano|how|bagaimana|adverb
pero|but|tetapi|conjunction
kaya|so; therefore|jadi; karena itu|conjunction
tapos|then|lalu|adverb
tuwing|every|setiap|preposition
tungkol|about|tentang|preposition
iba|other|lain|adjective
tao|person; people|orang|noun
kahulugan|meaning|makna|noun
salitang|word, linked|kata|noun
kuwento|story|cerita|noun
lalaki|male; man|laki-laki|noun
buwan|month|bulan|noun
direksiyon|direction|petunjuk arah|noun
hintuan|stop; stopping place|halte|noun
kilo|kilo|kilo|noun
bus|bus|bus|noun
itlog|egg|telur|noun
silid|room|ruangan|noun
terminal|terminal|terminal|noun
tagalog|Tagalog language|bahasa Tagalog|noun
filipino|Filipino language|bahasa Filipino|noun
indones|Indonesian person|orang Indonesia|noun
maynila|Manila|Manila|proper noun
arif|Arif|Arif|proper noun
liza|Liza|Liza|proper noun
nia|Nia|Nia|proper noun
mara|Mara|Mara|proper noun
joel|Joel|Joel|proper noun
sari|Sari|Sari|proper noun
lina|Lina|Lina|proper noun
rosa|Rosa|Rosa|proper noun
carlo|Carlo|Carlo|proper noun
dito|Dito|Dito|proper noun
rina|Rina|Rina|proper noun
paolo|Paolo|Paolo|proper noun
aya|Aya|Aya|proper noun
tomas|Tomas|Tomas|proper noun
bea|Bea|Bea|proper noun
alas-sais|six o'clock|pukul enam|expression
alas-siyete|seven o'clock|pukul tujuh|expression
alas-singko|five o'clock|pukul lima|expression
alas-otso|eight o'clock|pukul delapan|expression
mag-alas-otso|nearly eight o'clock|hampir pukul delapan|expression
taga-saan|from where|berasal dari mana|expression
taga-cebu|from Cebu|berasal dari Cebu|expression
magandang|beautiful; good, linked|indah; selamat|adjective
mabuting|good, linked|baik|adjective
magkasamang|together, linked|bersama|adverb
basong|glass, linked|gelas|noun
pagkaing|food; meal, linked|makanan|noun
estudyanteng|student, linked|siswa|noun
iisang|one single|satu|numeral
tatlong|three, linked|tiga|numeral
akong|I; my, linked|saya; milik saya|pronoun
kitang|you, linked to I|kamu|pronoun
aalis|will leave|akan berangkat|verb
binibigyan|is giving|memberi|verb
dumiretso|go straight|jalan lurus|verb
heto|here it is|ini dia|expression
ikinagagalak|am pleased|merasa senang|verb
magkape|drink coffee|minum kopi|verb
makikipagkilala|will meet new people|akan berkenalan|verb
makikiraan|excuse me; let me pass|permisi|expression
manood|watch|menonton|verb
nag-uusap|are talking|sedang berbicara|verb
nagmamadali|are rushing|terburu-buru|verb
nagtatanong|is asking|sedang bertanya|verb
padalhan|send to|kirimkan kepada|verb
pahingi|please give me|boleh minta|expression
pumupunta|goes; is going|pergi|verb
sabi|said; according to|berkata|verb
sige|all right|baiklah|expression
oo|yes|ya|expression
opo|yes, respectfully|ya, dengan hormat|expression
ayos|great; all right|bagus; baik|expression
hello|hello|halo|expression
hi|hi|hai|expression
kumusta|how are you|apa kabar|expression
ayoko|I do not like|saya tidak suka|verb
kaunti|a little; a few|sedikit|adjective
magkaibigan|friends with each other|berteman|noun
malakas|loud; strong|keras; kuat|adjective
sina|plural personal-name marker|penanda jamak untuk nama orang|marker
iyon|that|itu|pronoun
iyan|that near you|itu di dekatmu|pronoun
magkape|drink coffee|minum kopi|verb
"""

DEFAULT_NOTES = {
 "noun": t("Names a person, place, thing, or idea.", "Menamai orang, tempat, benda, atau gagasan.", "Ngalan ng tao, lugar, bagay, o ideya."),
 "verb": t("An action form; notice its aspect in the example.", "Bentuk tindakan; perhatikan aspeknya dalam contoh.", "Anyong kilos; pansinin ang aspekto sa halimbawa."),
 "adjective": t("Describes a noun or state.", "Menjelaskan nomina atau keadaan.", "Naglalarawan ng pangngalan o kalagayan."),
 "adverb": t("Adds time, manner, place, or degree.", "Menambahkan waktu, cara, tempat, atau tingkat.", "Nagdaragdag ng oras, paraan, lugar, o antas."),
 "pronoun": t("Refers to a person or thing.", "Merujuk orang atau benda.", "Tumutukoy sa tao o bagay."),
 "preposition": t("Marks place, direction, time, or relationship.", "Menandai tempat, arah, waktu, atau hubungan.", "Pananda ng lugar, direksiyon, oras, o ugnayan."),
 "expression": t("Learn this useful expression as one complete chunk.", "Pelajari ungkapan ini sebagai satu kesatuan.", "Aralin bilang isang buo ang pahayag na ito."),
 "numeral": t("Expresses number or order.", "Menyatakan jumlah atau urutan.", "Nagsasaad ng bilang o pagkakasunod."),
 "auxiliary": t("Supports the main predicate.", "Mendukung predikat utama.", "Tumutulong sa pangunahing panaguri."),
 "marker": t("A grammar marker understood through the complete sentence.", "Penanda tata bahasa yang dipahami melalui kalimat lengkap.", "Panandang pambalarila na mauunawaan sa buong pangungusap."),
 "particle": t("Adds conversational nuance.", "Menambahkan nuansa percakapan.", "Nagdaragdag ng himig sa usapan."),
 "conjunction": t("Connects words or clauses.", "Menghubungkan kata atau klausa.", "Nag-uugnay ng salita o sugnay."),
 "proper noun": t("A person's or place's name.", "Nama orang atau tempat.", "Pangalan ng tao o lugar."),
}

TOKEN = re.compile(r"[^\W\d_]+(?:['’-][^\W\d_]+)*", re.UNICODE)
PRACTICE = {
 "ba": (t("Complete the yes-no question: Estudyante ka ___?", "Lengkapi pertanyaan ya-tidak: Estudyante ka ___?", "Buuin ang tanong: Estudyante ka ___?"), ["ba", "po", "lang"]),
 "hindi": (t("Choose the predicate negator: ___ marumi ang silid.", "Pilih negator predikat: ___ marumi ang silid.", "Piliin ang pananggi: ___ marumi ang silid."), ["hindi", "wala", "huwag"]),
 "wala": (t("Choose the word for absence: ___ akong telepono.", "Pilih kata untuk ketiadaan: ___ akong telepono.", "Piliin ang salita sa kawalan: ___ akong telepono."), ["wala", "hindi", "ba"]),
 "huwag": (t("Complete the negative command: ___ buksan ang kahon.", "Lengkapi perintah negatif: ___ buksan ang kahon.", "Buuin ang utos: ___ buksan ang kahon."), ["huwag", "wala", "lang"]),
 "po": (t("Add respect: Salamat ___.", "Tambahkan kesopanan: Salamat ___.", "Idagdag ang paggalang: Salamat ___."), ["po", "ba", "pa"]),
 "ang": (t("Choose the focus marker: Nasa mesa ___ aklat.", "Pilih penanda fokus: Nasa mesa ___ aklat.", "Piliin ang pananda: Nasa mesa ___ aklat."), ["ang", "ng", "sa"]),
 "sa": (t("Choose the location marker: Nakatira siya ___ Maynila.", "Pilih penanda lokasi: Nakatira siya ___ Maynila.", "Piliin ang pananda ng lugar: Nakatira siya ___ Maynila."), ["sa", "ang", "ba"]),
}

def concepts():
    for line in WORDS.strip().splitlines():
        en, id_, tl, pos = line.split("|")
        yield [en.split("~"), id_.split("~"), tl.split("~")], pos

def build():
    seed = json.loads(ASSET.read_text(encoding="utf-8"))["lexicon"]
    lex = {(x["language"], x["word"].lower()): x for x in seed}
    for aliases, pos in concepts():
        primary = dict(zip(LANGS, (x[0] for x in aliases)))
        for i, lang in enumerate(LANGS):
            for word in aliases[i]:
                key = (lang, word.lower())
                lex[key] = {"language": lang, "word": word.lower(), "translations": primary,
                    "meanings": t(f"A1 meaning: {primary['en']}.", f"Makna A1: {primary['id']}.", f"Kahulugang A1: {primary['tl']}."),
                    "pos": pos, "pronunciation": word, "notes": DEFAULT_NOTES[pos]}
    for line in TL_EXTRA.strip().splitlines():
        word, en, id_, pos = line.split("|")
        lex[("tl", word)] = {"language": "tl", "word": word, "translations": t(en, id_, word),
            "meanings": t(f"Contextual A1 meaning: {en}.", f"Makna A1 kontekstual: {id_}.", f"Kahulugang A1: {word}."),
            "pos": pos, "pronunciation": word, "notes": DEFAULT_NOTES[pos]}
    # Focus expressions are first-class lexemes and use the same SRS as tapped words.
    for unit in UNITS:
        for triple in zip(*(unit["focus"][lang] for lang in LANGS)):
            primary = dict(zip(LANGS, triple))
            for lang, word in zip(LANGS, triple):
                lex[(lang, word.lower())] = {"language": lang, "word": word.lower(), "translations": primary,
                    "meanings": t(f"Useful A1 expression: {primary['en']}.", f"Ungkapan A1: {primary['id']}.", f"Pahayag sa A1: {primary['tl']}."),
                    "pos": "expression", "pronunciation": word, "notes": DEFAULT_NOTES["expression"]}
    missing = set()
    used = set()
    for unit in UNITS:
        for lang in LANGS:
            for sentence in unit["sentences"]:
                for word in TOKEN.findall(sentence[lang]):
                    key = (lang, word.lower()); used.add(key)
                    if key not in lex: missing.add((lang, word.lower(), unit["id"]))
            for word in unit["focus"][lang]: used.add((lang, word.lower()))
    # English and Indonesian remain fully tappable. Core course vocabulary has curated
    # concepts above; incidental function/inflected forms receive an explicit contextual gloss.
    contextual = [x for x in missing if x[0] != "tl"]
    for lang, word, _ in contextual:
        reference = t(f"Contextual form: {word}.", f"Bentuk kontekstual: {word}.", f"Anyong ayon sa konteksto: {word}.")
        lex[(lang, word)] = {"language": lang, "word": word, "translations": reference,
            "meanings": reference, "pos": "expression", "pronunciation": word, "notes": DEFAULT_NOTES["expression"]}
    missing = {x for x in missing if x[0] == "tl"}
    if missing:
        raise ValueError("Missing dictionary coverage:\n" + "\n".join(f"{a}:{b} in {c}" for a,b,c in sorted(missing)))
    assert [x["order"] for x in UNITS] == list(range(1, len(UNITS)+1))
    assert all(x["level"] == "BEGINNER" for x in UNITS)
    for word, (prompt, choices) in PRACTICE.items():
        if ("tl", word) in lex: lex[("tl", word)]["practice"] = {"prompt": prompt, "choices": choices}
    for key in used:
        item = lex[key]; item.setdefault("pronunciation", item["word"])
        assert all(item[field][lang].strip() for field in ("translations", "meanings", "notes") for lang in LANGS)
    ASSET.write_text(json.dumps({"version": 2, "course": "Tagalog A1", "stories": UNITS,
                                "lexicon": [lex[k] for k in sorted(used)]}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(f"Validated {len(UNITS)} ordered A1 units and {len(used)} dictionary entries in all three languages.")

if __name__ == "__main__": build()
