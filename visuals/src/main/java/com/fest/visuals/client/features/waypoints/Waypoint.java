package com.fest.visuals.client.features.waypoints;

import java.awt.Color;
import java.util.UUID;

public class Waypoint {
    private UUID id;
    private String name;
    private double x, y, z;
    private String dimension;
    private Color color;
    private String icon;
    /** Epoch millis after which the mark removes itself; zero for a permanent one. */
    private long expiresAt;

    public Waypoint(String name, double x, double y, double z, String dimension, Color color, String icon) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.color = color;
        this.icon = icon;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
