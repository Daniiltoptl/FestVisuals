// Everything needed to get from "Play" to a running game, with no Electron dependency so it can be
// run and tested from plain Node.
//
// On a clean machine this installs Mojang's own Java runtime, the Fabric profile, Fabric API and the
// bundled FestVisuals jar, then hands over to minecraft-launcher-core for the vanilla files. Later
// launches only check that files exist, so the wait is the game's own start-up.

import fs from 'fs';
import os from 'os';
import path from 'path';
import https from 'https';
import crypto from 'crypto';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const { Client } = require('minecraft-launcher-core');
const Handler = require('minecraft-launcher-core/components/handler.js');

const MC_VERSION = '26.2';
const LOADER_VERSION = '0.19.5';
const PROFILE = `fabric-loader-${LOADER_VERSION}-${MC_VERSION}`;
const FABRIC_API_URL = 'https://cdn.modrinth.com/data/P7dR8mSH/versions/ewUK83HI/fabric-api-0.161.0%2B26.2.jar';
const JAVA_RUNTIMES = 'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json';
const JAVA_COMPONENT = 'java-runtime-epsilon'; // what Mojang ships for 26.2 (Java 25)
const PARALLEL = 32;
const FULL_CHECK_EVERY_MS = 7 * 24 * 60 * 60 * 1000;

// minecraft-launcher-core SHA-1 hashes every asset and library (thousands of files) on every
// launch. After one full verification the fast path only checks that each file exists — anything
// missing is still downloaded — and a full check runs again weekly or after a launch that did not
// reach the game window.
let fastVerify = false;
const originalCheckSum = Handler.prototype.checkSum;
Handler.prototype.checkSum = function (hash, file) {
  if (fastVerify) return Promise.resolve(fs.existsSync(file));
  return originalCheckSum.call(this, hash, file);
};

// The stock check parses `java -version` output and throws inside the callback when it doesn't
// look as expected (javaw prints nothing), which leaves the launch hanging forever.
Handler.prototype.checkJava = function (java) {
  if (path.isAbsolute(java)) {
    return Promise.resolve(fs.existsSync(java) ? { run: true } : { run: false, message: `${java} not found` });
  }
  return new Promise((resolve) => {
    require('child_process').execFile(java, ['-version'], (error) => {
      resolve(error ? { run: false, message: error.message } : { run: true });
    });
  });
};

function markerPath(root) { return path.join(root, '.festvisuals-verified'); }
function verifiedRecently(root) {
  try {
    return Date.now() - fs.statSync(markerPath(root)).mtimeMs < FULL_CHECK_EVERY_MS;
  } catch {
    return false;
  }
}

function defaultRoot() {
  const base = process.env.APPDATA || path.join(os.homedir(), '.local', 'share');
  return path.join(base, '.festvisuals');
}

/** The requested folder if it can be created (a missing D: drive can't), otherwise the default. */
function usableRoot(requested) {
  for (const candidate of [requested, defaultRoot()]) {
    if (!candidate) continue;
    try {
      fs.mkdirSync(candidate, { recursive: true });
      return path.resolve(candidate);
    } catch {
      // try the next one
    }
  }
  throw new Error('Не удалось создать папку игры');
}

function get(url, redirects = 5) {
  return new Promise((resolve, reject) => {
    const request = https.get(url, { headers: { 'User-Agent': 'FestVisuals-Launcher' }, timeout: 30000 }, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location && redirects > 0) {
        response.resume();
        resolve(get(new URL(response.headers.location, url).toString(), redirects - 1));
        return;
      }
      if (response.statusCode !== 200) {
        response.resume();
        reject(new Error(`HTTP ${response.statusCode} для ${url}`));
        return;
      }
      const chunks = [];
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => resolve(Buffer.concat(chunks)));
      response.on('error', reject);
    });
    request.on('timeout', () => request.destroy(new Error(`Таймаут: ${url}`)));
    request.on('error', reject);
  });
}

async function getJson(url) {
  return JSON.parse((await get(url)).toString('utf8'));
}

/** Downloads to a temporary name and renames, so an interrupted download never looks complete. */
async function download(url, dest, sha1) {
  let lastError;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const data = await get(url);
      if (sha1 && crypto.createHash('sha1').update(data).digest('hex') !== sha1) {
        throw new Error(`Повреждённый файл ${path.basename(dest)}`);
      }
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      const tmp = `${dest}.part`;
      fs.writeFileSync(tmp, data);
      fs.renameSync(tmp, dest);
      return;
    } catch (e) {
      lastError = e;
    }
  }
  throw lastError;
}

async function inParallel(items, limit, worker) {
  let next = 0;
  const runners = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) await worker(items[next++]);
  });
  await Promise.all(runners);
}

/** Mojang's own Java 25 build, unpacked once into <root>/runtime. Returns the javaw path. */
async function ensureJava(root, progress) {
  if (process.platform !== 'win32') return 'java';

  const home = path.join(root, 'runtime', JAVA_COMPONENT);
  const javaw = path.join(home, 'bin', 'javaw.exe');
  const done = path.join(home, '.complete');
  if (fs.existsSync(done) && fs.existsSync(javaw)) return javaw;

  const platform = os.arch() === 'arm64' ? 'windows-arm64' : 'windows-x64';
  const all = await getJson(JAVA_RUNTIMES);
  const entry = all[platform] && all[platform][JAVA_COMPONENT] && all[platform][JAVA_COMPONENT][0];
  if (!entry) throw new Error(`Нет Java для ${platform}`);
  const manifest = await getJson(entry.manifest.url);

  const files = Object.entries(manifest.files).filter(([, f]) => f.type === 'file');
  let finished = 0;
  progress('java', 0);
  await inParallel(files, PARALLEL, async ([relative, file]) => {
    const dest = path.join(home, relative);
    const raw = file.downloads.raw;
    if (!fs.existsSync(dest) || fs.statSync(dest).size !== raw.size) {
      await download(raw.url, dest, raw.sha1);
    }
    progress('java', Math.round((++finished / files.length) * 100));
  });
  fs.writeFileSync(done, entry.version.name);
  return javaw;
}

