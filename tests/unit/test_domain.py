"""Pure domain logic: kana, furigana alignment, grammar matching, entry ranking."""
import pytest

from backend.domain.furigana import split_ruby
from backend.domain.grammar import GrammarMatcher, GrammarPattern
from backend.domain.japanese import has_kanji, kata_to_hira
from backend.domain.ranking import Query, score_name, score_word
from backend.repositories.jmdict import NameEntry, Sense, WordEntry


class TestKana:
    def test_katakana_becomes_hiragana(self):
        assert kata_to_hira("タベル") == "たべる"

    def test_other_characters_are_unchanged(self):
        assert kata_to_hira("食べるABCー") == "食べるABCー"

    @pytest.mark.parametrize("text, expected", [("食べる", True), ("人々", True), ("たべる", False), ("ABC", False)])
    def test_has_kanji(self, text, expected):
        assert has_kanji(text) is expected


class TestFurigana:
    def test_reading_only_over_the_kanji(self):
        assert split_ruby("食べる", "たべる") == [("食", "た"), ("べる", "")]

    def test_kanji_on_both_sides_of_kana(self):
        assert split_ruby("引き出し", "ひきだし") == [("引", "ひ"), ("き", ""), ("出", "だ"), ("し", "")]

    def test_all_kanji_word_keeps_whole_reading(self):
        assert split_ruby("学校", "がっこう") == [("学校", "がっこう")]

    def test_kana_word_has_no_furigana(self):
        assert split_ruby("ありがとう", "ありがとう") == [("ありがとう", "")]

    def test_no_reading(self):
        assert split_ruby("学校", "") == [("学校", "")]

    def test_katakana_okurigana_matches_hiragana_reading(self):
        assert split_ruby("見タ", "みた") == [("見", "み"), ("タ", "")]

    def test_mismatch_falls_back_to_whole_word(self):
        # irregular reading where the kana part does not appear in the reading
        assert split_ruby("お母さん", "おかあさま") == [("お母さん", "おかあさま")]


class TestGrammarMatcher:
    matcher = GrammarMatcher([
        GrammarPattern(r"ている", "〜ている", "ongoing", after_verb=True),
        GrammarPattern(r"かも", "〜かも", "maybe"),
    ])

    def test_after_verb_pattern_matches_after_a_verb(self):
        hits = self.matcher.find("食べている", [("食べ", "動詞"), ("て", "助詞"), ("いる", "動詞")])
        assert hits == [{"name": "〜ている", "match": "ている", "explanation": "ongoing", "at": 2}]

    def test_after_verb_pattern_ignored_after_a_noun(self):
        assert self.matcher.find("手ている", [("手", "名詞"), ("て", "助詞"), ("いる", "動詞")]) == []

    def test_match_must_start_at_a_word_boundary(self):
        # the 'かも' in the middle of the word 'しかも' ('moreover') is not the 'maybe' pattern
        assert self.matcher.find("しかも", [("しかも", "接続詞")]) == []

    def test_results_are_in_text_order_and_unique(self):
        tokens = [("雨", "名詞"), ("かも", "助詞"), ("、", "補助記号"), ("降っ", "動詞"), ("ている", "助動詞"),
                  ("かも", "助詞")]
        hits = self.matcher.find("雨かも、降っているかも", tokens)
        assert [h["name"] for h in hits] == ["〜かも", "〜ている"]

    def test_new_patterns_are_just_data(self):
        m = GrammarMatcher([GrammarPattern(r"はず", "〜はず", "should be")])
        assert m.find("来るはず", [("来る", "動詞"), ("はず", "名詞")])[0]["name"] == "〜はず"


def word(kanji=(), kana=(), pos=("noun",), pri=(), misc=()):
    return WordEntry(tuple(kanji), tuple(kana), frozenset(pri), (Sense(("gloss",), tuple(pos), tuple(misc)),))


class TestRanking:
    def test_standard_spelling_scores_highest(self):
        assert score_word(word(kanji=["見る"]), Query(normalized="見る")) == 4

    def test_reading_compared_in_hiragana(self):
        assert score_word(word(kana=["ミル"]), Query(reading="みる")) == 2

    def test_part_of_speech_hint(self):
        e = word(pos=["Ichidan verb", "transitive verb"])
        assert score_word(e, Query(pos="動詞")) == 2
        assert score_word(e, Query(pos="名詞")) == 0

    def test_common_word_bonus(self):
        assert score_word(word(pri=["news1"]), Query()) == 1
        assert score_word(word(pri=["nf20"]), Query()) == 0

    def test_usually_kana_only_counts_for_kana_surfaces(self):
        e = word(misc=["word usually written using kana alone"])
        assert score_word(e, Query(kana_surface=True)) == 1
        assert score_word(e, Query(kana_surface=False)) == 0

    def test_rules_add_up(self):
        e = word(kanji=["見る"], kana=["みる"], pos=["Ichidan verb"], pri=["ichi1"])
        assert score_word(e, Query(reading="みる", normalized="見る", pos="動詞")) == 4 + 2 + 2 + 1

    def test_custom_rules_extend_the_ranking(self):
        assert score_word(word(), Query(), rules=[lambda e, q: 7]) == 7

    def test_name_reading(self):
        n = NameEntry(("田中",), ("タナカ",), ("Tanaka",))
        assert score_name(n, "たなか") == 2
        assert score_name(n, "") == 0
