"""
Text-to-speech via Piper, run as a subprocess.

Piper ships as a standalone binary plus an .onnx voice model rather than a pip package with
pinned onnxruntime versions to fight with, and on 4 CPU cores it synthesizes a short sentence
in well under a second — see README.md for where to get the binary and a Russian voice.
"""

import logging
import os
import subprocess
import tempfile

log = logging.getLogger("jarvis.tts")


def synthesize(text: str) -> bytes | None:
    text = text.strip()
    if not text:
        return None

    piper_bin = os.environ.get("PIPER_BIN", "piper")
    voice = os.environ.get("PIPER_VOICE")
    if not voice:
        log.warning("PIPER_VOICE is not set; the reply will be silent")
        return None

    with tempfile.NamedTemporaryFile(suffix=".wav") as out:
        try:
            subprocess.run(
                [piper_bin, "--model", voice, "--output_file", out.name],
                input=text.encode("utf-8"),
                check=True,
                capture_output=True,
                timeout=15,
            )
        except (subprocess.CalledProcessError, FileNotFoundError, subprocess.TimeoutExpired) as e:
            log.error("piper failed: %s", e)
            return None

        return out.read()
