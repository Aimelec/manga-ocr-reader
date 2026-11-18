"""Use cases with fake tokenizer, dictionary, OCR and model."""
import pytest

from backend.domain.grammar import GrammarMatcher, GrammarPattern
from backend.repositories.jmdict import KanjiEntry, NameEntry, Sense, WordEntry
from backend.services.analysis import BubbleReader, TextAnalyzer, Vocabulary, WordExplainer
from backend.services.explain import LineExplainer
from backend.services.word_lookup import WordLookup
from tests.fakes import FakeChat, FakeDictionary, FakeOcr, FakeRuntime, FakeTokenizer, entry, morpheme

VOCAB = Vocabulary(pos_en={"名詞": "noun", "動詞": "verb", "助詞": "particle"},
                   form_en={"終止形": "plain form"},
                   function_words={"は": "topic marker", "だ": "copula"})


def analyzer_for(sentences, dictionary=None, patterns=()):
    tok = FakeTokenizer(sentences)
    dictionary = dictionary or FakeDictionary()
    return TextAnalyzer(tok, WordExplainer(tok, dictionary, VOCAB), GrammarMatcher(patterns), VOCAB), dictionary


class TestWordExplainer:
    def test_particle_uses_the_table(self):
        a, d = analyzer_for({"は": [morpheme("は", "助詞", "係助詞")]})
        t = a.tokens("は")[0]
        assert t["function"] == "topic marker" and t["entries"] == [] and d.calls == []

    def test_particle_falls_back_to_dictionary_form(self):
        a, _ = analyzer_for({"だっ": [morpheme("だっ", "助動詞", "*", base="だ")]})
        assert a.tokens("だっ")[0]["function"] == "copula"

    def test_symbols_get_nothing(self):
        a, d = analyzer_for({"。": [morpheme("。", "補助記号", "句点")]})
        assert a.tokens("。")[0]["entries"] == [] and d.calls == []

    def test_tries_normalized_then_base_then_surface(self):
        d = FakeDictionary(words={"食べる": [entry("to eat")]})
        m = morpheme("食べ", "動詞", "一般", base="食べる", normalized="食べる", reading="タベ")
        a, _ = analyzer_for({"食べ": [m]}, d)
        assert a.tokens("食べ")[0]["entries"][0]["senses"][0]["gloss"] == "to eat"
        assert d.calls[0][0] == "食べる"

    def test_surface_lookup_passes_the_reading(self):
        d = FakeDictionary(words={"今日": [entry("today")]})
        m = morpheme("今日", base="今日X", normalized="今日Y", reading="キョウ")
        a, _ = analyzer_for({"今日": [m]}, d)
        a.tokens("今日")
        assert [c[:2] for c in d.calls] == [("今日Y", ""), ("今日X", ""), ("今日", "きょう")]

    def test_proper_nouns_include_names(self):
        d = FakeDictionary(words={"田中": [entry("Tanaka")]})
        a, _ = analyzer_for({"田中": [morpheme("田中", "名詞", "固有名詞", reading="タナカ")]}, d)
        t = a.tokens("田中")[0]
        assert d.calls[0][2] is True and t["pos_en"] == "noun (name)"

    def test_unknown_compound_is_looked_up_by_parts(self):
        parts = [morpheme("宇宙", reading="ウチュウ"), morpheme("開発", reading="カイハツ"), morpheme("の", "助詞")]
        d = FakeDictionary(words={"宇宙": [entry("space"), entry("universe")], "開発": [entry("development")]})
        a, _ = analyzer_for({"宇宙開発": [morpheme("宇宙開発", reading="ウチュウカイハツ", parts=parts)]}, d)
        t = a.tokens("宇宙開発")[0]
        assert t["entries"] == []
        assert [(p["surface"], p["reading"], len(p["entries"])) for p in t["parts"]] == [
            ("宇宙", "うちゅう", 1), ("開発", "かいはつ", 1)]

    def test_kanji_details_once_per_character(self):
        d = FakeDictionary(kanji={"人": {"char": "人"}})
        a, _ = analyzer_for({"人人": [morpheme("人人", reading="ヒトビト")]}, d)
        assert a.tokens("人人")[0]["kanji"] == [{"char": "人"}]


