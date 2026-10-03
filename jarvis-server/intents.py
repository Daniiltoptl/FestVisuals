"""
Fast, local matching for common Minecraft voice commands.

Anything caught here answers in a few milliseconds without a network round trip — it's what
makes "тепнись на хом" feel instant. Only speech that doesn't match anything below falls
through to the LLM in brain.py.

Modules are matched against what the mod reports with each utterance: names *and* their
descriptions, so "выключи цветное небо" finds Sky Color ("Меняет цвет неба") without the
player knowing what the module is called. When nothing matches with confidence the phrase is
left to the LLM rather than guessed.
"""

import difflib
import re

# Used only when an older client sends no module list.
MODULE_NAMES = [
    "Trails", "Crosshair", "Block Highlight", "ChinaHat", "Motion Blur", "Particles",
    "Removals", "Swing Animation", "View Model", "Jump Circle", "Ambience",
    "Auto Accept", "Auto Auth", "Auto Eat", "Auto Invisible", "Auto Resell", "Auto Sprint",
    "Death Cords", "Fast Scroller", "Zoom", "Name Protect", "Sounds",
    "Potions", "Cooldowns", "Binds", "Target HUD", "Armor", "Dynamic Island",
    "Inventory", "Scoreboard", "Stats", "Saturation", "Click GUI",
]

_TRANSLIT = str.maketrans({
    "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "е": "e", "ё": "e", "ж": "zh", "з": "z",
    "и": "i", "й": "i", "к": "k", "л": "l", "м": "m", "н": "n", "о": "o", "п": "p", "р": "r",
    "с": "s", "т": "t", "у": "u", "ф": "f", "х": "h", "ц": "c", "ч": "ch", "ш": "sh", "щ": "sh",
    "ъ": "", "ы": "i", "ь": "", "э": "e", "ю": "u", "я": "ya",
})

# Filler that says nothing about which module is meant.
_STOP = {"мне", "мой", "мою", "мои", "это", "этот", "эту", "все", "всё", "пожалуйста", "плиз",
         "модуль", "функцию", "функция", "штуку", "штука", "на", "в", "и", "с", "по", "у", "а"}


def _words(text: str) -> list[str]:
    return [w for w in re.findall(r"[a-zа-яё0-9]+", text.lower()) if len(w) >= 3 and w not in _STOP]


def _same_root(a: str, b: str) -> bool:
    """Russian words differ in their endings ("небо"/"неба"); compare the shared start."""
    prefix = 0
    for x, y in zip(a, b):
        if x != y:
            break
        prefix += 1
    return prefix >= 3 and prefix >= 0.6 * min(len(a), len(b))


def _name_score(spoken: str, name: str) -> float:
    spoken_l = spoken.lower().strip()
    name_l = name.lower()
    if spoken_l == name_l:
        return 1.0
    latin = spoken_l.translate(_TRANSLIT).replace(" ", "")
    plain = name_l.replace(" ", "")
    direct = difflib.SequenceMatcher(None, latin, plain).ratio()
    # English spelling vs how it sounds in Russian: "zoom" is said "зум", "sky" is "скай".
    sounded = difflib.SequenceMatcher(None, _phonetic(latin), _phonetic(plain)).ratio()
    return max(direct, sounded)


def _phonetic(text: str) -> str:
    for a, b in (("oo", "u"), ("ee", "i"), ("ck", "k"), ("ph", "f"), ("y", "i"), ("w", "v"), ("x", "ks"),
                 ("ai", "i"), ("ei", "i")):
        text = text.replace(a, b)
    return text


def _desc_score(spoken: str, desc: str) -> float:
    asked = _words(spoken)
    if not asked:
        return 0.0
    have = _words(desc)
    hits = sum(1 for word in asked if any(_same_root(word, other) for other in have))
    return hits / len(asked)


def resolve_module(spoken: str, context: dict | None = None) -> str | None:
    """Best module for the spoken phrase, or None when no candidate is convincing."""
    modules = (context or {}).get("modules") or [{"name": n, "desc": ""} for n in MODULE_NAMES]

    best_name, best_score = None, 0.0
    for module in modules:
        name = module.get("name", "")
        score = max(_name_score(spoken, name), _desc_score(spoken, module.get("desc", "")) * 0.95)
        if score > best_score:
            best_name, best_score = name, score
    return best_name if best_score >= 0.62 else None


