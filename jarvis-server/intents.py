"""
Fast, local matching for common Minecraft voice commands.

Anything caught here answers in a few milliseconds without a network round trip to Groq —
it's what makes "тепнись на хом" feel instant. Only speech that doesn't match anything below
falls through to the LLM in brain.py.
"""

import difflib
import re

# Keep this in sync with the @ModuleRegister names in the mod (ModuleManager.java).
MODULE_NAMES = [
    "Trails", "Crosshair", "Block Highlight", "ChinaHat", "Motion Blur", "Particles",
    "Removals", "Swing Animation", "View Model", "Jump Circle", "Ambience",
    "Auto Accept", "Auto Auth", "Auto Eat", "Auto Invisible", "Auto Resell", "Auto Sprint",
    "Death Cords", "Fast Scroller", "Zoom", "Name Protect", "Sounds",
    "Potions", "Cooldowns", "Binds", "Target HUD", "Armor", "Dynamic Island",
    "Inventory", "Scoreboard", "Stats", "Saturation", "Click GUI", "Animations",
]

_LOWER_NAMES = {name.lower(): name for name in MODULE_NAMES}


def resolve_module(spoken: str) -> str | None:
    """Matches spoken module names loosely: 'трейлс', 'trails', a typo — whatever is close."""
    key = spoken.strip().lower()
    if key in _LOWER_NAMES:
        return _LOWER_NAMES[key]

    close = difflib.get_close_matches(key, _LOWER_NAMES.keys(), n=1, cutoff=0.6)
    return _LOWER_NAMES[close[0]] if close else None


class Intent:
    def __init__(self, pattern: str, handler):
        self.pattern = re.compile(pattern, re.IGNORECASE)
        self.handler = handler

    def try_match(self, text: str):
        match = self.pattern.search(text)
        return self.handler(match) if match else None


def _home(_match):
    return "Телепортирую домой", [{"type": "command", "value": "home"}]


def _spawn(_match):
    return "Телепортирую на спавн", [{"type": "command", "value": "spawn"}]


def _auction(_match):
    return "Открываю аукцион", [{"type": "command", "value": "ah"}]


def _toggle(enabled: bool):
    def handler(match):
        raw = match.group(1).strip()
        name = resolve_module(raw)
        if not name:
            return f"Не нашёл модуль {raw}", []
        return ("Включаю " if enabled else "Выключаю ") + name, [
            {"type": "module", "name": name, "enabled": enabled}
        ]

    return handler


def _say(match):
    text = match.group(1).strip()
    if not text:
        return "Что написать?", []
    return text, [{"type": "chat", "text": text}]


# Order matters: specific phrases before the generic toggle/say catch-alls.
INTENTS = [
    Intent(r"\b(?:теп\w*|телепорт\w*)\b.*\b(?:хом\w*|дом\w*|баз\w*)\b|^\s*домой\s*$", _home),
    Intent(r"\b(?:теп\w*|телепорт\w*)\b.*\bспавн\w*\b|^\s*спавн\w*\s*$", _spawn),
    Intent(r"\b(?:открой|открывай|запусти)\b.*\b(?:аук\w*|ah)\b", _auction),
    Intent(r"\b(?:включи|врубай|активируй)\b\s+(.+)", _toggle(True)),
    Intent(r"\b(?:выключи|вырубай|деактивируй)\b\s+(.+)", _toggle(False)),
    Intent(r"\b(?:напиши|скажи)\b(?:\s+в\s+чат)?\s+(.+)", _say),
]
