"""
Text-to-speech via Piper, run as a subprocess.

If PIPER_BIN / PIPER_VOICE are not configured, Piper and a Russian voice are fetched once into
./piper on first use — from the official Piper GitHub release and the rhasspy/piper-voices
repository on Hugging Face — so the locally launched Jarvis can speak without any manual setup.
"""

import io
import logging
import os
import subprocess
import sys
import tarfile
import tempfile
import threading
import urllib.request
import zipfile

log = logging.getLogger("jarvis.tts")

HERE = os.path.dirname(os.path.abspath(__file__))
PIPER_DIR = os.path.join(HERE, "piper")

PIPER_RELEASE = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/"
PIPER_ARCHIVES = {
    "win32": "piper_windows_amd64.zip",
    "linux": "piper_linux_x86_64.tar.gz",
}
VOICE_NAME = os.environ.get("PIPER_VOICE_NAME", "ru_RU-dmitri-medium")
VOICE_BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main/ru/ru_RU/{speaker}/{quality}/{name}"

_setup_lock = threading.Lock()
_setup_failed = False


def _download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "festvisuals-jarvis"})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def _local_binary() -> str:
    name = "piper.exe" if sys.platform == "win32" else "piper"
    for root, _dirs, files in os.walk(PIPER_DIR):
        if name in files:
            return os.path.join(root, name)
    return ""


def _ensure_local_piper() -> tuple[str, str]:
    """Returns (binary, voice) inside ./piper, downloading whatever is missing."""
    global _setup_failed
    with _setup_lock:
        os.makedirs(PIPER_DIR, exist_ok=True)

        binary = _local_binary()
        if not binary:
            archive = PIPER_ARCHIVES.get(sys.platform)
            if archive is None:
                raise RuntimeError(f"no Piper build for {sys.platform}")
            log.info("downloading Piper (%s)", archive)
            data = _download(PIPER_RELEASE + archive)
            if archive.endswith(".zip"):
                zipfile.ZipFile(io.BytesIO(data)).extractall(PIPER_DIR)
            else:
                tarfile.open(fileobj=io.BytesIO(data), mode="r:gz").extractall(PIPER_DIR, filter="data")
            binary = _local_binary()
            if not binary:
                raise RuntimeError("Piper archive had no binary")
            if sys.platform != "win32":
                os.chmod(binary, 0o755)

        voice = os.path.join(PIPER_DIR, VOICE_NAME + ".onnx")
        if not os.path.exists(voice) or not os.path.exists(voice + ".json"):
            _lang, speaker, quality = VOICE_NAME.split("-", 2)
            for suffix in (".onnx", ".onnx.json"):
                url = VOICE_BASE.format(speaker=speaker, quality=quality, name=VOICE_NAME + suffix)
                log.info("downloading voice %s", VOICE_NAME + suffix)
                with open(os.path.join(PIPER_DIR, VOICE_NAME + suffix), "wb") as out:
                    out.write(_download(url))
        return binary, voice


def _piper() -> tuple[str, str] | None:
    global _setup_failed
    binary = os.environ.get("PIPER_BIN")
    voice = os.environ.get("PIPER_VOICE")
    if binary and voice and os.path.exists(voice):
        return binary, voice
    if _setup_failed:
        return None
    try:
        return _ensure_local_piper()
    except Exception as e:  # noqa: BLE001
        log.error("could not set up Piper: %s", e)
        _setup_failed = True
        return None


def synthesize(text: str) -> bytes | None:
    text = text.strip()
    if not text:
        return None

    piper = _piper()
    if piper is None:
        return None
    binary, voice = piper

    out_path = os.path.join(tempfile.gettempdir(), f"jarvis_{os.getpid()}_{threading.get_ident()}.wav")
    try:
        subprocess.run(
            [binary, "--model", voice, "--output_file", out_path],
            input=text.encode("utf-8"),
            check=True,
            capture_output=True,
            timeout=30,
            creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0,
        )
        with open(out_path, "rb") as wav:
            return wav.read()
    except (subprocess.CalledProcessError, FileNotFoundError, subprocess.TimeoutExpired, OSError) as e:
        log.error("piper failed: %s", e)
        return None
    finally:
        try:
            os.remove(out_path)
        except OSError:
            pass
