"""
Decides what a transcript means.

Known Minecraft phrases are handled by intents.py with no network call. Everything else — free
chat, a command this file doesn't recognise, a question — goes to Groq, which is fast enough on
their hosted hardware that this still feels close to instant even though the VPS itself has no
GPU to run a model that smart on its own.

The mod sends what exists on the client with every utterance (modules with descriptions and
setting names, configs, themes); the prompt is built from that, so the model can turn plain
speech into the right module or setting even when the player never says its name.
"""

import json
import logging
import os

from intents import INTENTS, MODULE_NAMES

log = logging.getLogger("jarvis.brain")

_client = None

# Fastest model that follows the setting names exactly; the others are tried if it disappears.
DEFAULT_MODEL = "qwen/qwen3.8-27b"
FALLBACK_MODELS = ["qwen/qwen3.8-27b", "openai/gpt-oss-120b", "openai/gpt-oss-20b"]


def _groq():
    """Imported lazily: fixed phrases work without the groq package or a key installed at all."""
    global _client
    if _client is None:
        try:
            from groq import Groq
        except ImportError as e:
            raise RuntimeError("пакет groq не установлен (pip install groq)") from e
        key = os.environ.get("GROQ_API_KEY")
        if not key:
            raise RuntimeError("не задан GROQ_API_KEY")
        _client = Groq(api_key=key)
    return _client


RULES = """СПЕЦИАЛЬНЫЕ ПРАВИЛА:
1. Если игрок просит кинуть репорт на кого-то, кто его бьет или кто рядом, используй плейсхолдер %nearest% или %attacker% в качестве ника. Например: {"type": "command", "value": "report %nearest% 1.1"}
2. КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО выполнять любую автоматизацию игры: добывать алмазы, строить, копать, снайпить аукцион, автоматически ходить или фармить. Если игрок просит об этом, верни пустой список actions и скажи в reply, что ты отказываешься выполнять макросы/автоматизацию.
3. Команды пишутся без слэша (не /home, а home).
4. Модуль ищи по смыслу описания: "цветное небо" — модуль с описанием про цвет неба, "цифры урона" — модуль про урон. Имя модуля в action пиши ТОЧНО как в списке.
5. Настройку меняй только у существующего модуля и только существующую настройку из списка; для mode-настройки value — один из вариантов, для number — число в пределах min..max, для bool — true/false, для color — "#RRGGBB".
6. Если не понял — переспроси в reply, actions оставь пустым, не выдумывай."""

ACTIONS = """actions — массив действий:
- {"type": "command", "value": "home"} — выполнить команду сервера (без слэша)
- {"type": "chat", "text": "..."} — написать сообщение в чат
- {"type": "module", "name": "<модуль>", "enabled": true} — включить/выключить модуль
- {"type": "setting", "module": "<модуль>", "setting": "<настройка>", "value": <значение>} — изменить настройку модуля
- {"type": "config", "action": "load" | "save", "name": "<конфиг>"} — загрузить или сохранить конфиг
- {"type": "theme", "name": "<тема>"} — поставить тему интерфейса"""


def _describe_context(context: dict | None) -> str:
    if not context or not context.get("modules"):
        return "Модули: " + ", ".join(MODULE_NAMES) + "."

    lines = ["Модули (имя — описание — состояние — настройки):"]
    for module in context["modules"]:
        settings = []
        for s in module.get("settings", []):
            kind = s.get("type")
            if kind == "mode":
                settings.append(f'{s.get("name")} [{"/".join(s.get("options", []))}]')
            elif kind == "number":
                settings.append(f'{s.get("name")} ({s.get("min")}..{s.get("max")})')
            else:
                settings.append(f'{s.get("name")} ({kind})')
        state = "вкл" if module.get("on") else "выкл"
        line = f'- {module.get("name")} — {module.get("desc", "")} — {state}'
        if settings:
            line += " — " + "; ".join(settings)
        lines.append(line)

    configs = context.get("configs") or []
    themes = context.get("themes") or []
    lines.append("Конфиги: " + (", ".join(configs) if configs else "нет"))
    lines.append("Темы: " + (", ".join(themes) if themes else "нет") + f' (сейчас: {context.get("theme", "?")})')
    return "\n".join(lines)


