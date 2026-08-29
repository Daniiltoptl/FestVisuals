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
    private File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private boolean deadLastTick = false;

    public void init() {
        file = new File(ClientInfo.CONFIG_PATH_OTHER, "waypoints.json");
        load();
        UpdateEvent.getInstance().subscribe(new com.fest.visuals.api.event.Listener<>(this::onUpdate));
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




