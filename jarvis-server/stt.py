"""
Speech-to-text via faster-whisper, on the GPU when asked and available, otherwise the CPU.

The client always records 16kHz mono PCM (see JarvisAudioCapture on the mod side), which is
exactly what Whisper wants, so this never has to touch ffmpeg or any audio container.

GPU notes: faster-whisper runs on CTranslate2, which needs NVIDIA's cuBLAS and cuDNN libraries.
`pip install nvidia-cublas-cu12 nvidia-cudnn-cu12` provides them; on Windows their DLL folders
are not on the search path by default, so they are registered here before the model loads.
If the GPU cannot be used for any reason the model falls back to the CPU and the reason is
reported back to the player instead of failing silently.
"""

import glob
import logging
import os
import sys

import numpy as np

log = logging.getLogger("jarvis.stt")

# What the mod's "Модель STT" setting means. Heavy is only sensible on a GPU.
MODEL_SIZES = {
    "light": "small",      # base understands Russian too poorly to be worth the speed
    "medium": "medium",
    "heavy": "large-v3-turbo",
}

_models: dict[tuple[str, str], object] = {}
_gpu_problem: str | None = None
_dll_dirs_added = False
_broken_gpu: list[str] = []


def _register_cuda_dlls() -> None:
    """Make pip-installed cuBLAS/cuDNN DLLs findable on Windows."""
    global _dll_dirs_added
    if _dll_dirs_added or sys.platform != "win32":
        return
    _dll_dirs_added = True

    for site in sys.path:
        for pattern in ("nvidia/cublas/bin", "nvidia/cudnn/bin", "nvidia/cuda_runtime/bin"):
            for folder in glob.glob(os.path.join(site, pattern)):
                try:
                    os.add_dll_directory(folder)
                    os.environ["PATH"] = folder + os.pathsep + os.environ.get("PATH", "")
                except OSError:
                    pass


def _cuda_available() -> tuple[bool, str | None]:
    _register_cuda_dlls()
    try:
        import ctranslate2
        if ctranslate2.get_cuda_device_count() > 0:
            return True, None
        return False, "видеокарта NVIDIA с CUDA не найдена"
    except Exception as e:  # noqa: BLE001
        return False, f"CUDA недоступна ({e})"


def _load(size: str, device: str):
    key = (size, device)
    if key in _models:
        return _models[key]

    from faster_whisper import WhisperModel

    compute = "float16" if device == "cuda" else os.environ.get("WHISPER_COMPUTE", "int8")
    log.info("loading whisper model %s on %s (%s)", size, device, compute)
    model = WhisperModel(size, device=device, compute_type=compute)
    _models[key] = model
    return model


def resolve(model_choice: str | None, use_gpu: bool) -> tuple[str, str, str | None]:
    """Picks (model size, device, warning) for this request."""
    size = MODEL_SIZES.get((model_choice or "").lower(), os.environ.get("WHISPER_MODEL", "small"))
    if not use_gpu:
        return size, "cpu", None

    ok, problem = _cuda_available()
    if ok and _broken_gpu:
        ok, problem = False, _short(RuntimeError(_broken_gpu[-1]))
    if ok:
        return size, "cuda", None
    # A large model on the CPU would take ages; step down so the reply still comes quickly.
    if size == MODEL_SIZES["heavy"]:
        size = MODEL_SIZES["medium"]
    return size, "cpu", f"GPU не используется: {problem}, работаю на процессоре"


def transcribe(pcm_bytes: bytes, sample_rate: int, model_choice: str | None = None,
               use_gpu: bool = False, vocabulary: str | None = None) -> tuple[str, str | None]:
    """Returns (text, warning). The warning is set when the GPU was requested but not used."""
    global _gpu_problem
    audio = np.frombuffer(pcm_bytes, dtype="<i2").astype(np.float32) / 32768.0
    if sample_rate != 16000:
        audio = _resample(audio, sample_rate, 16000)

    size, device, warning = resolve(model_choice, use_gpu)
    try:
        model = _load(size, device)
    except Exception as e:  # noqa: BLE001 — typically missing cuDNN at load time
        if device != "cuda":
            raise
        log.warning("GPU model failed to load, using CPU: %s", e)
        warning = f"GPU не запустился ({e.__class__.__name__}), работаю на процессоре"
        device = "cpu"
        model = _load(size if size != MODEL_SIZES["heavy"] else MODEL_SIZES["medium"], device)

    language = os.environ.get("WHISPER_LANGUAGE", "ru")
    try:
        text = _run(model, audio, language, device, vocabulary)
    except RuntimeError as e:
        # Missing cuBLAS/cuDNN only surfaces here, on the first real decode, not at load time.
        if device != "cuda":
            raise
        log.warning("GPU decode failed, using CPU: %s", e)
        _models.pop((size, device), None)
        _broken_gpu.append(str(e))
        warning = f"GPU не запустился ({_short(e)}), работаю на процессоре"
        device = "cpu"
        model = _load(size if size != MODEL_SIZES["heavy"] else MODEL_SIZES["medium"], device)
        text = _run(model, audio, language, device, vocabulary)

    # Report a GPU problem once per change rather than on every utterance.
    if warning == _gpu_problem:
        warning = None
    else:
        _gpu_problem = warning
    return text, warning


def _run(model, audio: np.ndarray, language: str, device: str, vocabulary: str | None = None) -> str:
    # Segments are produced lazily; joining them is what actually runs the decoder. The prompt
    # primes Whisper with the words it is about to hear — command verbs and module names.
    segments, _ = model.transcribe(audio, language=language, beam_size=1 if device == "cpu" else 5,
                                   vad_filter=True, initial_prompt=vocabulary)
    return "".join(segment.text for segment in segments).strip()


def _short(error: Exception) -> str:
    text = str(error)
    if "cublas" in text.lower():
        return "нет библиотеки cuBLAS — установи requirements-gpu.txt"
    if "cudnn" in text.lower():
        return "нет библиотеки cuDNN — установи requirements-gpu.txt"
    return error.__class__.__name__


def _resample(audio: np.ndarray, src_rate: int, dst_rate: int) -> np.ndarray:
    duration = len(audio) / src_rate
    dst_len = max(1, int(duration * dst_rate))
    src_idx = np.linspace(0, len(audio) - 1, dst_len)
    return np.interp(src_idx, np.arange(len(audio)), audio).astype(np.float32)
