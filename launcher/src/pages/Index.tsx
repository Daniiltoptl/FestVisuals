import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Play, ArrowLeft, Download, Loader2, Check, FolderOpen } from "lucide-react";
import { gameDir, instanceDir, totalRamMb } from "@/lib/gameDir";
import { findVersion } from "@/lib/versions";

type GameState = {
  state: "idle" | "launching" | "downloading" | "starting" | "running";
  version: string | null;
  type?: string;
  percent?: number;
};

const PROGRESS_LABELS: Record<string, string> = {
  assets: "АССЕТОВ",
  "assets-copy": "АССЕТОВ",
  natives: "ДВИЖКА",
  classes: "БИБЛИОТЕК",
  libraries: "БИБЛИОТЕК",
  "classes-custom": "FABRIC",
  "classes-maven-custom": "FABRIC",
  "version-jar": "ИГРЫ",
  java: "JAVA",
  mods: "МОДОВ",
  festvisuals: "КЛИЕНТА",
};

const electron = (window as any).require ? (window as any).require("electron") : null;

const Index = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const version = findVersion(id);
  const [game, setGame] = useState<GameState>({ state: "idle", version: null });

  useEffect(() => {
    if (!version) navigate("/", { replace: true });
  }, [version, navigate]);

  useEffect(() => {
    if (!electron) return;
    const onState = (_: unknown, next: GameState) => setGame(next);
    const onError = (_: unknown, error: { version: string; message: string }) => {
      if (error.version === id) alert("Ошибка запуска: " + error.message);
    };
    electron.ipcRenderer.on("game-state", onState);
    electron.ipcRenderer.on("launch-error", onError);
    electron.ipcRenderer.send("get-game-state");
    return () => {
      electron.ipcRenderer.removeListener("game-state", onState);
      electron.ipcRenderer.removeListener("launch-error", onError);
    };
  }, [id]);

  if (!version) return null;

  const mine = game.version === version.id;
  const otherRunning = game.state !== "idle" && !mine;
  const status = mine ? game.state : "idle";

  const launch = () => {
    if (!electron || game.state !== "idle") return;
    const ram = Math.min(Number(localStorage.getItem("settings_ram")) || 4096, totalRamMb());
    const width = Math.min(Number(localStorage.getItem("settings_res_w")) || 1280, window.screen.availWidth);
    const height = Math.min(Number(localStorage.getItem("settings_res_h")) || 720, window.screen.availHeight);
    electron.ipcRenderer.send("launch-game", {
      version: version.id,
      root: gameDir(),
      ram,
      username: localStorage.getItem("user_login") || "FestPlayer",
      closeOnLaunch: localStorage.getItem("settings_close_launch") === "true",
      vulkan: localStorage.getItem("settings_vulkan") !== "false",
      window: { width, height, fullscreen: localStorage.getItem("settings_fullscreen") === "true" },
    });
  };

  const openMods = () => {
    const req = (window as any).require;
    if (!req || !electron) return;
    const mods = req("path").join(instanceDir(version.id), "mods");
    req("fs").mkdirSync(mods, { recursive: true });
    electron.shell.openPath(mods);
  };

  const label = (() => {
    if (otherRunning) return `ЗАПУЩЕНА ${game.version}`;
    switch (status) {
      case "launching": return "ПРОВЕРКА...";
      case "downloading": return `ЗАГРУЗКА ${PROGRESS_LABELS[game.type || ""] || ""} ${game.percent ?? 0}%`;
      case "starting": return "ЗАПУСК...";
      case "running": return "В ИГРЕ";
      default: return "ЗАПУСТИТЬ";
    }
  })();

  return (
    <div className="flex h-full w-full gap-8 p-6">
      {/* Left side: the version's art */}
      <div className="relative w-[350px] shrink-0 overflow-hidden rounded-2xl shadow-2xl">
        <div className="pointer-events-none absolute inset-0 z-10 bg-gradient-to-t from-background/80 via-transparent to-transparent" />
        <img src={version.image} alt={version.title} className="h-full w-full object-cover" />
      </div>

      {/* Right side: content */}
      <div className="relative flex flex-1 flex-col pb-2">
        <button
          onClick={() => navigate("/")}
          className="mb-4 flex w-fit items-center gap-2 rounded-lg px-2 py-1 text-sm font-semibold text-muted-foreground transition-colors hover:bg-secondary/50 hover:text-white"
        >
          <ArrowLeft size={16} />
          Назад
        </button>

        <div className="mb-6">
          <h1 className="mb-1 text-3xl font-extrabold tracking-tight text-white">
            {version.title} <span className="text-primary">{version.tag}</span>
          </h1>
          <h2 className="text-sm font-medium uppercase tracking-wider text-muted-foreground">{version.subtitle}</h2>
        </div>

        <div className="max-w-xl text-sm leading-relaxed text-gray-300">{version.description}</div>

        <div className="mt-auto flex items-center justify-end gap-3">
          <button
            onClick={openMods}
            className="flex items-center gap-2 rounded-xl bg-secondary px-5 py-3 text-sm font-bold uppercase tracking-wider text-white transition-colors hover:bg-secondary/80"
          >
            <FolderOpen size={18} />
            Папка модов
          </button>

          <button
            onClick={launch}
            disabled={status !== "idle" || otherRunning}
            className={`
              relative flex items-center gap-3 overflow-hidden rounded-xl px-8 py-3 font-bold shadow-lg transition-all
              ${status === "idle" && !otherRunning ? "bg-primary text-primary-foreground shadow-[0_0_20px_rgba(255,107,0,0.3)] hover:scale-105 hover:bg-primary/90 hover:shadow-[0_0_30px_rgba(255,107,0,0.5)] active:scale-95" : ""}
              ${otherRunning ? "cursor-not-allowed bg-secondary text-muted-foreground" : ""}
              ${status === "launching" || status === "starting" || status === "downloading" ? "cursor-wait bg-secondary text-white" : ""}
              ${status === "running" ? "bg-green-600 text-white" : ""}
            `}
          >
            {status === "downloading" && (
              <div
                className="absolute bottom-0 left-0 top-0 bg-primary/30 transition-all duration-300 ease-out"
                style={{ width: `${game.percent ?? 0}%` }}
              />
            )}
            <div className="relative z-10 flex items-center gap-3">
              {status === "idle" && !otherRunning && <Play size={18} fill="currentColor" />}
              {(status === "launching" || status === "starting") && <Loader2 size={18} className="animate-spin" />}
              {status === "downloading" && <Download size={18} className="animate-bounce" />}
              {status === "running" && <Check size={18} />}
              <span className="text-sm uppercase tracking-wider">{label}</span>
            </div>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Index;
