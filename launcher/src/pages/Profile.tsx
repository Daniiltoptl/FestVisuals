import { useState, useRef, useEffect } from "react";
import { User } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

const Profile = () => {
  const [avatar, setAvatar] = useState<string | null>(localStorage.getItem('user_avatar'));
  const [login, setLogin] = useState<string>(localStorage.getItem('user_login') || 'FestPlayer');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    localStorage.setItem('user_login', login);
  }, [login]);

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64 = reader.result as string;
        setAvatar(base64);
        localStorage.setItem('user_avatar', base64);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="w-full h-full p-6 overflow-y-auto">
      <div className="max-w-4xl mx-auto space-y-6">
        
        <div className="flex items-center gap-3 mb-8">
          <User size={28} className="text-primary" />
          <h1 className="text-2xl font-bold">Профиль игрока</h1>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          
          {/* Avatar Area */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm flex flex-col items-center justify-center">
            <div className="w-32 h-32 rounded-full bg-secondary/80 border-4 border-background flex items-center justify-center shadow-inner mb-6 relative overflow-hidden">
              {avatar ? (
                <img src={avatar} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                <User size={48} className="text-muted-foreground" />
              )}
            </div>
            <input 
              type="file" 
              accept="image/*" 
              className="hidden" 
              ref={fileInputRef} 
              onChange={handleAvatarChange} 
            />
            <Button 
              variant="secondary" 
              className="hover:text-primary transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              Изменить аватарку
            </Button>
          </div>
          
          {/* Stats Area */}
          <div className="bg-background/50 border border-border p-6 rounded-2xl shadow-sm">
            <h2 className="text-lg font-bold uppercase tracking-wider mb-6">Информация</h2>
            <div className="space-y-4">
              <div className="flex justify-between items-center py-2 border-b border-border/50">
                <span className="text-muted-foreground text-sm font-medium">Логин</span>
                <Input 
                  value={login} 
                  onChange={(e) => setLogin(e.target.value)}
                  className="w-40 bg-secondary/50 border-border text-right font-bold text-white focus-visible:ring-primary"
                />
              </div>
              <div className="flex justify-between items-center py-2 border-b border-border/50">
                <span className="text-muted-foreground text-sm font-medium">Роль</span>
                <span className="font-bold text-primary">Пользователь</span>
              </div>
              <div className="flex justify-between items-center py-2">
                <span className="text-muted-foreground text-sm font-medium">Подписка до</span>
                <span className="font-mono font-bold text-white">Навсегда</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Profile;