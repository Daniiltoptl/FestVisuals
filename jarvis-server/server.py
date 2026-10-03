"""
Websocket front door for the mod's Jarvis module.

One utterance is a JSON text frame ({"type": "utterance", "sampleRate": ..., "token": ...})
immediately followed by one binary frame of raw PCM. The reply mirrors that shape: a JSON text
frame, then a WAV binary frame if there's something to say. See JarvisClient.java on the mod side
for the exact contract.
"""

import asyncio
import json
import logging
import os

import websockets
from dotenv import load_dotenv

import brain
import stt
import tts

load_dotenv()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("jarvis.server")


async def handle(websocket):
    peer = websocket.remote_address
    log.info("client connected: %s", peer)

    try:
        async for header_raw in websocket:
            if not isinstance(header_raw, str):
                continue  # a stray binary frame with no header ahead of it

            try:
                header = json.loads(header_raw)
            except ValueError:
                continue
            if header.get("type") != "utterance":
                continue

            expected_token = os.environ.get("JARVIS_TOKEN") or None
            if expected_token and header.get("token") != expected_token:
                await websocket.send(json.dumps({"type": "error", "message": "bad token"}))
                continue

            try:
                audio = await asyncio.wait_for(websocket.recv(), timeout=5)
            except (asyncio.TimeoutError, websockets.ConnectionClosed):
                break
            if not isinstance(audio, (bytes, bytearray)):
                continue

            await _process(websocket, bytes(audio), header)
    except websockets.ConnectionClosed:
        pass
    finally:
        log.info("client disconnected: %s", peer)


def _vocabulary(context) -> str:
    """Words Whisper should expect: the verbs Jarvis understands and every module name."""
    words = ["Джарвис, включи, выключи, загрузи конфиг, поставь тему, тепнись на хом, спавн, аукцион."]
    if isinstance(context, dict):
        names = [m.get("name", "") for m in context.get("modules", []) if isinstance(m, dict)]
        if names:
            words.append("Модули: " + ", ".join(names) + ".")
    return " ".join(words)[:800]


async def _process(websocket, audio: bytes, header: dict) -> None:
    loop = asyncio.get_running_loop()
    sample_rate = int(header.get("sampleRate", 16000))

    try:
        transcript, stt_warning = await loop.run_in_executor(
            None, stt.transcribe, audio, sample_rate, header.get("sttModel"), bool(header.get("useGpu")),
            _vocabulary(header.get("context")))
    except Exception as e:  # noqa: BLE001
        log.exception("stt failed")
        await websocket.send(json.dumps({"type": "error", "message": f"STT: {e}"}))
        return

    log.info("transcript: %r", transcript)

    try:
        context = header.get("context") if isinstance(header.get("context"), dict) else None
        reply, actions = await loop.run_in_executor(None, brain.think, transcript, context)
    except Exception as e:  # noqa: BLE001
        log.exception("brain failed")
        await websocket.send(json.dumps({"type": "error", "message": f"brain: {e}"}))
        return

    wav = None
    if reply and header.get("voice", True):
        try:
            wav = await loop.run_in_executor(None, tts.synthesize, reply)
        except Exception:  # noqa: BLE001
            log.exception("tts failed")

    await websocket.send(json.dumps({
        "type": "result",
        "transcript": transcript,
        "reply": reply if not stt_warning else f"{reply} ({stt_warning})",
        "actions": actions,
        "audio": wav is not None,
    }))
    if wav:
        await websocket.send(wav)


async def main() -> None:
    host = os.environ.get("JARVIS_HOST", "0.0.0.0")
    port = int(os.environ.get("JARVIS_PORT", "8765"))

    log.info("Jarvis server listening on %s:%s", host, port)
    async with websockets.serve(handle, host, port, max_size=32 * 1024 * 1024):
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
