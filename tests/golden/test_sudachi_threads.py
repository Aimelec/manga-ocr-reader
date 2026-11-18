"""Regression: the tokenizer must work when several requests analyze text at the same time."""
import threading

import pytest

from backend.adapters.sudachi import SudachiTokenizer

pytestmark = pytest.mark.golden   # needs the Sudachi dictionary


def test_concurrent_tokenizing():
    tok = SudachiTokenizer()
    errors = []

    def work():
        try:
            for _ in range(200):
                for m in tok.tokenize("国際宇宙開発機構の発表を聞いた。明日は雪が降るかもしれない。"):
                    tok.split(m)
        except Exception as e:      # collected: exceptions in threads do not fail the test by themselves
            errors.append(e)

    threads = [threading.Thread(target=work) for _ in range(8)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    assert errors == []