def _system_prompt(context: dict | None) -> str:
    return (
        "Ты — голосовой ассистент Jarvis внутри Minecraft-клиента FestVisuals.\n"
        "Пользователь диктует тебе текст через микрофон. Пойми, чего он хочет, и верни JSON.\n"
        "Отвечай СТРОГО JSON без markdown в формате:\n"
        '{"reply": "Краткий ответ для синтеза речи (1-2 предложения)", "actions": [...]}\n\n'
        f"{ACTIONS}\n\n{_describe_context(context)}\n\n{RULES}"
    )


def think(transcript: str, context: dict | None = None) -> tuple[str, list[dict]]:
    text = transcript.strip()
    if not text:
        return "Не распознано, повторите", []

    for intent in INTENTS:
        result = intent.try_match(text, context)
        if result:
            return result

    return _ask_groq(text, context)


def _ask_groq(text: str, context: dict | None) -> tuple[str, list[dict]]:
    # Groq retires models without notice; when the preferred one is gone, the next one answers.
    models = [os.environ.get("GROQ_MODEL", DEFAULT_MODEL)] + [m for m in FALLBACK_MODELS if m != os.environ.get("GROQ_MODEL")]

    response, error = None, None
    for model in models:
        try:
            response = _groq().chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": _system_prompt(context)},
                    {"role": "user", "content": text},
                ],
                response_format={"type": "json_object"},
                temperature=0.3,
                max_tokens=600,
            )
            break
        except Exception as e:  # noqa: BLE001 — surfaced to the user as a spoken/chat error either way
            error = e
            log.warning("Groq request with %s failed: %s", model, e)
            if getattr(e, "status_code", None) not in (400, 404):
                break  # network or auth trouble: another model won't help
    if response is None:
        return f"Ошибка связи с нейросетью: {error}", []

    raw = response.choices[0].message.content or ""
    try:
        data = json.loads(raw)
        reply = str(data.get("reply", "")).strip() or "Готово"
        return reply, _sanitize_actions(data.get("actions"), context)
    except (ValueError, AttributeError):
        log.warning("Groq returned non-JSON despite json_object mode: %r", raw)
        return raw.strip()[:200] or "Не распознано", []


def _known(context: dict | None, key: str) -> set[str] | None:
    if not context or key not in context:
        return None
    if key == "modules":
        return {m.get("name", "").lower() for m in context["modules"]}
    return {str(v).lower() for v in context[key]}


def _sanitize_actions(actions, context: dict | None = None) -> list[dict]:
    """Never trust the model's shape blindly — a malformed action must not reach the client."""
    if not isinstance(actions, list):
        return []

    modules = _known(context, "modules")
    themes = _known(context, "themes")
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
            if modules is None or action["name"].lower() in modules:
                clean.append({"type": "module", "name": action["name"], "enabled": action["enabled"]})
        elif kind == "setting" and isinstance(action.get("module"), str) and isinstance(action.get("setting"), str) \
                and isinstance(action.get("value"), (str, int, float, bool)):
            if modules is None or action["module"].lower() in modules:
                clean.append({"type": "setting", "module": action["module"], "setting": action["setting"],
                              "value": action["value"]})
        elif kind == "config" and action.get("action") in ("load", "save") and isinstance(action.get("name"), str):
            clean.append({"type": "config", "action": action["action"], "name": action["name"][:32]})
        elif kind == "theme" and isinstance(action.get("name"), str):
            if themes is None or action["name"].lower() in themes:
                clean.append({"type": "theme", "name": action["name"]})
    return clean
