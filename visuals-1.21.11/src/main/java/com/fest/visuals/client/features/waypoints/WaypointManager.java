package com.fest.visuals.client.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fest.visuals.api.system.backend.ClientInfo;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.player.other.UpdateEvent;
import com.fest.visuals.api.system.interfaces.QuickImports;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

public class WaypointManager implements QuickImports {
    private static final WaypointManager INSTANCE = new WaypointManager();
    public static WaypointManager getInstance() { return INSTANCE; }

    @Getter private final List<Waypoint> waypoints = new ArrayList<>();
    @Getter @Setter private boolean autoDeathWaypoint = true;
    /** FunTime event announcements in chat become temporary marks. */
    @Getter @Setter private boolean autoEventWaypoints = true;
    private File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private boolean deadLastTick = false;

    public void init() {
        file = new File(ClientInfo.CONFIG_PATH_OTHER, "waypoints.json");
        load();
        UpdateEvent.getInstance().subscribe(new com.fest.visuals.api.event.Listener<>(this::onUpdate));
        com.fest.visuals.api.event.events.client.PacketEvent.getInstance().subscribe(new com.fest.visuals.api.event.Listener<>(event -> {
            if (!autoEventWaypoints || !event.isReceive()) return;
            String text = com.fest.visuals.api.utils.other.ChatPacketUtil.extractText(event.packet());
            if (text.isEmpty()) return;
            // Packets arrive on the network thread; waypoints are touched by the render thread.
            mc.execute(() -> FtEventWaypoints.onChat(text));
        }));
    }

    public void addWaypoint(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public void removeWaypoint(Waypoint wp) {
        waypoints.remove(wp);
        save();
    }

    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (waypoints.removeIf(wp -> wp.getExpiresAt() != 0 && wp.getExpiresAt() < now)) save();
        
        boolean isDead = !mc.player.isAlive();
        if (isDead && !deadLastTick && autoDeathWaypoint) {
            String dim = mc.level.dimension().toString();
            Waypoint deathWp = new Waypoint("Death", mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim, Color.RED, "skull");
            addWaypoint(deathWp);
        }
        deadLastTick = isDead;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("autoDeathWaypoint", autoDeathWaypoint);
            root.addProperty("autoEventWaypoints", autoEventWaypoints);
            JsonArray arr = new JsonArray();
            for (Waypoint wp : waypoints) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", wp.getId().toString());
                obj.addProperty("name", wp.getName());
                obj.addProperty("x", wp.getX());
                obj.addProperty("y", wp.getY());
                obj.addProperty("z", wp.getZ());
                obj.addProperty("dimension", wp.getDimension());
                obj.addProperty("color", wp.getColor().getRGB());
                obj.addProperty("icon", wp.getIcon());
                if (wp.getExpiresAt() != 0) obj.addProperty("expiresAt", wp.getExpiresAt());
                arr.add(obj);
            }
            root.add("waypoints", arr);
            FileWriter writer = new FileWriter(file);
            gson.toJson(root, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!file.exists()) return;
        try {
            FileReader reader = new FileReader(file);
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            reader.close();

            if (root.has("autoEventWaypoints")) {
                autoEventWaypoints = root.get("autoEventWaypoints").getAsBoolean();
            }
            if (root.has("autoDeathWaypoint")) {
                autoDeathWaypoint = root.get("autoDeathWaypoint").getAsBoolean();
            }

            if (root.has("waypoints")) {
                waypoints.clear();
                JsonArray arr = root.getAsJsonArray("waypoints");
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    Waypoint wp = new Waypoint(
                            obj.get("name").getAsString(),
                            obj.get("x").getAsDouble(),
                            obj.get("y").getAsDouble(),
                            obj.get("z").getAsDouble(),
                            obj.get("dimension").getAsString(),
                            new Color(obj.get("color").getAsInt(), true),
                            obj.get("icon").getAsString()
                    );
                    if (obj.has("expiresAt")) {
                        wp.setExpiresAt(obj.get("expiresAt").getAsLong());
                    }
                    if (obj.has("id")) {
                        wp.setId(UUID.fromString(obj.get("id").getAsString()));
                    }
                    waypoints.add(wp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




