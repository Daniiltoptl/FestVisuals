"""
Decides what a transcript means.

Known Minecraft phrases are handled by intents.py with no network call. Everything else — free
chat, a command this file doesn't recognise, a question — goes to Groq, which is fast enough on
their hosted hardware that this still feels close to instant even though the VPS itself has no
GPU to run a model that smart on its own.
"""

import json
import logging
import os

from groq import Groq

from intents import INTENTS, MODULE_NAMES

log = logging.getLogger("jarvis.brain")

_client: Groq | None = None


def _groq() -> Groq:
    global _client
    if _client is None:
        key = os.environ.get("GROQ_API_KEY")
        if not key:
            raise RuntimeError("GROQ_API_KEY is not set")
        _client = Groq(api_key=key)
    return _client


SYSTEM_PROMPT = f"""Ты — голосовой ассистент Jarvis внутри Minecraft-мода FestVisuals.
Пользователь произносит короткую голосовую команду на русском, часто с игровым сленгом.
Отвечай СТРОГО в виде JSON без пояснений и без markdown, в формате:

{{"reply": "короткий голосовой ответ на русском, одно предложение", "actions": [...]}}

actions — список из нуля или более действий:
- {{"type": "command", "value": "home"}} — выполнить команду сервера (без ведущего слэша)
- {{"type": "chat", "text": "..."}} — написать сообщение в игровой чат
- {{"type": "module", "name": "<точное имя из списка ниже>", "enabled": true}} — включить/выключить модуль клиента

Доступные модули (имя указывай ТОЧНО как в списке): {", ".join(MODULE_NAMES)}.

Сленг: "тепнись"/"телепортируй" — команда телепорта (home/spawn/tpa и т.д.), "хом"/"база" — дом,
"аук" — аукцион (команда ah). Если не понял, чего хочет пользователь — переспроси в reply и
оставь actions пустым, не выдумывай команду. Если просьба не про Minecraft — просто ответь в
reply, actions оставь пустым."""


def think(transcript: str) -> tuple[str, list[dict]]:
    text = transcript.strip()
    if not text:
        return "Не расслышал, повтори", []

    for intent in INTENTS:
        result = intent.try_match(text)
        if result:
            return result

    return _ask_groq(text)


def _ask_groq(text: str) -> tuple[str, list[dict]]:
    model = os.environ.get("GROQ_MODEL", "llama-3.3-70b-versatile")

    try:
        response = _groq().chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": text},
            ],
            response_format={"type": "json_object"},
            temperature=0.4,
            max_tokens=300,
        )
    except Exception as e:  # noqa: BLE001 — surfaced to the user as a spoken/chat error either way
        log.exception("Groq request failed")
        return f"Ошибка запроса к нейронке: {e}", []

    raw = response.choices[0].message.content or ""
    try:
        data = json.loads(raw)
        reply = str(data.get("reply", "")).strip() or "Готово"
        return reply, _sanitize_actions(data.get("actions"))
    except (ValueError, AttributeError):
        log.warning("Groq returned non-JSON despite json_object mode: %r", raw)
        return raw.strip()[:200] or "Не понял", []


def _sanitize_actions(actions) -> list[dict]:
    """Never trust the model's shape blindly — a malformed action must not reach the client."""
    if not isinstance(actions, list):
        return []

    clean = []
    for action in actions:
        if not isinstance(action, dict):
            continue

        kind = action.get("type")
        if kind == "command" and isinstance(action.get("value"), str):
            clean.append({"type": "command", "value": action["value"]})
        elif kind == "chat" and isinstance(action.get("text"), str):
            clean.append({"type": "chat", "text": action["text"]})
        elif kind == "module" and isinstance(action.get("name"), str) and isinstance(action.get("enabled"), bool):
            clean.append({"type": "module", "name": action["name"], "enabled": action["enabled"]})
    return clean
