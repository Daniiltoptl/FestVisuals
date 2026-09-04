"""
Speech-to-text via faster-whisper, CPU-only.

The client always records 16kHz mono PCM (see JarvisAudioCapture on the mod side), which is
exactly what Whisper wants, so this never has to touch ffmpeg or any audio container — just a
raw int16 buffer straight off the wire.
"""

import logging
import os

import numpy as np
from faster_whisper import WhisperModel

log = logging.getLogger("jarvis.stt")

_model: WhisperModel | None = None


def _load() -> WhisperModel:
    global _model
    if _model is None:
        name = os.environ.get("WHISPER_MODEL", "small")
        compute = os.environ.get("WHISPER_COMPUTE", "int8")
        log.info("loading whisper model %s (compute_type=%s)", name, compute)
        _model = WhisperModel(name, device="cpu", compute_type=compute)
    return _model


def transcribe(pcm_bytes: bytes, sample_rate: int) -> str:
    audio = np.frombuffer(pcm_bytes, dtype="<i2").astype(np.float32) / 32768.0

    if sample_rate != 16000:
        # The mod always sends 16kHz; this only guards against a future client that doesn't.
        audio = _resample(audio, sample_rate, 16000)

    language = os.environ.get("WHISPER_LANGUAGE", "ru")
    segments, _ = _load().transcribe(audio, language=language, beam_size=1, vad_filter=True)
    return "".join(segment.text for segment in segments).strip()


def _resample(audio: np.ndarray, src_rate: int, dst_rate: int) -> np.ndarray:
    duration = len(audio) / src_rate
    dst_len = max(1, int(duration * dst_rate))
    src_idx = np.linspace(0, len(audio) - 1, dst_len)
    return np.interp(src_idx, np.arange(len(audio)), audio).astype(np.float32)
