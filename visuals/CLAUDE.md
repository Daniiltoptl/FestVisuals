# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

Fabric mod for Minecraft **26.2** on Java **25**, built with Fabric Loom `1.17-SNAPSHOT`. All Gradle tasks below go through the wrapper.

- `./gradlew build` — full build + `remapJar` in `build/libs/evaware-<version>.jar` (bundled deps merged in).
- `./gradlew runClient` — launch dev client in `run/`.
- `./gradlew runClient2` — second client in `run2/` (defined in `build.gradle` `loom.runs.client2`), useful for testing two accounts / multiplayer.
- `./gradlew genSources` — decompile Minecraft into `.gradle/loom-cache/` so IDE navigation works.
- Every run config gets `--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Djoml.nounsafe=true` — required for MC 26.2's Panama use and JOML's unsafe path. Do not strip these when running from an IDE.

There are no unit tests in this project (Gradle's `test` task is set to `failOnNoDiscoveredTests = false`).

## Coordinates

- `archivesName = evaware`, `group = sweetie.evaware`, `mod_version` bumped in `gradle.properties`. Mod id in `fabric.mod.json` is `festvisuals`; the entrypoint is `com.fest.visuals.FestVisuals`.
- Access widener: `src/main/resources/festvisuals.accesswidener` (points at obfuscated mappings — `net/minecraft/...` intermediary names).
- Mixin config: `src/main/resources/festvisuals.mixins.json`, package `com.fest.visuals.inject` (client-only, JAVA_21 compat).
- Bundled runtime deps (DJL + PyTorch CPU, CatBoost, Discord RPC) live in a custom Gradle `bundled` configuration and are shaded into the final jar by the `jar` task — treat them as first-class dependencies.

## Architecture

Three sibling trees under `com.fest.visuals`:

- `api/` — framework: event bus, module base, settings, commands, config store, rendering pipeline, fonts, utilities.
- `client/` — everything the user sees: `features/modules/**`, `features/commands/**`, `features/waypoints/**`, `ui/**` (ClickGUI, HUD widgets, theme editor).
- `inject/` — Mixins (`client/`, `entity/`, `input/`, `other/`, `render/`). 39 classes; new mixins must be listed in `festvisuals.mixins.json`.

### Entry & lifecycle

`FestVisuals#onInitializeClient` (Fabric client entrypoint) registers the render pipeline (`EvaPipelines`/`EvaLayers`), hooks `LevelRenderEvents.COLLECT_SUBMITS` to bridge into the internal `Render3DEvent`, then walks three phases in order: `loadManagers()` → `loadServices()` → `loadFiles()`. `postLoad()` sorts modules by rendered name width and boots the Kawase blur program. `onClose()` persists Waypoints, `autoConfig`, drag positions, macros, theme, and stops Discord RPC — keep new persistent state on that list.

### Custom event bus (`api/event`)

Not Fabric events. Events extend `Flora<T>` with a nested `record` payload; each concrete event exposes a static `getInstance()` singleton, and subscribers register via `Flora.subscribe(new Listener<>(handler))`. `Flora` maintains a `ConcurrentSkipListSet` of listeners with a lazy `Listener[]` cache — do not iterate `listeners` directly, always go through `getCache()`.

Modules automatically get event bookkeeping through `Configurable`: `onEvent()` runs on enable, `removeAllEvents()` on disable. Store returned `EventListener` handles with `addEvents(...)` so they unsubscribe cleanly.

Where they fire from: Fabric callbacks in `FestVisuals`/`services`, Mixin `@Inject`s under `inject/`, and the raw GLFW keyboard/mouse paths in `MixinKeyboard`/`MixinMouse` (they call `KeyEvent.getInstance().call(new KeyEventData(...))`).

### Modules (`api/module`)

- Abstract `Module extends Configurable implements QuickImports` (`QuickImports` provides `mc`, `print`, `sendPacket` helpers).
- Metadata comes from the runtime annotation `@ModuleRegister(name = "...", category = Category.X, bind = GLFW.GLFW_KEY_...)`. Missing annotation throws at construction.
- `bind` defaults to `-999` (sentinel meaning "no bind" — `hasBind()` checks this). Mouse binds are stored offset by `-100` in `HeartbeatService#keyEvent` — mirror that offset when adding new bind pickers.
- Register instances in `ModuleManager#load()` (manual list, no classpath scan). Modules are singletons, exposed via Lombok-generated `getInstance()`.
- Enable/disable flow: `toggle()` → `setEnabled(newState, config)`; when the state actually changes, `onEnable()`/`onEvent()` fire and events subscribe, or `onDisable()`/`removeAllEvents()` runs. Passing `config = true` (used during config load) skips the "user pressed a bind" side effects.
- `ClickGUIModule` is the click-GUI itself — it toggles back off on `onEvent()` after opening `ScreenClickGUI`, so re-pressing Right Shift closes it.

### Settings (`api/module/setting`)

`Setting<T>` subclasses: `BooleanSetting`, `SliderSetting`, `ColorSetting`, `ModeSetting`, `MultiBooleanSetting`, `BindSetting`, `RunSetting`. Add them in the module constructor with `addSettings(...)`. `setVisible(Supplier<Boolean>)` chains for conditional visibility; `onAction(Runnable)` triggers on value change (call `runAction()` from custom setters).

### Commands (`api/command`)

Brigadier-based custom dispatcher (not vanilla `ClientCommandRegistrationCallback`). Extend `Command`, annotate with `@CommandRegister(name = "...")`, implement `execute(LiteralArgumentBuilder<SharedSuggestionProvider>)`, register in `CommandManager#load()`. Chat integration lives in `inject/other/`.

### Configs & files

`ConfigManager` (Gson) serialises every module's settings by name; `FestVisuals#onClose` writes the `autoConfig` slot. `FriendManager`, `MacroManager`, `DraggableManager`, `ThemeEditor` are peer singletons with the same load/save pattern — hook new persistent state in via those.

### UI (`client/ui`)

- Main GUI: `client/ui/clickgui/ScreenClickGUI extends Screen`. Layout constants in `ClickGuiLayout`, sub-panels split across `ClickGuiHeader/Sidebar/ModuleList/Configs/Waypoints`. `Panel` is the module column.
- HUD: `client/ui/widget/**` (drag-positioned via `DraggableManager`).
- Theme system in `client/ui/theme/basic` with a live editor.
- Rendering: `api/utils/render` — `RenderUtil`, `ScissorUtil`, custom pipelines (`EvaPipelines`, `EvaLayers`), MSDF font rendering (`api/utils/render/fonts`, glyphs baked at build time), Kawase blur (`KawaseBlurProgram`). Draw text through `Fonts.PS_MEDIUM.draw(...)` etc., not the vanilla `TextRenderer`.

### Rotation / combat helpers

`api/utils/rotation/**` runs a rotation state machine (`RotationManager` loaded in `loadManagers()`) that combat / player modules feed into; `api/utils/combat/**` has target selection and hit prediction. Modules should request rotations through the manager rather than writing to `Player.setYRot`/`setXRot` directly — the manager fakes yaw/pitch for outgoing packets via `MixinPlayerMove`.

## Conventions

- **Lombok is required** — `@Getter`/`@Setter`/`@UtilityClass` are used everywhere; keep the annotation processor enabled in your IDE.
- Singletons: `@Getter private static final Foo instance = new Foo();`. Constructors are public (framework code calls them once).
- `QuickImports` gives you `mc` (`Minecraft` instance) for free — implement it instead of calling `Minecraft.getInstance()` everywhere.
- Reserved obfuscated names (`class_...`, `field_...`, `method_...`) show up in stack traces — remap them via the loom-generated Minecraft sources rather than editing directly.
- Mod id in resources is `festvisuals/` while archive name is `evaware` — keep that split when adding assets.
