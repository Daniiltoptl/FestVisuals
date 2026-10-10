package com.fest.visuals.client.features.modules.utility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.system.configs.FriendManager;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.combat.Trajectory;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.display.WorldShapes;

/**
 * FunTime item helper: while one of the server's special items is in hand, shows the area it
 * will affect, and turns it green when using it right now would catch an enemy.
 *
 * <p>Radii come from the items' own lore on FunTime ("\u0440\u0430\u0434\u0438\u0443\u0441: 10 \u0431\u043B\u043E\u043A\u043E\u0432" and so on): \u0434\u0435\u0437\u043E\u0440\u0438\u0435\u043D\u0442\u0430\u0446\u0438\u044F,
 * \u044F\u0432\u043D\u0430\u044F \u043F\u044B\u043B\u044C and \u043E\u0433\u043D\u0435\u043D\u043D\u044B\u0439 \u0441\u043C\u0435\u0440\u0447 hit 10 blocks around you, \u0441\u043D\u0435\u0436\u043E\u043A-\u0437\u0430\u043C\u043E\u0440\u043E\u0437\u043A\u0430 7 around where it
 * lands, \u0431\u043E\u0436\u044C\u044F \u0430\u0443\u0440\u0430 2, \u0432\u0437\u0440\u044B\u0432\u043D\u0430\u044F \u0448\u0442\u0443\u0447\u043A\u0430 5. \u0422\u0440\u0430\u043F\u043A\u0438 are cages around your head block \u2014 5x5x5, 9x9x9
 * for the explosive one \u2014 and \u043F\u043B\u0430\u0441\u0442 is a 5x2x5 slab on the face you are looking at.
 *
 * <p>Items are recognised by type and by name, since FunTime reuses ordinary items (an ender eye
 * is only \u0434\u0435\u0437\u043E\u0440\u0438\u0435\u043D\u0442\u0430\u0446\u0438\u044F if the server named it so); the name check can be switched off for
 * servers that name them differently.
 */
@ModuleRegister(name = "FT Helper", desc = "\u0420\u0430\u0434\u0438\u0443\u0441 \u0434\u0435\u0437\u043A\u0438, \u043F\u044B\u043B\u0438, \u0442\u0440\u0430\u043F\u043A\u0438, \u043F\u043B\u0430\u0441\u0442\u0430 \u0438 \u0434\u0440. \u2014 \u0437\u0435\u043B\u0451\u043D\u044B\u0439, \u0435\u0441\u043B\u0438 \u0432\u0440\u0430\u0433 \u0432 \u0437\u043E\u043D\u0435", category = Category.OTHER)
public class FtHelperModule extends Module {
    @Getter private static final FtHelperModule instance = new FtHelperModule();

    public final BooleanSetting circles = new BooleanSetting("\u0414\u0435\u0437\u043A\u0430 / \u043F\u044B\u043B\u044C / \u0441\u043C\u0435\u0440\u0447 / \u0430\u0443\u0440\u0430").value(true);
    public final BooleanSetting traps = new BooleanSetting("\u0422\u0440\u0430\u043F\u043A\u0438 \u0438 \u0441\u0442\u0430\u043D").value(true);
    public final BooleanSetting plast = new BooleanSetting("\u041F\u043B\u0430\u0441\u0442").value(true);
    public final BooleanSetting snowball = new BooleanSetting("\u0421\u043D\u0435\u0436\u043E\u043A-\u0437\u0430\u043C\u043E\u0440\u043E\u0437\u043A\u0430").value(true);
    public final BooleanSetting strictNames = new BooleanSetting("\u041F\u0440\u043E\u0432\u0435\u0440\u044F\u0442\u044C \u043D\u0430\u0437\u0432\u0430\u043D\u0438\u0435").value(true);
    public final BooleanSetting fill = new BooleanSetting("\u0417\u0430\u043B\u0438\u0432\u043A\u0430").value(true);
    public final BooleanSetting markEnemies = new BooleanSetting("\u041E\u0442\u043C\u0435\u0447\u0430\u0442\u044C \u0432\u0440\u0430\u0433\u043E\u0432 \u0432 \u0437\u043E\u043D\u0435").value(true);
    public final SliderSetting lineWidth = new SliderSetting("\u0422\u043E\u043B\u0449\u0438\u043D\u0430").value(2f).range(0.5f, 6f).step(0.25f);
    public final ColorSetting color = new ColorSetting("\u0426\u0432\u0435\u0442").value(new Color(255, 90, 60, 230));
    public final ColorSetting enemyColor = new ColorSetting("\u0426\u0432\u0435\u0442, \u0435\u0441\u043B\u0438 \u0432\u0440\u0430\u0433 \u0432 \u0437\u043E\u043D\u0435").value(new Color(60, 255, 120, 240));

