// Default game folder: %APPDATA%\.festvisuals, which exists on every Windows machine (a fixed
// drive letter such as D: does not). The main process falls back to the same folder.
export function defaultGameDir(): string {
  const req = (window as any).require;
  if (!req) return '';
  const path = req('path');
  const env = req('process').env;
  return path.join(env.APPDATA || req('os').homedir(), '.festvisuals');
}

export function gameDir(): string {
  return localStorage.getItem('settings_dir') || defaultGameDir();
}

/** One version's own folder (mods, configs, worlds) inside the game folder. */
export function instanceDir(version: string): string {
  const req = (window as any).require;
  if (!req) return '';
  return req('path').join(gameDir(), 'instances', version);
}

/** Total physical RAM in MB, rounded down to a 512 MB step; 8192 when it cannot be read. */
export function totalRamMb(): number {
  const req = (window as any).require;
  if (!req) return 8192;
  try {
    const bytes = req('os').totalmem();
    return Math.max(2048, Math.floor(bytes / 1024 / 1024 / 512) * 512);
  } catch {
    return 8192;
  }
}
