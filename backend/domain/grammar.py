"""Grammar-pattern matching on an analyzed line. The patterns themselves are data (repositories)."""
import re
from dataclasses import dataclass, field

# Parts of speech after which an `after_verb` pattern may start.
VERBLIKE = frozenset({"動詞", "形容詞", "助動詞"})


@dataclass(frozen=True)
class GrammarPattern:
    regex: str
    name: str
    explanation: str
    after_verb: bool = False
    compiled: re.Pattern = field(init=False, repr=False, compare=False)

    def __post_init__(self):
        object.__setattr__(self, "compiled", re.compile(self.regex))


class GrammarMatcher:
    """Finds grammar patterns in a line. New patterns are added as data, without changing this class."""

    def __init__(self, patterns):
        self.patterns = list(patterns)

    def find(self, text, tokens):
        """tokens: list of (surface, pos1) covering `text`. Returns [{name, match, explanation, at}] in text order.

        A match only counts if it starts at a word boundary and, for `after_verb` patterns,
        right after a verb, adjective or auxiliary (avoids hits like the でる inside 出る).
        """
        starts, prev_pos, pos = {}, None, 0
        for surface, pos1 in tokens:
            starts[pos] = prev_pos
            pos += len(surface)
            prev_pos = pos1
        found, seen = [], set()
        for p in self.patterns:
            for m in p.compiled.finditer(text):
                if m.start() not in starts:
                    continue
                if p.after_verb and starts[m.start()] not in VERBLIKE:
                    continue
                if p.name not in seen:
                    seen.add(p.name)
                    found.append({"name": p.name, "match": m.group(0), "explanation": p.explanation,
                                  "at": m.start()})
        return sorted(found, key=lambda f: f["at"])