    private enum Shape { CIRCLE, CUBE, PLAST, IMPACT }

    /** One FunTime item: what it looks like, what it is called, and what it covers. */
    private record FtItem(String name, Item item, String keyword, Shape shape, double size) {}

    private static final List<FtItem> ITEMS = List.of(
            new FtItem("\u0414\u0435\u0437\u043E\u0440\u0438\u0435\u043D\u0442\u0430\u0446\u0438\u044F", Items.ENDER_EYE, "\u0434\u0435\u0437\u043E\u0440\u0438\u0435\u043D\u0442", Shape.CIRCLE, 10),
            new FtItem("\u042F\u0432\u043D\u0430\u044F \u043F\u044B\u043B\u044C", Items.SUGAR, "\u043F\u044B\u043B\u044C", Shape.CIRCLE, 10),
            new FtItem("\u041E\u0433\u043D\u0435\u043D\u043D\u044B\u0439 \u0441\u043C\u0435\u0440\u0447", Items.FIRE_CHARGE, "\u0441\u043C\u0435\u0440\u0447", Shape.CIRCLE, 10),
            new FtItem("\u0412\u0437\u0440\u044B\u0432\u043D\u0430\u044F \u0448\u0442\u0443\u0447\u043A\u0430", Items.FIRE_CHARGE, "\u0432\u0437\u0440\u044B\u0432\u043D", Shape.CIRCLE, 5),
            new FtItem("\u0411\u043E\u0436\u044C\u044F \u0430\u0443\u0440\u0430", Items.PHANTOM_MEMBRANE, "\u0430\u0443\u0440\u0430", Shape.CIRCLE, 2),
            new FtItem("\u0421\u043D\u0435\u0436\u043E\u043A-\u0437\u0430\u043C\u043E\u0440\u043E\u0437\u043A\u0430", Items.SNOWBALL, "\u0437\u0430\u043C\u043E\u0440\u043E\u0437", Shape.IMPACT, 7),
            new FtItem("\u0422\u0440\u0430\u043F\u043A\u0430", Items.NETHERITE_SCRAP, "\u0442\u0440\u0430\u043F", Shape.CUBE, 2),
            new FtItem("\u0422\u0440\u0430\u043F\u043A\u0430", Items.POPPED_CHORUS_FRUIT, "\u0442\u0440\u0430\u043F", Shape.CUBE, 2),
            new FtItem("\u0412\u0437\u0440\u044B\u0432\u043D\u0430\u044F \u0442\u0440\u0430\u043F\u043A\u0430", Items.PRISMARINE_SHARD, "\u0442\u0440\u0430\u043F", Shape.CUBE, 4),
            new FtItem("\u0421\u0442\u0430\u043D", Items.NETHER_STAR, "\u0441\u0442\u0430\u043D", Shape.CUBE, 15),
            new FtItem("\u041F\u043B\u0430\u0441\u0442", Items.DRIED_KELP, "\u043F\u043B\u0430\u0441\u0442", Shape.PLAST, 0)
    );

    /** Eased 0..1 values: how far the shape has grown in, and how green it currently is. */
    private float appear;
    private float alarm;
    private FtItem shown;
    private long shownSince;
    private long lastFrame;

