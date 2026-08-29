package com.fest.visuals.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.fest.visuals.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class PlayerUtil implements QuickImports {
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");

    public boolean isEating() {
        return mc.player.isUsingItem() && mc.player.getUseItem().getComponents().has(DataComponents.FOOD);
    }

    public boolean canSee(Vec3 to) {
        HitResult hitResult = mc.level.clip(new ClipContext(mc.getCameraEntity().getEyePosition(), to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.getCameraEntity()));
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    public boolean isAboveWater() {
        if (mc.player == null) return false;
        if (mc.level == null) return false;

        return mc.player.isUnderWater() || mc.level.getBlockState(mc.player.blockPosition().offset(0, (int) (-0.5), 0)).getBlock() == Blocks.WATER;
    }

    public boolean isInWeb() {
        if (mc.player == null) return false;
        AABB playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = mc.player.blockPosition();

        return getNearbyBlockPositions(playerPosition).stream().anyMatch(pos -> isBlockCobweb(playerBox, pos));
    }

    private boolean isBlockCobweb(AABB playerBox, BlockPos blockPos) {
        return playerBox.intersects(new AABB(blockPos)) && mc.level != null && mc.level.getBlockState(blockPos).getBlock() == Blocks.COBWEB;
    }

    public List<BlockPos> getNearbyBlockPositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = (center.getX() - 2); x <= (center.getX() + 2); x++) {
            for (int y = (center.getY() - 1); y <= (center.getY() + 4); y++) {
                for (int z = (center.getZ() - 2); z <= (center.getZ() + 2); z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    public Block getBlock(float x, float y, float z) {
        Vec3 pos = mc.player.position();
        return mc.level.getBlockState(new BlockPos(new Vec3i((int) (pos.x + x), (int) (pos.y + y), (int) (pos.z + z)))).getBlock();
    }

    public boolean hasCollisionWith(Entity entity) {
        return hasCollisionWith(entity, 0f);
    }

    public boolean hasCollisionWith(Entity entity, float expand) {
        AABB box = mc.player.getBoundingBox();
        AABB targetbox = entity.getBoundingBox().inflate(expand, 0, expand);

        return box.maxX > targetbox.minX
                && box.maxY > targetbox.minY
                && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX
                && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ;
    }

    public boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }
}