class TestTextAnalyzer:
    def test_kanji_word_gets_hiragana_reading_and_ruby(self):
        a, _ = analyzer_for({"食べる": [morpheme("食べる", "動詞", "一般", form="終止形-一般", reading="タベル")]})
        t = a.tokens("食べる")[0]
        assert t["reading"] == "たべる" and t["has_kanji"] is True
        assert t["ruby"] == [("食", "た"), ("べる", "")]
        assert (t["pos_en"], t["form_en"]) == ("verb", "plain form")

    def test_kana_word_reading_is_its_surface(self):
        a, _ = analyzer_for({"ねこ": [morpheme("ねこ", reading="ネコ")]})
        assert a.tokens("ねこ")[0]["reading"] == "ねこ"

    def test_unknown_word_keeps_its_surface(self):
        a, _ = analyzer_for({"謎語": [morpheme("謎語", reading="ナゾゴ", oov=True)]})
        assert a.tokens("謎語")[0]["reading"] == "謎語"

    def test_empty_tokens_are_skipped(self):
        a, _ = analyzer_for({"…": [morpheme("…", "補助記号"), morpheme("", "補助記号")]})
        assert [t["surface"] for t in a.tokens("…")] == ["…"]

    def test_form_only_shown_for_inflecting_words(self):
        a, _ = analyzer_for({"本": [morpheme("本", form="終止形-一般", reading="ホン")]})
        assert a.tokens("本")[0]["form_en"] == ""

    def test_analyze_builds_hiragana_line_and_grammar(self):
        sentences = {"雨かも": [morpheme("雨", reading="アメ"), morpheme("かも", "助詞", "副助詞")]}
        a, _ = analyzer_for(sentences, patterns=[GrammarPattern("かも", "〜かも", "maybe")])
        r = a.analyze("雨かも")
        assert r["text"] == "雨かも" and r["hiragana"] == "あめかも"
        assert [g["name"] for g in r["grammar"]] == ["〜かも"]


class TestBubbleReader:
    def test_reads_the_image_then_analyzes_its_text(self):
        a, _ = analyzer_for({"本": [morpheme("本", reading="ホン")]})
        ocr = FakeOcr("本")
        assert BubbleReader(ocr, a).read("IMAGE")["text"] == "本"
        assert ocr.images == ["IMAGE"]


class FakeRepo:
    def __init__(self):
        self.word_calls = 0

    def words(self, query, include_names=False):
        self.word_calls += 1
        w = [WordEntry(("見る",), ("みる",), frozenset({"ichi1"}),
                       tuple(Sense((f"g{i}",), ("Ichidan verb",), ()) for i in range(6))),
             WordEntry(("観る",), ("みる",), frozenset(), (Sense(("watch",), ("Ichidan verb",), ()),))]
        names = [NameEntry(("見",), ("ケン",), ("Ken",)), NameEntry(("見",), ("ミル",), ("Miru",))]
        return w, (names if include_names else [])

    def kanji(self, char):
        return KanjiEntry(char, tuple(f"m{i}" for i in range(9)), ("ケン",) * 6, ("み.る",) * 6, 7) if char == "見" else None


class TestWordLookup:
    def test_best_entry_first_and_senses_trimmed(self):
        out = WordLookup(FakeRepo()).lookup_word("見る", "みる", normalized="見る", pos="動詞")
        assert out[0]["kanji"] == ["見る"] and len(out[0]["senses"]) == 4
        assert out[0]["score"] > out[1]["score"]

    def test_names_come_first_ranked_by_reading(self):
        out = WordLookup(FakeRepo()).lookup_word("見", "みる", names=True)
        assert [e["senses"][0]["gloss"] for e in out[:2]] == ["Miru", "Ken"]
        assert out[0]["senses"][0]["pos"] == "name"

    def test_results_are_cached(self):
        repo = FakeRepo()
        lookup = WordLookup(repo)
        lookup.lookup_word("見る")
        lookup.lookup_word("見る")
        assert repo.word_calls == 1

    def test_kanji_details_trimmed(self):
        k = WordLookup(FakeRepo()).lookup_kanji("見")
        assert (len(k["meanings"]), len(k["on"]), len(k["kun"]), k["strokes"]) == (5, 4, 4, 7)
        assert WordLookup(FakeRepo()).lookup_kanji("x") is None


class TestLineExplainer:
    def make(self, chat=None):
        a, _ = analyzer_for({"本だ": [morpheme("本", reading="ホン"), morpheme("だ", "助動詞", "*")]})
        runtime = FakeRuntime()
        return LineExplainer(runtime, chat or FakeChat(("Tr", "anslation")), a, 0.3, 700), runtime

    def test_streams_the_model_answer(self):
        explainer, _ = self.make()
        assert "".join(explainer.explain("本だ")) == "Translation"

    def test_prompt_has_context_and_word_analysis(self):
        chat = FakeChat()
        explainer, _ = self.make(chat)
        list(explainer.explain("本だ", ["前の吹き出し"]))
        req = chat.requests[0]
        user = req["messages"][1]["content"]
        assert req["url"] == "http://model" and (req["temperature"], req["max_tokens"]) == (0.3, 700)
        assert "1. 前の吹き出し" in user and "Current bubble:\n本だ" in user
        assert "- 本 [ほん] (noun)" in user and "Word analysis" in user

    def test_model_is_held_only_while_streaming(self):
        explainer, runtime = self.make()
        stream = explainer.explain("本だ")
        assert runtime.sessions == 0           # nothing loads until the answer is read
        next(stream)
        assert runtime.open == 1
        list(stream)
        assert runtime.open == 0

    def test_session_released_on_error(self):
        explainer, runtime = self.make(FakeChat(("part",), error=RuntimeError("model crashed")))
        with pytest.raises(RuntimeError):
            list(explainer.explain("本だ"))
        assert runtime.open == 0
