// Small HTTP helpers shared by the game installer, the mod manager and the mod feed: redirects,
// timeouts, SHA-1 verification and write-then-rename so an interrupted download never looks done.

import fs from 'fs';
import path from 'path';
import https from 'https';
import crypto from 'crypto';

export const PARALLEL = 32;

export function get(url, { redirects = 5, timeout = 30000 } = {}) {
  return new Promise((resolve, reject) => {
    const request = https.get(url, { headers: { 'User-Agent': 'FestVisuals-Launcher (festvisuals.pro)' }, timeout }, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location && redirects > 0) {
        response.resume();
        resolve(get(new URL(response.headers.location, url).toString(), { redirects: redirects - 1, timeout }));
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

export async function getJson(url, options) {
  return JSON.parse((await get(url, options)).toString('utf8'));
}

/** Downloads to a temporary name and renames, retrying a few times; checks SHA-1 when given. */
export async function download(url, dest, sha1) {
  let lastError;
  for (let attempt = 0; attempt < 4; attempt++) {
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
      await new Promise((r) => setTimeout(r, 400 * (attempt + 1)));
    }
  }
  throw lastError;
}

export async function inParallel(items, limit, worker) {
  let next = 0;
  const runners = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) await worker(items[next++]);
  });
  await Promise.all(runners);
}
