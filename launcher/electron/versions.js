// The Minecraft versions the launcher offers. Each gets its own game folder (mods, configs,
// worlds, options) under <root>/instances/<id>; libraries, assets and Java runtimes are shared.
// `mods` are Modrinth project slugs the launcher installs and keeps up to date for that version.

export const VERSIONS = {
  '26.2': {
    loader: '0.19.5',
    // FestVisuals itself; its jar already carries Sodium, Lithium, FerriteCore, ImmediatelyFast
    // and Dynamic FPS, so only Fabric API is added next to it.
    festvisuals: true,
    // ViaFabricPlus: the version picker in the multiplayer menu, to join servers of other versions.
    mods: ['fabric-api', 'viafabricplus'],
  },
  '1.21.11': {
    loader: '0.19.5',
    festvisuals: false,
    mods: ['fabric-api', 'sodium', 'lithium', 'ferrite-core', 'immediatelyfast', 'dynamic-fps', 'viafabricplus'],
  },
  '1.16.5': {
    loader: '0.19.5',
    festvisuals: false,
    mods: ['fabric-api', 'sodium', 'lithium', 'ferrite-core', 'dynamic-fps'],
  },
};

export function profileId(version) {
  return `fabric-loader-${VERSIONS[version].loader}-${version}`;
}
