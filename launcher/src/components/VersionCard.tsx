import { ArrowRight } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useState } from "react";

interface VersionCardProps {
  version: string;
  image: string;
  title: string;
}

const VersionCard = ({ version, image, title }: VersionCardProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <>
      <div 
        className="glass-panel card-hover group relative overflow-hidden cursor-pointer"
        onClick={() => setIsDialogOpen(true)}
      >
        <img 
          src={image} 
          alt={title}
          className="w-full h-48 object-cover rounded-t-lg"
        />
        <div className="absolute top-2 right-2 bg-black/50 px-2 py-1 rounded text-xs">
          {title}
        </div>
        <div className="p-4 flex justify-between items-center">
          <h3 className="text-lg font-medium">{version}</h3>
          <ArrowRight className="opacity-0 group-hover:opacity-100 transition-opacity" size={20} />
        </div>
      </div>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="bg-[#0B1622] border-none text-white max-w-4xl">
          <DialogHeader>
            <div className="flex justify-between items-center mb-4">
              <DialogTitle className="text-2xl font-bold">{version}</DialogTitle>
              <Button 
                variant="ghost" 
                className="text-white hover:bg-white/10"
                onClick={() => setIsDialogOpen(false)}
              >
                НАЗАД
                <ArrowRight className="ml-2" />
              </Button>
            </div>
          </DialogHeader>
          
          <div className="grid grid-cols-1 md:grid-cols-[300px,1fr] gap-6">
            <img 
              src={image} 
              alt={title}
              className="w-full rounded-lg object-cover"
            />
            <div className="space-y-4">
              <h3 className="text-xl font-medium">{title}</h3>
              <p className="text-gray-300">
                Мы создали для вас лучший клиент, который даст вам огромное преимущество в игре. В этом клиенте огромный функционал, который подойдёт под все популярные сервера майнкрафт. Этот клиент является стабильным, а это означает, что вы получите наилучший игровой опыт без багов и различных ошибок.
              </p>
              <Button className="bg-blue-600 hover:bg-blue-700 text-white px-8">
                ЗАПУСТИТЬ
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default VersionCard;