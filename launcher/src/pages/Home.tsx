import { useNavigate } from "react-router-dom";
import VersionCard from "@/components/VersionCard";
import { VERSIONS } from "@/lib/versions";

const Home = () => {
  const navigate = useNavigate();

  return (
    <div className="flex h-full w-full flex-col p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-extrabold tracking-tight text-white">Выбери версию</h1>
        <p className="text-sm text-muted-foreground">У каждой версии своя папка модов, настроек и миров</p>
      </div>

      <div className="grid min-h-0 flex-1 grid-cols-3 gap-5">
        {VERSIONS.map((version) => (
          <VersionCard key={version.id} version={version} onOpen={() => navigate(`/play/${version.id}`)} />
        ))}
      </div>
    </div>
  );
};

export default Home;
