// Keeps an instance's mods folder in line with its version's mod list.
//
// Mods the launcher installs ("managed") are recorded in mods/.festvisuals-mods.json, so updates
// replace exactly those files and anything the player added by hand is left alone.

import fs from 'fs';
import path from 'path';
import { VERSIONS } from './versions.js';
import { getJson, download } from './net.js';
import { readZipEntry } from './unzip.js';

const STATE_FILE = '.festvisuals-mods.json';
const REFRESH_EVERY_MS = 12 * 60 * 60 * 1000;

function readState(modsDir) {
  try {
    return JSON.parse(fs.readFileSync(path.join(modsDir, STATE_FILE), 'utf8'));
  } catch {
    return { resolved: {}, managed: [], resolvedAt: 0 };
  }
}

/** The jar's Fabric mod id, or null when it is not a Fabric mod (or unreadable). */
function modId(jar) {
  try {
    const json = readZipEntry(jar, 'fabric.mod.json');
    return json ? JSON.parse(json.toString('utf8')).id || null : null;
  } catch {
    return null;
  }
}

/** Newest Fabric build of a Modrinth project for one game version. */
async function resolveLatest(slug, gameVersion) {
  const query = new URLSearchParams({ loaders: '["fabric"]', game_versions: JSON.stringify([gameVersion]) });
  const versions = await getJson(`https://api.modrinth.com/v2/project/${slug}/version?${query}`, { timeout: 15000 });
  const latest = versions[0];
  if (!latest) return null;
  const file = latest.files.find((f) => f.primary) || latest.files[0];
  return { version: latest.version_number, filename: file.filename, url: file.url, sha1: file.hashes.sha1, size: file.size };
}

function jarsIn(dir) {
  try {
    return fs.readdirSync(dir).filter((name) => name.endsWith('.jar'));
  } catch {
    return [];
  }
}

/** Brings <gameDir>/mods in line with the version's mod list; `festvisualsJar` goes in for 26.2. */
export async function ensureMods(gameDir, version, { festvisualsJar, progress }) {
  const cfg = VERSIONS[version];
  const modsDir = path.join(gameDir, 'mods');
  fs.mkdirSync(modsDir, { recursive: true });
  const state = readState(modsDir);

  // Ask Modrinth for newer builds twice a day; offline, the last known files are used.
  const stale = Date.now() - (state.resolvedAt || 0) > REFRESH_EVERY_MS || cfg.mods.some((slug) => !state.resolved[slug]);
  if (stale) {
    await Promise.all(cfg.mods.map(async (slug) => {
      try {
        const latest = await resolveLatest(slug, version);
        if (latest) state.resolved[slug] = latest;
      } catch {
        // keep the previous answer
      }
    }));
    state.resolvedAt = Date.now();
  }
  if (!state.resolved['fabric-api']) throw new Error(`Не удалось получить Fabric API для ${version} — проверь интернет`);

  const keep = new Set();
  const ids = new Set();
  const files = cfg.mods.map((slug) => state.resolved[slug]).filter(Boolean);
  for (const [i, file] of files.entries()) {
    const target = path.join(modsDir, file.filename);
    if (!fs.existsSync(target) || fs.statSync(target).size !== file.size) {
      progress('mods', Math.round((i / files.length) * 100));
      await download(file.url, target, file.sha1);
    }
    file.id = file.id || modId(target);
    if (file.id) ids.add(file.id);
    keep.add(file.filename);
  }
  progress('mods', 100);

  // Our own files that are no longer wanted (an older build after an update).
  for (const name of state.managed || []) {
    if (!keep.has(name)) fs.rmSync(path.join(modsDir, name), { force: true });
  }
  // A stray second copy of a managed mod (e.g. an old fabric-api.jar) would load twice.
  for (const name of jarsIn(modsDir)) {
    if (keep.has(name) || /^festvisuals.*\.jar$/i.test(name)) continue;
    const id = modId(path.join(modsDir, name));
    if (id && ids.has(id)) fs.rmSync(path.join(modsDir, name), { force: true });
  }

  if (cfg.festvisuals && festvisualsJar && fs.existsSync(festvisualsJar)) {
    const target = path.join(modsDir, 'festvisuals.jar');
    for (const name of jarsIn(modsDir)) {
      if (/^festvisuals.*\.jar$/i.test(name) && name !== 'festvisuals.jar') fs.rmSync(path.join(modsDir, name), { force: true });
    }
    // Newer source (a fresh download from the feed) or a different size: replace the copy.
    const source = fs.statSync(festvisualsJar);
    const current = fs.existsSync(target) ? fs.statSync(target) : null;
    if (!current || current.size !== source.size || source.mtimeMs > current.mtimeMs) {
      fs.copyFileSync(festvisualsJar, target);
    }
  }

  state.managed = [...keep];
  fs.writeFileSync(path.join(modsDir, STATE_FILE), JSON.stringify(state, null, 2));
}
