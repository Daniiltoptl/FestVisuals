// Puts the mod into mod/festvisuals.jar, which electron-builder ships as a resource and the
// launcher installs into the game's mods folder.
//
// The latest GitHub release is preferred (the repository is private, so a token comes from
// GITHUB_TOKEN or the origin remote URL); without one, the newest local build is used.
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const REPO = 'Daniiltoptl/FestVisuals';
const OUT = path.join('mod', 'festvisuals.jar');

function token() {
  if (process.env.GITHUB_TOKEN) return process.env.GITHUB_TOKEN;
  try {
    const remote = execSync('git config --get remote.origin.url', { encoding: 'utf8' });
    const match = remote.match(/(gh[pousr]_[A-Za-z0-9]+|github_pat_[A-Za-z0-9_]+)/);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}

async function fromRelease() {
  const auth = token();
  if (!auth) return null;
  const headers = { Authorization: `Bearer ${auth}`, 'User-Agent': 'festvisuals-launcher-build' };
  const release = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
    headers: { ...headers, Accept: 'application/vnd.github+json' },
  });
  if (!release.ok) throw new Error(`release lookup failed: HTTP ${release.status}`);
  const info = await release.json();
  const asset = info.assets.find((a) => a.name.endsWith('.jar') && !a.name.includes('-sources'));
  if (!asset) throw new Error(`release ${info.tag_name} has no jar`);
  const file = await fetch(asset.url, { headers: { ...headers, Accept: 'application/octet-stream' } });
  if (!file.ok) throw new Error(`jar download failed: HTTP ${file.status}`);
  fs.mkdirSync('mod', { recursive: true });
  fs.writeFileSync(OUT, Buffer.from(await file.arrayBuffer()));
  return `${asset.name} from release ${info.tag_name}`;
}

function fromLocalBuild() {
  const libs = path.resolve('..', 'visuals', 'build', 'libs');
  const jars = fs.existsSync(libs)
    ? fs.readdirSync(libs)
        .filter((name) => name.endsWith('.jar') && !/-(sources|dev|javadoc)\.jar$/.test(name))
        .map((name) => path.join(libs, name))
        .sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs)
    : [];
  if (jars.length === 0) return null;
  fs.mkdirSync('mod', { recursive: true });
  fs.copyFileSync(jars[0], OUT);
  return `${path.basename(jars[0])} from the local build`;
}

let source = null;
try {
  source = await fromRelease();
} catch (e) {
  console.warn(`GitHub release unavailable (${e.message}), using the local build.`);
}
source = source || fromLocalBuild();
if (!source) {
  console.error('No mod jar: no GitHub release reachable and nothing in visuals/build/libs.');
  process.exit(1);
}
console.log(`Bundled ${source}`);
