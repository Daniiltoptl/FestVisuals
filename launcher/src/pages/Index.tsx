import { useState, useEffect } from "react";
import { Play, ArrowRight, Download, Loader2, Check } from "lucide-react";
import { gameDir } from "@/lib/gameDir";

const Index = () => {
  const [status, setStatus] = useState<'idle' | 'checking' | 'downloading' | 'starting' | 'playing'>('idle');
  const [progress, setProgress] = useState(0);
  const [downloadType, setDownloadType] = useState('');

  useEffect(() => {
    const electron = (window as any).require ? (window as any).require('electron') : null;
    if (!electron) return;

    const handleProgress = (_: any, data: { type: string, percent: number }) => {
      setStatus('downloading');
      setProgress(data.percent || 0);
      
      const typeTranslations: Record<string, string> = {
        'assets': 'АССЕТОВ',
        'natives': 'ДВИЖКА',
        'classes': 'ЯДРА',
        'libraries': 'БИБЛИОТЕК',
        'classes-custom': 'FABRIC',
        'classes-maven-custom': 'FABRIC',
        'version-jar': 'ИГРЫ',
        'java': 'JAVA',
        'fabric': 'FABRIC API'
      };
      setDownloadType(typeTranslations[data.type] || data.type);
    };

    // The process is running but the window is not up yet.
    const handleSuccess = () => {
      setStatus((current) => (current === 'playing' ? current : 'starting'));
    };

    const handleReady = () => setStatus('playing');
    const handleClosed = () => setStatus('idle');

    const handleError = (_: any, err: string) => {
      console.error("Launch error:", err);
      setStatus('idle');
      alert("Ошибка запуска: " + err);
    };

    electron.ipcRenderer.on('launch-progress', handleProgress);
    electron.ipcRenderer.on('launch-success', handleSuccess);
    electron.ipcRenderer.on('game-ready', handleReady);
    electron.ipcRenderer.on('game-closed', handleClosed);
    electron.ipcRenderer.on('launch-error', handleError);

    // Ask main process if currently launching
    electron.ipcRenderer.send('check-launch-status');
    electron.ipcRenderer.on('launch-status-reply', (_: any, isLaunching: boolean) => {
      if (isLaunching && status === 'idle') setStatus('checking');
    });

    return () => {
      electron.ipcRenderer.removeListener('launch-progress', handleProgress);
      electron.ipcRenderer.removeListener('launch-success', handleSuccess);
      electron.ipcRenderer.removeListener('game-ready', handleReady);
      electron.ipcRenderer.removeListener('game-closed', handleClosed);
      electron.ipcRenderer.removeListener('launch-error', handleError);
      electron.ipcRenderer.removeAllListeners('launch-status-reply');
    };
  }, []);

  const handlePlayClick = () => {
    if (status !== 'idle') return;
    
    const electron = (window as any).require ? (window as any).require('electron') : null;
    if (!electron) return;

    const ram = Number(localStorage.getItem('settings_ram')) || 4096;
    const root = gameDir();
    const username = localStorage.getItem('user_login') || 'FestPlayer';

    setStatus('checking');
    electron.ipcRenderer.send('launch-game', { root, ram, username });
  };

  // Reset after some time if "in game" just for demo purposes (optional)
  // Usually this would reset when the game process closes.

  return (
    <div className="w-full h-full flex p-6 gap-8">
      
      {/* Left side: Hero Image */}
      <div className="w-[350px] shrink-0 rounded-2xl overflow-hidden relative shadow-2xl">
        <div className="absolute inset-0 bg-gradient-to-t from-background/80 via-transparent to-transparent z-10 pointer-events-none"></div>
        <img 
          src="./hero_bg.jpg" 
          alt="FestVisuals Hero" 
          className="w-full h-full object-cover"
        />
      </div>

      {/* Right side: Content */}
      <div className="flex-1 flex flex-col pt-4 pb-2 relative">
        
        {/* Top section: Title */}
        <div className="mb-6">
          <h1 className="text-3xl font-extrabold text-white tracking-tight mb-1">26.2 FREE</h1>
          <h2 className="text-sm text-muted-foreground font-medium uppercase tracking-wider">Клиент</h2>
        </div>

        {/* Text Description */}
        <div className="text-sm text-gray-300 leading-relaxed max-w-xl">
          Мы создали для вас лучший клиент, который даст вам огромное преимущество в игре. 
          В этом клиенте огромный функционал, который подойдет под все популярные сервера майнкрафт. 
          Этот клиент является стабильным, а это означает, что вы получите наилучший игровой опыт 
          без багов и различных ошибок.
        </div>

        {/* Bottom section: Play Button */}
        <div className="mt-auto flex justify-end">
          <button 
            onClick={handlePlayClick}
            disabled={status !== 'idle' && status !== 'playing'}
            className={`
              font-bold py-3 px-8 rounded-xl flex items-center gap-3 transition-all shadow-lg
              ${status === 'idle' ? 'bg-primary hover:bg-primary/90 text-primary-foreground hover:scale-105 active:scale-95 shadow-[0_0_20px_rgba(255,107,0,0.3)] hover:shadow-[0_0_30px_rgba(255,107,0,0.5)]' : ''}
              ${status === 'checking' || status === 'starting' ? 'bg-secondary text-white cursor-wait' : ''}
              ${status === 'downloading' ? 'bg-secondary text-white cursor-wait overflow-hidden relative' : ''}
              ${status === 'playing' ? 'bg-green-600 hover:bg-green-700 text-white' : ''}
            `}
          >
            {status === 'downloading' && (
              <div 
                className="absolute left-0 top-0 bottom-0 bg-primary/30 transition-all duration-300 ease-out" 
                style={{ width: `${progress}%` }} 
              />
            )}
            
            <div className="relative z-10 flex items-center gap-3">
              {status === 'idle' && <Play size={18} fill="currentColor" />}
              {(status === 'checking' || status === 'starting') && <Loader2 size={18} className="animate-spin" />}
              {status === 'downloading' && <Download size={18} className="animate-bounce" />}
              {status === 'playing' && <Check size={18} />}
              
              <span className="uppercase tracking-wider text-sm">
                {status === 'idle' && "ЗАПУСТИТЬ"}
                {status === 'checking' && "ПРОВЕРКА..."}
                {status === 'starting' && "ЗАПУСК..."}
                {status === 'downloading' && `ЗАГРУЗКА ${downloadType} ${progress}%`}
                {status === 'playing' && "В ИГРЕ"}
              </span>
            </div>
          </button>
        </div>
      </div>
      
    </div>
  );
};

export default Index;