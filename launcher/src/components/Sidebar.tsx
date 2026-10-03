import { Home, User, Settings, LogOut } from "lucide-react";
import { NavLink } from "react-router-dom";
import { cn } from "@/lib/utils";

const Sidebar = () => {
  return (
    <div className="w-16 flex flex-col items-center py-4 gap-4 bg-background">
      <div className="flex flex-col gap-2 w-full px-2">
        <NavLink 
          to="/" 
          className={({ isActive }) => cn(
            "flex items-center justify-center w-full aspect-square rounded-xl transition-all duration-200",
            isActive ? "bg-accent text-primary shadow-sm" : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
          )}
        >
          <Home size={22} strokeWidth={2.5} />
        </NavLink>
        
        <NavLink 
          to="/profile" 
          className={({ isActive }) => cn(
            "flex items-center justify-center w-full aspect-square rounded-xl transition-all duration-200",
            isActive ? "bg-accent text-primary shadow-sm" : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
          )}
        >
          <User size={22} strokeWidth={2.5} />
        </NavLink>
        
        <NavLink 
          to="/settings" 
          className={({ isActive }) => cn(
            "flex items-center justify-center w-full aspect-square rounded-xl transition-all duration-200",
            isActive ? "bg-accent text-primary shadow-sm" : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
          )}
        >
          <Settings size={22} strokeWidth={2.5} />
        </NavLink>
      </div>
      
      <div className="mt-auto w-full px-2">
        <button className="flex items-center justify-center w-full aspect-square rounded-xl text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-all duration-200">
          <LogOut size={22} strokeWidth={2.5} />
        </button>
      </div>
    </div>
  );
};

export default Sidebar;