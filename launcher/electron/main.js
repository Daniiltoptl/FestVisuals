import { app, BrowserWindow, ipcMain } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';
import http from 'http';
import { spawn } from 'child_process';
import { launchGame } from './game.js';

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

    // The mod jar ships inside the launcher (see scripts/bundle-mod.mjs).
    const modJar = app.isPackaged
      ? path.join(process.resourcesPath, 'mod', 'festvisuals.jar')
      : path.join(__dirname, '..', 'mod', 'festvisuals.jar');
    const emit = (channel, payload) => {
      if (!event.sender.isDestroyed()) event.sender.send(channel, payload);
    };

    try {
      await launchGame({ root, ram, username, modJar }, emit);
    } catch (err) {
      console.error(err);
      emit('launch-error', err.message);
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
