package com.fest.visuals;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.command.CommandManager;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.system.DiscordHook;
import com.fest.visuals.api.system.configs.ConfigManager;
import com.fest.visuals.api.system.configs.ConfigSkin;
import com.fest.visuals.api.system.configs.FriendManager;
import com.fest.visuals.api.system.configs.MacroManager;
import com.fest.visuals.api.system.draggable.DraggableManager;
import com.fest.visuals.api.system.files.FileManager;
import com.fest.visuals.api.system.media.NowPlayingService;
import com.fest.visuals.api.utils.other.SoundUtil;
import com.fest.visuals.api.utils.render.KawaseBlurProgram;
import com.fest.visuals.api.utils.render.pipeline.FestLayers;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.services.HeartbeatService;
import com.fest.visuals.client.services.RenderService;
import com.fest.visuals.client.ui.theme.ThemeEditor;
import com.fest.visuals.client.ui.widget.WidgetManager;

public class FestVisuals implements ClientModInitializer {
	@Getter private static FestVisuals instance = new FestVisuals();

    @Override
	public void onInitializeClient() {
        instance = this;

        FestPipelines.register();
        FestLayers.register();

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (context.poseStack() == null || context.submitNodeCollector() == null) return;
            RenderUtil.WORLD.beginFrame(context.submitNodeCollector());
            Render3DEvent.getInstance().call(new Render3DEvent.Render3DEventData(
                    context.poseStack(),
                    net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false),
                    context.submitNodeCollector(), context.levelState().cameraRenderState));
            RenderUtil.WORLD.finishFrame();
        });

        SoundUtil.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
        ));

        KawaseBlurProgram.load();
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
    }

    private void loadManagers() {
        com.fest.visuals.api.system.rpc.DiscordRPCManager.getInstance().start();
        com.fest.visuals.client.features.waypoints.WaypointsRender.getInstance().init();
        com.fest.visuals.client.features.waypoints.WaypointManager.getInstance().init();
        WidgetManager.getInstance().load();
        com.fest.visuals.api.system.configs.UtilityConfig.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();
        NowPlayingService.getInstance().start();

        DiscordHook.startRPC();
    }

    public void onClose() {
        com.fest.visuals.api.system.rpc.DiscordRPCManager.getInstance().stop();
        com.fest.visuals.client.features.waypoints.WaypointManager.getInstance().save();
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();
        NowPlayingService.getInstance().stop();

        DiscordHook.stopRPC();
    }
}



