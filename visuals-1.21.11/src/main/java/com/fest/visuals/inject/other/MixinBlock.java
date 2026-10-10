package com.fest.visuals.inject.other;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.player.world.BlockPlaceEvent;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    public void blockPlaceHook(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack, CallbackInfo callbackInfo) {
        BlockPlaceEvent.getInstance().call(new BlockPlaceEvent.BlockPlaceEventData((Block) (Object) this, state, pos, placer));
    }
}
