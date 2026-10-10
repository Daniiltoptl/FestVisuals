package com.fest.visuals.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.fest.visuals.client.features.modules.hud.SaturationModule;
import com.fest.visuals.client.features.modules.render.BlockHighlightModule;
import com.fest.visuals.client.features.modules.render.CrosshairModule;
import com.fest.visuals.client.features.modules.render.HitboxModule;
import com.fest.visuals.client.features.modules.render.TrajectoryModule;
import com.fest.visuals.client.features.modules.render.WingsModule;
import com.fest.visuals.client.features.modules.utility.ZoomModule;
import com.fest.visuals.client.ui.clickgui.ScreenClickGUI;

/** 1.21.11 smoke test: join a world with the visuals on and screenshot HUD, world shapes and GUI. */
public class VisualsScreenshotTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.takeScreenshot("title");
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getClientWorld().waitForChunksRender();
            TestServerContext server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            context.getInput().setCursorPos(0, 0);
            server.runCommand("execute as @a at @s run summon villager ^3 ^ ^5 {NoAI:1b,PersistenceRequired:1b}");

            context.runOnClient(mc -> {
                HitboxModule.getInstance().mobs.setValue(true);
                HitboxModule.getInstance().setEnabled(true);
                TrajectoryModule.getInstance().setEnabled(true);
                CrosshairModule.getInstance().setEnabled(true);
                SaturationModule.getInstance().setEnabled(true);
                BlockHighlightModule.getInstance().setEnabled(true);
            });
            context.waitTicks(30);
            context.takeScreenshot("world_first");

            server.runCommand("tp @a ~ ~ ~ 0 55");
            context.waitTicks(10);
            context.takeScreenshot("block_highlight");
            server.runCommand("tp @a ~ ~ ~ 0 5");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
            context.runOnClient(mc -> {
                com.fest.visuals.client.features.modules.render.ChinaHatModule.getInstance().setEnabled(true);
                WingsModule.getInstance().setEnabled(true);
            });
            context.waitTicks(10);
            context.takeScreenshot("wings_back");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
            context.runOnClient(mc -> WingsModule.getInstance().setEnabled(false));

            context.runOnClient(mc -> mc.setScreen(ScreenClickGUI.getInstance()));
            context.waitTicks(10);
            context.takeScreenshot("clickgui");
            context.runOnClient(mc -> mc.setScreen(null));
            context.waitTicks(5);
            context.takeScreenshot("world_last");
        }
    }
}
