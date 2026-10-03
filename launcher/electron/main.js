import { app, BrowserWindow, ipcMain } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import http from 'http';
import { spawn } from 'child_process';
import { Client } from 'minecraft-launcher-core';
import fs from 'fs';
import https from 'https';
import { createRequire } from 'module';

// minecraft-launcher-core SHA-1 hashes every asset and library (thousands of files) on every
// launch, which is most of the wait. After one full verification the fast path only checks that
// each file exists — anything missing is still downloaded — and a full check runs again weekly
// or after a launch that did not reach the game window.
const require = createRequire(import.meta.url);
const Handler = require('minecraft-launcher-core/components/handler.js');
const originalCheckSum = Handler.prototype.checkSum;
let fastVerify = false;
Handler.prototype.checkSum = function (hash, file) {
  if (fastVerify) return Promise.resolve(fs.existsSync(file));
  return originalCheckSum.call(this, hash, file);
};
const FULL_CHECK_EVERY_MS = 7 * 24 * 60 * 60 * 1000;
function markerPath(root) { return path.join(root, '.festvisuals-verified'); }
function verifiedRecently(root) {
  try {
    return Date.now() - fs.statSync(markerPath(root)).mtimeMs < FULL_CHECK_EVERY_MS;
  } catch {
    return false;
  }
}

const launcher = new Client();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let jarvisProcess = null;

// Start a local HTTP server so Minecraft can trigger Jarvis
const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  
  if (req.url === '/start-jarvis') {
    if (!jarvisProcess) {
      console.log('Starting Jarvis from Minecraft command...');
      const jarvisDir = path.join(process.cwd(), '../jarvis-server');
      jarvisProcess = spawn('python', ['server.py'], { cwd: jarvisDir });
      
      jarvisProcess.on('close', () => {
        jarvisProcess = null;
      });
    }
    res.writeHead(200);
    res.end('Jarvis started');
  } else if (req.url === '/stop-jarvis') {
    if (jarvisProcess) {
      console.log('Stopping Jarvis...');
      jarvisProcess.kill();
      jarvisProcess = null;
    }
    res.writeHead(200);
    res.end('Jarvis stopped');
  } else {
    res.writeHead(404);
    res.end('Not found');
  }
});

// Port 27515 or something. Let's use 27515 (default FestVisuals port maybe?) or 3000. 
// Actually I will just use 27515. Wait, 4567? I'll use 4567. 
// Loopback only: anything on the network (or any web page, given the CORS header) could
// otherwise start processes on this machine.
server.listen(4567, '127.0.0.1', () => {
  console.log('Jarvis IPC Server listening on port 4567');
});

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    title: 'FestVisuals Launcher',
    icon: path.join(__dirname, 'icon.jpg'),
    autoHideMenuBar: true,
    frame: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
    },
  });

  // In development, load the Vite dev server
  if (!app.isPackaged) {
    win.loadURL('http://localhost:8080');
  } else {
    // In production, load the built index.html
    win.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  ipcMain.on('minimize-window', () => {
    if (win) win.minimize();
  });
  
  ipcMain.on('close-window', () => {
    if (win) win.close();
  });

  let isLaunching = false;

  ipcMain.on('launch-game', async (event, config) => {
    if (isLaunching) {
      console.log("Already launching, ignoring request.");
      return;
    }
    
    isLaunching = true;
    const { root, ram, username } = config;
    console.log("Launching game in", root, "with username", username);

    let opts = {
      clientPackage: null,
      authorization: {
        access_token: '0',
        client_token: '0',
        uuid: '00000000-0000-0000-0000-000000000000',
        name: username || 'Player',
        user_properties: '{}',
        meta: {
          type: 'offline',
          demo: false
        }
      },
      root: root,
      version: {
        number: "26.2",
        type: "release",
        custom: "fabric-loader-0.19.5-26.2"
      },
      memory: {
        max: `${ram}M`,
        min: `${ram / 2}M`
      }
    };

    // Auto-install Fabric API if missing
    const modsDir = path.join(root, 'mods');
    if (!fs.existsSync(modsDir)) {
      fs.mkdirSync(modsDir, { recursive: true });
    }
    
    const fabricApiUrl = 'https://cdn.modrinth.com/data/P7dR8mSH/versions/ewUK83HI/fabric-api-0.161.0%2B26.2.jar';
    const fabricApiPath = path.join(modsDir, 'fabric-api.jar');
    
    if (!fs.existsSync(fabricApiPath)) {
      console.log('Downloading Fabric API...');
      event.sender.send('launch-progress', { type: 'FABRIC API', percent: 0 });
      
      await new Promise((resolve, reject) => {
        const file = fs.createWriteStream(fabricApiPath);
        https.get(fabricApiUrl, (response) => {
          response.pipe(file);
          file.on('finish', () => {
            file.close();
            resolve(true);
          });
        }).on('error', (err) => {
          fs.unlink(fabricApiPath, () => {});
          reject(err);
        });
      });
      console.log('Fabric API downloaded.');
    }

    // Listeners are attached per launch and dropped afterwards; adding them every time used to
    // stack a new copy on each click.
    launcher.removeAllListeners('progress');
    launcher.removeAllListeners('data');
    launcher.removeAllListeners('close');

    let windowOpen = false;
    launcher.on('progress', (e) => {
      // e.type is typically 'assets', 'natives', 'classes', 'libraries'
      const percent = e.total ? Math.round((e.task / e.total) * 100) : 0;
      event.sender.send('launch-progress', { type: e.type, percent });
    });
    launcher.on('data', (line) => {
      const text = String(line);
      // LWJGL reports its backend as the game window is created: from here the player sees the game.
      if (!windowOpen && (text.includes('Backend library:') || text.includes('Sound engine started'))) {
        windowOpen = true;
        try { fs.writeFileSync(markerPath(root), String(Date.now())); } catch {}
        event.sender.send('game-ready');
      }
    });
    launcher.on('close', (code) => {
      // A launch that never opened the window may have hit a broken file: verify fully next time.
      if (!windowOpen) { try { fs.rmSync(markerPath(root), { force: true }); } catch {} }
      event.sender.send('game-closed', code);
    });

    fastVerify = verifiedRecently(root);
    try {
      await launcher.launch(opts);
      // The process is up, but the window takes a while: the UI says "starting" until game-ready.
      event.sender.send('launch-success');
    } catch (err) {
      console.error(err);
      event.sender.send('launch-error', err.message);
    } finally {
      isLaunching = false;
    }
  });

  ipcMain.on('check-launch-status', (event) => {
    event.sender.send('launch-status-reply', isLaunching);
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (jarvisProcess) jarvisProcess.kill();
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
