import { useEffect, useState } from "react";
import { Download, RefreshCw } from "lucide-react";

type UpdateState = { state: "idle" | "downloading" | "ready"; version?: string; percent?: number };

const electron = (window as any).require ? (window as any).require("electron") : null;

/** Thin bar shown while the launcher downloads and installs a new version of itself. */
const UpdateBanner = () => {
  const [update, setUpdate] = useState<UpdateState>({ state: "idle" });

  useEffect(() => {
    if (!electron) return;
    const onUpdate = (_: unknown, next: UpdateState) =>
      setUpdate((current) => ({ ...current, ...next, version: next.version || current.version }));
    electron.ipcRenderer.on("updater", onUpdate);
    return () => electron.ipcRenderer.removeListener("updater", onUpdate);
  }, []);

  if (update.state === "idle") return null;

  return (
    <div className="flex items-center gap-3 border-b border-border bg-primary/15 px-4 py-2 text-xs font-semibold text-white">
      {update.state === "downloading" ? (
        <>
          <Download size={14} className="animate-bounce text-primary" />
          <span>Обновление лаунчера{update.version ? ` до ${update.version}` : ""}…</span>
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
            <div className="h-full bg-primary transition-all duration-300" style={{ width: `${update.percent ?? 0}%` }} />
          </div>
          <span className="w-10 text-right">{update.percent ?? 0}%</span>
        </>
      ) : (
        <>
          <RefreshCw size={14} className="animate-spin text-primary" />
          <span>Обновление {update.version} готово — перезапускаю лаунчер…</span>
        </>
      )}
    </div>
  );
};

export default UpdateBanner;
