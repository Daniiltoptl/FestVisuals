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
