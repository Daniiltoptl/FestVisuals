import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Folder, HardDrive, Monitor, Settings as SettingsIcon } from "lucide-react";

const Settings = () => {
  // Load settings from localStorage or use defaults
  const [ram, setRam] = useState(() => Number(localStorage.getItem('settings_ram')) || 4096);
  const [gameDir, setGameDir] = useState(() => localStorage.getItem('settings_dir') || 'D:/FestVisualss');
  const [resW, setResW] = useState(() => localStorage.getItem('settings_res_w') || '1920');
  const [resH, setResH] = useState(() => localStorage.getItem('settings_res_h') || '1080');
  const [fullscreen, setFullscreen] = useState(() => localStorage.getItem('settings_fullscreen') === 'true');
  const [closeOnLaunch, setCloseOnLaunch] = useState(() => localStorage.getItem('settings_close_launch') === 'true');

  // Save settings on change
  useEffect(() => {
    localStorage.setItem('settings_ram', String(ram));
    localStorage.setItem('settings_dir', gameDir);
    localStorage.setItem('settings_res_w', resW);
    localStorage.setItem('settings_res_h', resH);
    localStorage.setItem('settings_fullscreen', String(fullscreen));
    localStorage.setItem('settings_close_launch', String(closeOnLaunch));
  }, [ram, gameDir, resW, resH, fullscreen, closeOnLaunch]);

  return (
    <div className="w-full h-full p-6 overflow-y-auto">
      <div className="max-w-4xl mx-auto space-y-6">
        
        <div className="flex items-center gap-3 mb-8">
          <SettingsIcon size={28} className="text-primary" />
          <h1 className="text-2xl font-bold">Настройки лаунчера</h1>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* RAM Settings */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm">
            <div className="flex items-center gap-3 mb-2">
              <HardDrive className="text-primary" size={20} />
              <h2 className="text-lg font-bold uppercase tracking-wider">Выделение ОЗУ</h2>
            </div>
            <p className="text-sm text-muted-foreground mb-6">
              Укажите объем оперативной памяти, который будет доступен для игры.
            </p>
            
            <div className="space-y-4">
              <div className="flex items-center justify-between text-sm font-bold text-white mb-2">
                <span>1024 MB</span>
                <span className="text-primary">{ram} MB</span>
                <span>8192 MB</span>
              </div>
              <input 
                type="range" 
                min="1024" 
                max="8192" 
                step="512" 
                value={ram} 
                onChange={(e) => setRam(Number(e.target.value))}
                className="w-full accent-primary h-2 bg-secondary rounded-lg appearance-none cursor-pointer"
              />
              <div className="flex justify-between gap-4 mt-4">
                <Input 
                  type="number" 
                  value={ram} 
                  onChange={(e) => setRam(Number(e.target.value))}
                  className="bg-secondary/50 border-border text-center font-mono" 
                />
              </div>
            </div>
          </div>
          
          {/* Game Directory */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm">
            <div className="flex items-center gap-3 mb-2">
              <Folder className="text-primary" size={20} />
              <h2 className="text-lg font-bold uppercase tracking-wider">Директория игры</h2>
            </div>
            <p className="text-sm text-muted-foreground mb-6">
              Папка, в которую будут скачиваться все ресурсы и сам клиент FestVisuals.
            </p>
            <div className="flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <Input 
                  value={gameDir} 
                  onChange={(e) => setGameDir(e.target.value)}
                  className="bg-secondary/50 border-border font-mono text-sm" 
                />
              </div>
              <div className="flex gap-4">
                <Button 
                  variant="secondary" 
                  className="w-full flex items-center justify-center gap-2 hover:bg-primary/20 hover:text-primary transition-colors"
                  onClick={() => {
                    const electron = (window as any).require('electron');
                    const fs = (window as any).require('fs');
                    try {
                      if (!fs.existsSync(gameDir)) fs.mkdirSync(gameDir, { recursive: true });
                      if (electron) electron.shell.openPath(gameDir);
                    } catch(e) { console.error("Error opening dir:", e); }
                  }}
                >
                  <Folder size={16} />
                  Папка игры
                </Button>
                <Button 
                  variant="secondary" 
                  className="w-full flex items-center justify-center gap-2 hover:bg-primary/20 hover:text-primary transition-colors"
                  onClick={() => {
                    const electron = (window as any).require('electron');
                    const fs = (window as any).require('fs');
                    const path = (window as any).require('path');
                    try {
                      const modsDir = path.join(gameDir, 'mods');
                      if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });
                      if (electron) electron.shell.openPath(modsDir);
                    } catch(e) { console.error("Error opening mods:", e); }
                  }}
                >
                  <Folder size={16} />
                  Папка модов
                </Button>
              </div>
            </div>
          </div>
          
          {/* Resolution */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm">
            <div className="flex items-center gap-3 mb-2">
              <Monitor className="text-primary" size={20} />
              <h2 className="text-lg font-bold uppercase tracking-wider">Разрешение экрана</h2>
            </div>
            <p className="text-sm text-muted-foreground mb-6">
              Настройте разрешение окна при запуске игры.
            </p>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs text-muted-foreground font-bold uppercase mb-1 block">Ширина</label>
                <Input 
                  type="number" 
                  value={resW} 
                  onChange={(e) => setResW(e.target.value)}
                  className="bg-secondary/50 border-border font-mono" 
                />
              </div>
              <div>
                <label className="text-xs text-muted-foreground font-bold uppercase mb-1 block">Высота</label>
                <Input 
                  type="number" 
                  value={resH} 
                  onChange={(e) => setResH(e.target.value)}
                  className="bg-secondary/50 border-border font-mono" 
                />
              </div>
            </div>
          </div>
          
          {/* Other Settings */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm">
            <h2 className="text-lg font-bold uppercase tracking-wider mb-2">Дополнительно</h2>
            <div className="space-y-4 mt-6">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Полноэкранный режим</span>
                <Switch 
                  checked={fullscreen} 
                  onCheckedChange={setFullscreen} 
                />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Закрывать лаунчер при запуске</span>
                <Switch 
                  checked={closeOnLaunch} 
                  onCheckedChange={setCloseOnLaunch} 
                />
              </div>
            </div>
          </div>
          
        </div>
      </div>
    </div>
  );
};

export default Settings;