    public FtHelperModule() {
        addSettings(circles, traps, plast, snowball, strictNames, fill, markEnemies, lineWidth, color, enemyColor);
    }

    @Override
    public void onEvent() {
        addEvents(Render3DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack(), event.partialTicks()))));
    }

    @Override
    public void onDisable() {
        shown = null;
        appear = 0f;
        alarm = 0f;
    }

    private boolean enabledFor(FtItem item) {
        return switch (item.shape()) {
            case CIRCLE -> circles.getValue();
            case CUBE -> traps.getValue();
            case PLAST -> plast.getValue();
            case IMPACT -> snowball.getValue();
        };
    }

    private static String plain(Component component) {
        return component.getString().replaceAll("\u00A7[0-9a-fk-or]", "").toLowerCase(Locale.ROOT);
    }

    private FtItem identify(ItemStack stack) {
        if (stack.isEmpty()) return null;

        String text = null;
        for (FtItem item : ITEMS) {
            if (!stack.is(item.item()) || !enabledFor(item)) continue;
            if (!strictNames.getValue()) return item;

            if (text == null) {
                StringBuilder builder = new StringBuilder(plain(stack.getHoverName()));
                ItemLore lore = stack.get(DataComponents.LORE);
                if (lore != null) {
                    for (Component line : lore.lines()) builder.append(' ').append(plain(line));
                }
                text = builder.toString();
            }
            if (text.contains(item.keyword())) return item;
        }
        return null;
    }

    private List<Player> enemies() {
        List<Player> list = new ArrayList<>();
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;
            if (FriendManager.getInstance().contains(player.getGameProfile().name())) continue;
            list.add(player);
        }
        return list;
    }

    private void render(PoseStack matrices, float partial) {
        if (mc.player == null || mc.level == null) return;

        FtItem item = identify(mc.player.getItemInHand(InteractionHand.MAIN_HAND));
        if (item == null) item = identify(mc.player.getItemInHand(InteractionHand.OFF_HAND));

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        if (item != null && item != shown) {
            shown = item;
            shownSince = now;
            appear = 0f;
        }
        appear += ((item != null ? 1f : 0f) - appear) * Math.min(1f, dt * 9f);
        if (item == null && appear < 0.01f) {
            shown = null;
            return;
        }
        if (shown == null) return;

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer buffer = RenderUtil.WORLD.occludedQuads();

        boolean caught = switch (shown.shape()) {
            case CIRCLE -> drawCircle(buffer, matrix, mc.player.getPosition(partial), shown.size(), now);
            case IMPACT -> drawImpact(buffer, matrix, partial, now);
            case CUBE -> drawCube(buffer, matrix, now);
            case PLAST -> drawPlast(buffer, matrix, now);
        };

        alarm += ((caught ? 1f : 0f) - alarm) * Math.min(1f, dt * 10f);
        RenderUtil.WORLD.endRender(matrices);
    }

    private Color tint(float alphaScale) {
        Color mixed = ColorUtil.interpolate(enemyColor.getValue(), color.getValue(), alarm);
        return ColorUtil.setAlpha(mixed, Math.round(mixed.getAlpha() * appear * alphaScale));
    }

    /** Grows in with an overshoot-free ease, so switching items reads as the zone unfolding. */
    private double grown(double radius, long now) {
        float t = Math.min(1f, (now - shownSince) / 420f);
        return radius * (1.0 - Math.pow(1.0 - t, 3.0));
    }

    private boolean drawCircle(VertexConsumer buffer, Matrix4f matrix, Vec3 feet, double radius, long now) {
        Vec3 centre = feet.add(0, 0.02, 0);
        double r = grown(radius, now);
        double line = lineWidth.getValue() * 0.05 * Math.max(1.0, radius / 5.0);

        boolean caught = false;
        List<Player> inside = new ArrayList<>();
        for (Player enemy : enemies()) {
            if (enemy.distanceTo(mc.player) <= radius) {
                caught = true;
                inside.add(enemy);
            }
        }

        Color main = tint(1f);
        int segments = (int) Math.min(160, 48 + radius * 8);

        if (fill.getValue()) {
            WorldShapes.disc(buffer, matrix, centre, r, segments, WorldShapes.argb(main, 0.06f), WorldShapes.argb(main, 0.32f));
            // A soft wall along the rim makes the edge readable from above and from the side.
            WorldShapes.wall(buffer, matrix, centre, r, 0.45 + 0.15 * alarm, segments, WorldShapes.argb(main, 0.35f), WorldShapes.argb(main, 0f));
        }

        WorldShapes.ring(buffer, matrix, centre, r - line, r, 0, Math.PI * 2, segments, main.getRGB(), main.getRGB());

        // Two arcs orbiting the rim, and a sonar sweep that pulses out from the centre.
        double spin = now / 900.0;
        for (int i = 0; i < 2; i++) {
            WorldShapes.ring(buffer, matrix, centre.add(0, 0.01, 0), r - line * 2.5, r + line * 0.5,
                    spin + i * Math.PI, Math.PI / 3, 24, WorldShapes.argb(main, 0.2f), main.getRGB());
        }
        float pulse = (now % 1600L) / 1600f;
        double pulseRadius = r * pulse;
        if (pulseRadius > line * 2) {
            WorldShapes.ring(buffer, matrix, centre, pulseRadius - line, pulseRadius, 0, Math.PI * 2, segments,
                    WorldShapes.argb(main, 0f), WorldShapes.argb(main, (1f - pulse) * 0.6f));
        }

        if (markEnemies.getValue()) {
            for (Player enemy : inside) markEnemy(buffer, matrix, enemy, now);
        }
        return caught;
    }

    private boolean drawImpact(VertexConsumer buffer, Matrix4f matrix, float partial, long now) {
        Trajectory.Launch launch = Trajectory.launchFor(mc.player, new ItemStack(Items.SNOWBALL));
        Trajectory.Result result = Trajectory.simulate(mc.player, launch, partial, 200);
        if (!result.landed()) return false;

        Vec3 impact = result.end();
        double radius = shown.size();

        // The flight path, faint, so the circle is clearly tied to where the snowball goes.
        List<Vec3> points = result.points();
        Color main = tint(1f);
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).distanceToSqr(points.get(0)) < 0.6) continue;
            WorldShapes.line(buffer, matrix, points.get(i - 1), points.get(i), 0.02,
                    WorldShapes.argb(main, 0.4f), WorldShapes.argb(main, 0.4f));
        }

        boolean caught = false;
        List<Player> inside = new ArrayList<>();
        for (Player enemy : enemies()) {
            if (enemy.position().distanceTo(impact) <= radius) {
                caught = true;
                inside.add(enemy);
            }
        }

        double r = grown(radius, now);
        double line = lineWidth.getValue() * 0.03;
        Vec3 centre = impact.add(0, 0.02, 0);
        int segments = 96;
        Color ring = tint(1f);

        if (fill.getValue()) {
            WorldShapes.disc(buffer, matrix, centre, r, segments, WorldShapes.argb(ring, 0.3f), WorldShapes.argb(ring, 0.08f));
            WorldShapes.wall(buffer, matrix, centre, r, 0.6, segments, WorldShapes.argb(ring, 0.3f), WorldShapes.argb(ring, 0f));
        }
        WorldShapes.ring(buffer, matrix, centre, r - line, r, 0, Math.PI * 2, segments, ring.getRGB(), ring.getRGB());

        if (markEnemies.getValue()) {
            for (Player enemy : inside) markEnemy(buffer, matrix, enemy, now);
        }
        return caught;
    }

    private boolean drawCube(VertexConsumer buffer, Matrix4f matrix, long now) {
        // Anchored to blocks, like the cage the server builds: centred on the block at head height.
        BlockPos head = mc.player.blockPosition().above();
        double half = shown.size();
        AABB zone = new AABB(head).inflate(half - 0.01);

        float t = Math.min(1f, (now - shownSince) / 420f);
        double ease = 1.0 - Math.pow(1.0 - t, 3.0);
        AABB drawn = zone.deflate((1.0 - ease) * half);

        boolean caught = false;
        List<Player> inside = new ArrayList<>();
        for (Player enemy : enemies()) {
            if (zone.intersects(enemy.getBoundingBox())) {
                caught = true;
                inside.add(enemy);
            }
        }

        Color main = tint(1f);
        double line = lineWidth.getValue() * 0.015;
        WorldShapes.boxOutline(buffer, matrix, drawn, line, main.getRGB());
        if (fill.getValue()) WorldShapes.boxFill(buffer, matrix, drawn, WorldShapes.argb(main, 0.16f));

        // A scan line sweeping up the cage.
        double sweep = (now % 1400L) / 1400.0;
        double y = drawn.minY + (drawn.maxY - drawn.minY) * sweep;
        WorldShapes.boxFill(buffer, matrix, new AABB(drawn.minX, y, drawn.minZ, drawn.maxX, y + 0.02, drawn.maxZ),
                WorldShapes.argb(main, (float) (0.5 * Math.sin(Math.PI * sweep))));

        if (markEnemies.getValue()) {
            for (Player enemy : inside) markEnemy(buffer, matrix, enemy, now);
        }
        return caught;
    }

    private boolean drawPlast(VertexConsumer buffer, Matrix4f matrix, long now) {
        if (!(mc.hitResult instanceof BlockHitResult hit)) return false;

        BlockPos target = hit.getBlockPos();
        Direction side = hit.getDirection();
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            float pitch = mc.player.getXRot();
            if (pitch > 45) {
                side = Direction.UP;
            } else if (pitch < -45) {
                side = Direction.DOWN;
            } else {
                side = mc.player.getDirection().getOpposite();
            }
        }
        int x = target.getX(), y = target.getY(), z = target.getZ();

        AABB zone = switch (side) {
            case UP -> new AABB(x - 2, y + 1, z - 2, x + 3, y + 3, z + 3);
            case DOWN -> new AABB(x - 2, y - 1, z - 2, x + 3, y + 1, z + 3);
            case NORTH, SOUTH -> new AABB(x - 2, y - 2, side == Direction.SOUTH ? z + 1 : z - 1,
                    x + 3, y + 3, side == Direction.SOUTH ? z + 3 : z + 1);
            default -> new AABB(side == Direction.EAST ? x + 1 : x - 1, y - 2, z - 2,
                    side == Direction.EAST ? x + 3 : x + 1, y + 3, z + 3);
        };

        boolean caught = false;
        for (Player enemy : enemies()) {
            if (zone.intersects(enemy.getBoundingBox())) caught = true;
        }

        Color main = tint(1f);
        WorldShapes.boxOutline(buffer, matrix, zone, lineWidth.getValue() * 0.015, main.getRGB());
        if (fill.getValue()) {
            float breathe = (float) (0.1 + 0.06 * Math.sin(now / 250.0));
            WorldShapes.boxFill(buffer, matrix, zone, WorldShapes.argb(main, breathe));
        }
        return caught;
    }

    /** A small bouncing ring under an enemy caught in the zone. */
    private void markEnemy(VertexConsumer buffer, Matrix4f matrix, Player enemy, long now) {
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 feet = enemy.getPosition(partial).add(0, 0.03, 0);
        double bounce = 0.55 + 0.1 * Math.sin(now / 120.0);
        Color mark = ColorUtil.setAlpha(enemyColor.getValue(), Math.round(enemyColor.getValue().getAlpha() * appear));
        WorldShapes.ring(buffer, matrix, feet, bounce - 0.06, bounce, 0, Math.PI * 2, 32, mark.getRGB(), mark.getRGB());
        WorldShapes.disc(buffer, matrix, feet, bounce, 32, WorldShapes.argb(mark, 0.25f), WorldShapes.argb(mark, 0.05f));
    }
}
