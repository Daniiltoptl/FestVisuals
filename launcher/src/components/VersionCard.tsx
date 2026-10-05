import { ArrowRight } from "lucide-react";
import type { GameVersion } from "@/lib/versions";

interface VersionCardProps {
  version: GameVersion;
  onOpen: () => void;
}

const VersionCard = ({ version, onOpen }: VersionCardProps) => (
  <button
    onClick={onOpen}
    className="group relative h-full min-h-0 overflow-hidden rounded-2xl border border-border text-left shadow-xl transition-all duration-300 hover:-translate-y-1 hover:border-primary/60 hover:shadow-[0_0_30px_rgba(255,107,0,0.25)]"
  >
    <img
      src={version.image}
      alt={version.title}
      className="absolute inset-0 h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
    />
    <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/25 to-transparent" />

    <div className="absolute right-3 top-3 rounded-md bg-black/60 px-2 py-0.5 text-[11px] font-bold tracking-wider text-primary">
      {version.tag}
    </div>

    <div className="absolute inset-x-0 bottom-0 p-5">
      <div className="text-3xl font-extrabold tracking-tight text-white">{version.title}</div>
      <div className="mt-1 flex items-center justify-between text-sm text-gray-300">
        <span>{version.subtitle}</span>
        <ArrowRight size={18} className="translate-x-[-4px] opacity-0 transition-all group-hover:translate-x-0 group-hover:opacity-100" />
      </div>
    </div>
  </button>
);

export default VersionCard;
