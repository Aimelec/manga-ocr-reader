"""Regression: dictionary lookups must work from any thread (the server answers requests on a thread pool)."""
import threading

import pytest

from backend.config import load_settings
from backend.repositories.jmdict import JmdictRepository

pytestmark = pytest.mark.golden   # needs dict/jamdict.db


def test_lookups_from_different_threads():
    repo = JmdictRepository(load_settings().dict_db)
    errors = []

    def lookup(word):
        try:
            words, _ = repo.words(word, include_names=True)
            assert words
            assert repo.kanji(word[0]) is not None
        except Exception as e:      # collected: assertions in threads do not fail the test by themselves
            errors.append(e)

    first = threading.Thread(target=lookup, args=("見る",))
    first.start()
    first.join()
    others = [threading.Thread(target=lookup, args=(w,)) for w in ("食べる", "学校", "猫", "走る")]
    for t in others:
        t.start()
    for t in others:
        t.join()
    lookup("部屋")                  # and from the main thread
    assert errors == []