def _resolve_from(spoken: str, options: list[str]) -> str | None:
    spoken_l = spoken.strip().lower()
    for option in options:
        if option.lower() == spoken_l:
            return option
    lowered = {o.lower(): o for o in options}
    close = difflib.get_close_matches(spoken_l, lowered.keys(), n=1, cutoff=0.6)
    if not close:
        latin = spoken_l.translate(_TRANSLIT)
        close = difflib.get_close_matches(latin, lowered.keys(), n=1, cutoff=0.6)
    return lowered[close[0]] if close else None


class Intent:
    def __init__(self, pattern: str, handler):
        self.pattern = re.compile(pattern, re.IGNORECASE)
        self.handler = handler

    def try_match(self, text: str, context: dict | None):
        match = self.pattern.search(text)
        return self.handler(match, context) if match else None


def _home(_match, _context):
    return "Телепортирую домой", [{"type": "command", "value": "home"}]


def _spawn(_match, _context):
    return "Телепортирую на спавн", [{"type": "command", "value": "spawn"}]


def _auction(_match, _context):
    return "Открываю аукцион", [{"type": "command", "value": "ah"}]


def _toggle(enabled: bool):
    def handler(match, context):
        name = resolve_module(match.group(1), context)
        if not name:
            return None  # let the LLM have a go with the full module list
        return ("Включаю " if enabled else "Выключаю ") + name, [
            {"type": "module", "name": name, "enabled": enabled}
        ]

    return handler


def _config(operation: str):
    def handler(match, context):
        spoken = match.group(1).strip()
        if operation == "save":
            name = re.sub(r"[^\w\- ]", "", spoken)[:32].strip()
            if not name:
                return None
            return f"Сохраняю конфиг {name}", [{"type": "config", "action": "save", "name": name}]
        name = _resolve_from(spoken, (context or {}).get("configs") or [])
        if not name:
            return f"Не нашёл конфиг {spoken}", []
        return f"Загружаю конфиг {name}", [{"type": "config", "action": "load", "name": name}]

    return handler


def _theme(match, context):
    spoken = match.group(1).strip()
    name = _resolve_from(spoken, (context or {}).get("themes") or [])
    if not name:
        return f"Не нашёл тему {spoken}", []
    return f"Ставлю тему {name}", [{"type": "theme", "name": name}]


def _say(match, _context):
    text = match.group(1).strip()
    if not text:
        return "Что написать?", []
    return text, [{"type": "chat", "text": text}]


# Order matters: specific phrases before the generic toggle/say catch-alls.
INTENTS = [
    Intent(r"\b(?:теп\w*|телепорт\w*)\b.*\b(?:хом\w*|дом\w*|баз\w*)\b|^\s*домой\s*$", _home),
    Intent(r"\b(?:теп\w*|телепорт\w*)\b.*\bспавн\w*\b|^\s*спавн\w*\s*$", _spawn),
    Intent(r"\b(?:открой|открывай|запусти)\b.*\b(?:аук\w*|ah)\b", _auction),
    Intent(r"\b(?:загрузи|загрузить|включи|поставь)\s+конфиг\w*\s+(.+)", _config("load")),
    Intent(r"\b(?:сохрани|сохранить)\s+конфиг\w*\s+(?:как\s+)?(.+)", _config("save")),
    Intent(r"\b(?:поставь|включи|смени\s+на|сделай)\s+тем\w*\s+(.+)", _theme),
    # Stems rather than exact verbs: speech recognition happily turns "выключи" into "выключится".
    Intent(r"\b(?:выкл\w*|выруб\w*|отключ\w*|деактив\w*|убер\w*)\s+(.+)", _toggle(False)),
    Intent(r"\b(?:вкл\w*|вруб\w*|актив\w*)\s+(.+)", _toggle(True)),
    Intent(r"\b(?:напиши|скажи)\b(?:\s+в\s+чат)?\s+(.+)", _say),
]
