"""Sudachi part-of-speech groups used by the analyzer."""

CONTENT_POS = frozenset({"名詞", "代名詞", "動詞", "形容詞", "形状詞", "副詞", "連体詞", "接続詞", "感動詞",
                         "接頭辞", "接尾辞"})
FUNCTION_POS = frozenset({"助詞", "助動詞"})       # particles and auxiliaries: explained from a table
INFLECTING_POS = frozenset({"動詞", "形容詞", "助動詞"})   # show their conjugation form
PROPER_NOUN = "固有名詞"                            # second level of the part of speech
