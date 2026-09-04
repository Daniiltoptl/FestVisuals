# Jarvis voice server

Backend for the mod's **Jarvis** module (Разное tab). Hold a bound key in-game, speak, let go —
the mod streams the recording here, this works out what was meant, and sends back a spoken reply
plus a list of actions the mod carries out: running a server command, sending a chat message, or
toggling another module.

Designed for a small CPU-only VPS (no GPU needed):

- **Speech-to-text** — [faster-whisper](https://github.com/SYSTRAN/faster-whisper), local, CPU,
  quantized. Runs entirely on the VPS, no network call.
- **Understanding** — known Minecraft phrases ("тепнись на хом", "открой аук", "включи трейлс")
  are matched instantly by `intents.py`, no network call. Anything else falls through to
  [Groq](https://groq.com/) (`brain.py`), whose hosted inference is fast enough that a 70B model
  still replies in well under a second — that's the "smart and fast" part this hardware alone
  can't do.
- **Speech synthesis** — [Piper](https://github.com/rhasspy/piper), local, CPU, near-instant for
  a short sentence.

Only the utterances Groq actually sees leave the VPS; teleport/module commands that match a known
phrase never touch the network.

## Setup

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

**Piper** (binary + a Russian voice — pick any voice from the
[Piper voices list](https://github.com/rhasspy/piper/blob/master/VOICES.md), `irina` used below
as an example):

```bash
mkdir -p piper && cd piper
wget https://github.com/rhasspy/piper/releases/latest/download/piper_linux_x86_64.tar.gz
tar xzf piper_linux_x86_64.tar.gz --strip-components=1
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/ru/ru_RU/irina/medium/ru_RU-irina-medium.onnx
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/ru/ru_RU/irina/medium/ru_RU-irina-medium.onnx.json
cd ..
```

**Config:**

```bash
cp .env.example .env
```

Edit `.env`:
- `GROQ_API_KEY` — free key from https://console.groq.com/keys (no card needed)
- `PIPER_BIN` / `PIPER_VOICE` — paths from the step above
- `JARVIS_TOKEN` — set this to a random string if the port will be reachable from the open
  internet. The mod has a matching "Токен доступа" field in the module's settings. Without a
  token, anyone who finds the port can make the mod send chat messages and commands as you.

**Run it:**

```bash
python server.py
```

## Running as a service

```bash
sudo mkdir -p /opt/festvisuals-jarvis
sudo cp -r . /opt/festvisuals-jarvis
sudo useradd -r -s /usr/sbin/nologin festvisuals || true
sudo chown -R festvisuals:festvisuals /opt/festvisuals-jarvis
sudo cp festvisuals-jarvis.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now festvisuals-jarvis
sudo journalctl -u festvisuals-jarvis -f
```

## In the mod

Jarvis module → **Адрес сервера**: `ws://<vps-ip>:8765/jarvis`, **Токен доступа**: same value as
`JARVIS_TOKEN` if you set one, then bind a key and hold it to talk.

## Tuning

- `WHISPER_MODEL`: `tiny`/`base` for lower latency, `small` (default) balanced, `medium` slower
  but more accurate on 4 cores.
- `GROQ_MODEL`: `llama-3.1-8b-instant` for the lowest latency, `llama-3.3-70b-versatile` (default)
  for smarter answers on anything a fixed phrase doesn't cover.
- `intents.py` — add more fixed phrases here for your server's actual commands (its real
  `/home`, `/ah`, etc. may not match the defaults); anything added here answers instantly and
  never calls Groq.
- `brain.py`'s `SYSTEM_PROMPT` — edit if you want it to know about server-specific commands or
  behave differently.

## Firewall

Only the Jarvis port needs to be reachable, and only from wherever the game client actually
connects from. If that's always the same machine, prefer binding to a VPN/SSH-tunnel address (or
`127.0.0.1` plus an SSH `-L` tunnel from the client machine) over opening the port to
`0.0.0.0`/the public internet, token or not.