/** The Fabric version profile minecraft-launcher-core reads as a "custom" version. */
async function ensureFabricProfile(root) {
  const file = path.join(root, 'versions', PROFILE, `${PROFILE}.json`);
  if (fs.existsSync(file)) return;
  const profile = await get(`https://meta.fabricmc.net/v2/versions/loader/${MC_VERSION}/${LOADER_VERSION}/profile/json`);
  JSON.parse(profile.toString('utf8')); // refuse to save an error page
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, profile);
}

/** Fabric API plus the FestVisuals jar shipped inside the launcher, replacing older copies. */
async function ensureMods(root, modJar, progress) {
  const mods = path.join(root, 'mods');
  fs.mkdirSync(mods, { recursive: true });

  const fabricApi = path.join(mods, 'fabric-api.jar');
  if (!fs.existsSync(fabricApi)) {
    progress('fabric', 0);
    await download(FABRIC_API_URL, fabricApi);
  }

  if (!modJar || !fs.existsSync(modJar)) return;
  const target = path.join(mods, path.basename(modJar));
  for (const name of fs.readdirSync(mods)) {
    if (/^festvisuals.*\.jar$/i.test(name) && name !== path.basename(modJar)) {
      fs.rmSync(path.join(mods, name), { force: true });
    }
  }
  if (!fs.existsSync(target) || fs.statSync(target).size !== fs.statSync(modJar).size) {
    fs.copyFileSync(modJar, target);
  }
}

/** Assets are content-addressed, so an existing vanilla install can lend its copies. */
function sharedAssets() {
  const vanilla = path.join(process.env.APPDATA || os.homedir(), '.minecraft', 'assets');
  return fs.existsSync(path.join(vanilla, 'objects')) ? vanilla : undefined;
}

/**
 * Prepares everything and starts the game.
 * emit(channel, payload) reports: launch-progress {type, percent}, launch-success, game-ready,
 * game-closed (code), launch-error (message).
 */
export async function launchGame({ root: requestedRoot, ram, username, modJar }, emit) {
  // One message per visible change: thousands of per-file updates only clog the IPC channel.
  const shown = {};
  const progress = (type, percent) => {
    if (shown[type] === percent) return;
    shown[type] = percent;
    emit('launch-progress', { type, percent });
  };
  const root = usableRoot(requestedRoot);

  const [javaPath] = await Promise.all([
    ensureJava(root, progress),
    ensureFabricProfile(root),
    ensureMods(root, modJar, progress),
  ]);

  const launcher = new Client();
  let windowOpen = false;
  let closed = false;
  let lastLines = [];
  launcher.on('progress', (e) => {
    progress(e.type, e.total ? Math.round((e.task / e.total) * 100) : 0);
  });
  launcher.on('data', (line) => {
    const text = String(line);
    lastLines = lastLines.concat(text.split(/\r?\n/).filter(Boolean)).slice(-20);
    // LWJGL reports its backend as the game window is created: from here the player sees the game.
    if (!windowOpen && (text.includes('Backend library:') || text.includes('Sound engine started'))) {
      windowOpen = true;
      try { fs.writeFileSync(markerPath(root), String(Date.now())); } catch {}
      emit('game-ready');
    }
  });
  launcher.on('debug', (line) => {
    lastLines = lastLines.concat(String(line)).slice(-20);
  });
  launcher.on('close', (code) => {
    closed = true;
    if (!windowOpen) {
      // A launch that never opened the window may have hit a broken file: verify fully next time.
      try { fs.rmSync(markerPath(root), { force: true }); } catch {}
      if (code !== 0) emit('launch-error', `Игра закрылась (код ${code}).\n${lastLines.slice(-6).join('\n')}`);
    }
    emit('game-closed', code);
  });

  fastVerify = verifiedRecently(root);
  const process_ = await launcher.launch({
    authorization: {
      access_token: '0',
      client_token: '0',
      uuid: '00000000-0000-0000-0000-000000000000',
      name: username || 'Player',
      user_properties: '{}',
      meta: { type: 'offline', demo: false },
    },
    root,
    javaPath,
    version: { number: MC_VERSION, type: 'release', custom: PROFILE },
    memory: { max: `${ram}M`, min: `${Math.max(512, Math.floor(ram / 2))}M` },
    // Minecraft's start-up crash-report preview asks OSHI for process counters; where Windows'
    // counter registry is damaged that falls back to WMI and stalls the window for up to minutes.
    // The mod sets the same properties, this covers the launch before it loads.
    customArgs: ['-Doshi.os.windows.perfproc.disabled=true', '-Doshi.util.wmi.timeout=2000'],
    overrides: {
      maxSockets: PARALLEL,
      assetRoot: sharedAssets(),
    },
  });
  if (!process_) {
    // A failed Java check is reported through 'close'; any other failure only leaves a debug line.
    if (!closed) throw new Error(lastLines.slice(-3).join('\n') || 'Не удалось запустить игру');
    return root;
  }
  // The process is up, but the window takes a while: the UI says "starting" until game-ready.
  emit('launch-success');
  return root;
}
