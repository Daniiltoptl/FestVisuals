// Starts the local Jarvis voice server when the mod asks for it (local processing mode).
//
// The server's Python sources ship inside the launcher (resources/jarvis); in development they are
// used straight from the repository. Python itself and the server's requirements must be installed
// on the machine — when they are not, the reason is passed back so the mod can show it in chat.

import fs from 'fs';
import path from 'path';
import { spawn } from 'child_process';

const CANDIDATES = process.platform === 'win32'
  ? [['pythonw', []], ['pyw', ['-3']], ['python', []], ['py', ['-3']]]
  : [['python3', []], ['python', []]];

let child = null;
let startedAt = 0;
let lastError = '';
let tail = '';

function serverDir(app, electronDir) {
  const dirs = [
    process.env.FESTVISUALS_JARVIS_DIR,
    app.isPackaged ? path.join(process.resourcesPath, 'jarvis') : null,
    path.join(electronDir, '..', '..', 'jarvis-server'),
  ].filter(Boolean);
  return dirs.find((dir) => fs.existsSync(path.join(dir, 'server.py'))) || null;
}

function trySpawn(index, dir, env, resolve) {
  if (index >= CANDIDATES.length) {
    resolve({ ok: false, message: 'Python не найден — установи Python 3.11+ (python.org) и зависимости jarvis-server' });
    return;
  }
  const [cmd, args] = CANDIDATES[index];
  let proc;
  try {
    proc = spawn(cmd, [...args, 'server.py'], { cwd: dir, env, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  } catch {
    trySpawn(index + 1, dir, env, resolve);
    return;
  }

  let settled = false;
  proc.once('error', () => {
    if (settled) return;
    settled = true;
    trySpawn(index + 1, dir, env, resolve);
  });
  proc.once('spawn', () => {
    if (settled) return;
    settled = true;
    child = proc;
    startedAt = Date.now();
    tail = '';
    const keep = (data) => { tail = (tail + data.toString()).slice(-2000); };
    proc.stdout.on('data', keep);
    proc.stderr.on('data', keep);
    proc.once('exit', (code) => {
      if (child === proc) child = null;
      // An exit within seconds is a startup failure (missing module, Store "python" stub, ...).
      if (Date.now() - startedAt < 15000) {
        const line = tail.trim().split(/\r?\n/).filter(Boolean).pop() || `код выхода ${code}`;
        lastError = `локальный сервер Jarvis не запустился: ${line}`;
      }
    });
    resolve({ ok: true, message: 'starting' });
  });
}

/** Starts the server unless it is already running; resolves with what to tell the mod. */
export function startJarvis(app, electronDir) {
  if (child) return Promise.resolve({ ok: true, message: 'running' });
  if (lastError && Date.now() - startedAt < 20000) {
    const message = lastError;
    lastError = '';
    return Promise.resolve({ ok: false, message });
  }

  const dir = serverDir(app, electronDir);
  if (!dir) return Promise.resolve({ ok: false, message: 'не найдены файлы jarvis-server (переустанови лаунчер)' });

  const env = { ...process.env, JARVIS_HOST: '127.0.0.1', PYTHONIOENCODING: 'utf-8' };
  // A launcher-installed copy has no .env with the API keys next to it; use the user's own file.
  const envFile = path.join(app.getPath('userData'), 'jarvis.env');
  if (fs.existsSync(envFile)) env.JARVIS_ENV_FILE = envFile;

  return new Promise((resolve) => trySpawn(0, dir, env, resolve));
}

export function stopJarvis() {
  if (child) child.kill();
  child = null;
}
