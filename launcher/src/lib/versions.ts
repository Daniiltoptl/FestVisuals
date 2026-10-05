// What the launcher shows for each version. The ids must match electron/versions.js, which holds
// what gets installed.

export interface GameVersion {
  id: string;
  title: string;
  tag: string;
  subtitle: string;
  image: string;
  description: string;
}

export const VERSIONS: GameVersion[] = [
  {
    id: "26.2",
    title: "26.2",
    tag: "FREE",
    subtitle: "Клиент FestVisuals",
    image: "./versions/26.2.jpg",
    description:
      "Мы создали для вас лучший клиент, который даст вам огромное преимущество в игре. " +
      "В этом клиенте огромный функционал, который подойдет под все популярные сервера майнкрафт. " +
      "Этот клиент является стабильным, а это означает, что вы получите наилучший игровой опыт " +
      "без багов и различных ошибок.",
  },
  {
    id: "1.21.11",
    title: "1.21.11",
    tag: "FABRIC",
    subtitle: "Fabric + оптимизация",
    image: "./versions/1.21.11.jpg",
    description:
      "Чистый Fabric с модами на производительность: Sodium, Lithium, FerriteCore, " +
      "ImmediatelyFast и Dynamic FPS. Всё ставится и обновляется само, свои моды кидай в папку модов этой версии.",
  },
  {
    id: "1.16.5",
    title: "1.16.5",
    tag: "FABRIC",
    subtitle: "Fabric + оптимизация",
    image: "./versions/1.16.5.jpg",
    description:
      "Классическая 1.16.5 на Fabric с Sodium, Lithium, FerriteCore и Dynamic FPS. " +
      "Всё ставится и обновляется само, свои моды кидай в папку модов этой версии.",
  },
];

export function findVersion(id: string | undefined): GameVersion | undefined {
  return VERSIONS.find((v) => v.id === id);
}
