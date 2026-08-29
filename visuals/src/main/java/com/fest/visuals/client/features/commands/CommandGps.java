package com.fest.visuals.client.features.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import com.fest.visuals.api.command.Command;
import com.fest.visuals.api.command.CommandRegister;
import com.fest.visuals.client.features.waypoints.Waypoint;
import com.fest.visuals.client.features.waypoints.WaypointManager;
import com.fest.visuals.api.utils.color.UIColors;

import java.util.List;
import java.util.stream.Collectors;

@CommandRegister(name = "gps")
public class CommandGps extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("add")
                .then(argument("x", DoubleArgumentType.doubleArg())
                .then(argument("y", DoubleArgumentType.doubleArg())
                .then(argument("z", DoubleArgumentType.doubleArg())
                .then(argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            double x = DoubleArgumentType.getDouble(context, "x");
                            double y = DoubleArgumentType.getDouble(context, "y");
                            double z = DoubleArgumentType.getDouble(context, "z");
                            String name = StringArgumentType.getString(context, "name");
                            
                            if (mc.level == null) return SINGLE_SUCCESS;
                            String dim = mc.level.dimension().toString();
                            
                            Waypoint wp = new Waypoint(name, x, y, z, dim, UIColors.primary(255), "COORDS");
                            WaypointManager.getInstance().addWaypoint(wp);
                            print("Добавлена метка: " + name + " на координатах " + x + " " + y + " " + z);
                            
                            return SINGLE_SUCCESS;
                        }))))));

        builder.then(literal("remove")
                .then(argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder1) -> {
                            List<String> names = WaypointManager.getInstance().getWaypoints().stream().map(Waypoint::getName).collect(Collectors.toList());
                            return SharedSuggestionProvider.suggest(names, builder1);
                        })
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            Waypoint toRemove = null;
                            for (Waypoint wp : WaypointManager.getInstance().getWaypoints()) {
                                if (wp.getName().equalsIgnoreCase(name)) {
                                    toRemove = wp;
                                    break;
                                }
                            }
                            if (toRemove != null) {
                                WaypointManager.getInstance().removeWaypoint(toRemove);
                                print("Метка " + name + " удалена.");
                            } else {
                                print("Метка с таким названием не найдена.");
                            }
                            return SINGLE_SUCCESS;
                        })));
    }
}