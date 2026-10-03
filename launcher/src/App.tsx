import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HashRouter, Routes, Route } from "react-router-dom";
import Sidebar from "./components/Sidebar";
import Index from "./pages/Index";
import Profile from "./pages/Profile";
import Settings from "./pages/Settings";
import { Minus, X } from "lucide-react";

// @ts-ignore
const electron = window.require ? window.require('electron') : null;

const queryClient = new QueryClient();

const Topbar = () => {
  const handleMinimize = () => {
    if (electron) electron.ipcRenderer.send('minimize-window');
  };
  
  const handleClose = () => {
    if (electron) electron.ipcRenderer.send('close-window');
  };

  return (
    <div className="h-10 flex items-center justify-between px-4 window-drag bg-background text-foreground shrink-0 z-50">
      <div className="font-bold text-sm tracking-wider uppercase">FESTVISUALS</div>
      <div className="flex items-center gap-4 no-drag">
        <button onClick={handleMinimize} className="hover:text-primary transition-colors">
          <Minus size={16} />
        </button>
        <button onClick={handleClose} className="hover:text-destructive transition-colors">
          <X size={16} />
        </button>
      </div>
    </div>
  );
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <HashRouter>
        <div className="h-screen w-screen flex flex-col overflow-hidden bg-background">
          <Topbar />
          <div className="flex flex-1 overflow-hidden p-2 gap-2">
            <Sidebar />
            <main className="flex-1 overflow-hidden bg-card rounded-xl border border-border shadow-lg">
              <Routes>
                <Route path="/" element={<Index />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/settings" element={<Settings />} />
              </Routes>
            </main>
          </div>
        </div>
      </HashRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;