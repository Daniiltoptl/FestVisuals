package com.fest.visuals.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.fest.visuals.api.event.events.Event;

public class BlockPlaceEvent extends Event<BlockPlaceEvent.BlockPlaceEventData> {
    @Getter private static final BlockPlaceEvent instance = new BlockPlaceEvent();

    public record BlockPlaceEventData(Block block, BlockState state, BlockPos pos, LivingEntity placer) {}
}
