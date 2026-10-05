// Puts the current mod into mod/festvisuals.jar, which electron-builder ships as a resource: the
// launcher's offline fallback when it cannot reach the mod feed on first start.
//
// Source order: the public releases repo (what CI publishes; no token needed), then the private
// repo's latest release (token from GITHUB_TOKEN or the origin remote), then the newest local build.
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { execSync } from 'child_process';

const FEED = 'https://github.com/Daniiltoptl/FestVisuals-Releases/releases/download/mod/';
const OUT = path.join('mod', 'festvisuals.jar');

function save(buffer) {
  fs.mkdirSync('mod', { recursive: true });
  fs.writeFileSync(OUT, buffer);
}

async function fromFeed() {
  const meta = await (await fetch(FEED + 'mod.json')).json();
  const jar = Buffer.from(await (await fetch(FEED + meta.file)).arrayBuffer());
  if (crypto.createHash('sha1').update(jar).digest('hex') !== meta.sha1) throw new Error('checksum mismatch');
  save(jar);
  return `${meta.file} ${meta.version} from the public feed`;
}

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

async function fromPrivateRelease() {
  const auth = token();
  if (!auth) return null;
  const headers = { Authorization: `Bearer ${auth}`, 'User-Agent': 'festvisuals-launcher-build' };
  const info = await (await fetch('https://api.github.com/repos/Daniiltoptl/FestVisuals/releases/latest', {
    headers: { ...headers, Accept: 'application/vnd.github+json' },
  })).json();
  const asset = info.assets.find((a) => a.name.endsWith('.jar') && !a.name.includes('-sources'));
  if (!asset) return null;
  const file = await fetch(asset.url, { headers: { ...headers, Accept: 'application/octet-stream' } });
  if (!file.ok) return null;
  save(Buffer.from(await file.arrayBuffer()));
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
  save(fs.readFileSync(jars[0]));
  return `${path.basename(jars[0])} from the local build`;
}

let source = null;
for (const attempt of [fromFeed, fromPrivateRelease]) {
  try {
    source = await attempt();
    if (source) break;
  } catch (e) {
    console.warn(`${attempt.name} failed: ${e.message}`);
  }
}
source = source || fromLocalBuild();
if (!source) {
  console.error('No mod jar available from the feed, the releases or a local build.');
  process.exit(1);
}
console.log(`Bundled ${source}`);
