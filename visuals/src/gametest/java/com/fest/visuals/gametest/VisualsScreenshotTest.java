package com.fest.visuals.gametest;

import java.nio.file.Path;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.fest.visuals.client.features.modules.hud.SaturationModule;
import com.fest.visuals.client.features.waypoints.Waypoint;
import com.fest.visuals.client.features.waypoints.WaypointManager;
import com.fest.visuals.client.ui.clickgui.ClickGuiTab;
import com.fest.visuals.client.ui.clickgui.ScreenClickGUI;
import com.fest.visuals.client.features.modules.render.BlockHighlightModule;
import com.fest.visuals.client.features.modules.render.CrosshairModule;
import com.fest.visuals.client.features.modules.render.HitboxModule;
import com.fest.visuals.client.features.modules.render.TrajectoryModule;
import com.fest.visuals.client.features.modules.utility.FtHelperModule;

/**
 * Sets up a small scene and screenshots the world visuals, so they can be checked without
 * playing: a mob for the hitboxes, and FunTime items and a snowball in hand for the helpers.
 */
public class VisualsScreenshotTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getClientLevel().waitForChunksRender();
            TestServerContext server = world.getServer();

            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            context.getInput().lookAt(0f, 12f);
            context.waitTicks(5);
            server.runCommand("execute as @a at @s run summon villager ^3 ^ ^5 {NoAI:1b,PersistenceRequired:1b}");

            context.runOnClient(mc -> {
                HitboxModule.getInstance().mobs.setValue(true);
                HitboxModule.getInstance().setEnabled(true);
                TrajectoryModule.getInstance().setEnabled(true);
                FtHelperModule.getInstance().strictNames.setValue(false);
                FtHelperModule.getInstance().setEnabled(true);
                CrosshairModule.getInstance().setEnabled(true);
                SaturationModule.getInstance().setEnabled(true);
                BlockHighlightModule.getInstance().setEnabled(true);
            });
            context.waitTicks(20);

            hold(context, server, "netherite_scrap");
            shot(context, "ft_trap");
            context.getInput().lookAt(0f, 55f);
            context.waitTicks(5);
            shot(context, "ft_trap_down");

            hold(context, server, "sugar");
            shot(context, "ft_circle_down");
            context.getInput().lookAt(0f, 12f);
            context.waitTicks(5);
            shot(context, "ft_circle");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
            context.waitTicks(5);
            shot(context, "ft_circle_third");
            context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

            hold(context, server, "snowball");
            shot(context, "trajectory_snowball");

            hold(context, server, "bow");
            server.runCommand("give @a minecraft:arrow 16");
            context.getInput().holdKey(options -> options.keyUse);
            context.waitTicks(25);
            shot(context, "trajectory_bow");
            context.getInput().releaseKey(options -> options.keyUse);

            hold(context, server, "air");
            server.runCommand("execute at @a run setblock ~ ~ ~2 minecraft:short_grass");
            server.runCommand("execute at @a run setblock ~1 ~ ~2 minecraft:oak_slab");
            server.runCommand("execute at @a run setblock ~-1 ~ ~2 minecraft:oak_fence");
            context.getInput().lookAt(0f, 35f);
            context.waitTicks(10);
            shot(context, "outline_grass");
            context.getInput().lookAt(-25f, 30f);
            context.waitTicks(10);
            shot(context, "outline_slab");

            server.runCommand("gamemode survival @a");
            hold(context, server, "diamond_sword");
            context.getInput().lookAt(0f, -20f);
            context.waitTicks(10);
            shot(context, "hud");

            context.runOnClient(mc -> {
                WaypointManager manager = WaypointManager.getInstance();
                String dimension = mc.level.dimension().toString();
                manager.addWaypoint(new Waypoint("База", 120, 64, -340, dimension, new java.awt.Color(70, 170, 255), "COORDS"));
                Waypoint event = new Waypoint("Мистический сундук", 800, 70, 1200, dimension, new java.awt.Color(200, 110, 255), "COORDS");
                event.setExpiresAt(System.currentTimeMillis() + 4 * 60_000L);
                manager.addWaypoint(event);
            });
            context.setScreen(ScreenClickGUI::getInstance);
            context.runOnClient(mc -> ScreenClickGUI.getInstance().switchTo(ClickGuiTab.WAYPOINTS));
            context.waitTicks(30);
            shot(context, "waypoints");
        }
    }

    private static void hold(ClientGameTestContext context, TestServerContext server, String item) {
        server.runCommand("item replace entity @a hotbar.0 with minecraft:" + item);
        context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
        context.waitTicks(15);
    }

    private static void shot(ClientGameTestContext context, String name) {
        Path path = context.takeScreenshot(name);
        System.out.println("[festvisuals-gametest] screenshot " + name + " -> " + path.toAbsolutePath());
    }
}
