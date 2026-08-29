package com.fest.visuals.inject.other;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.api.event.events.player.move.CollisionEvent;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinAbstractBlockState {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("RETURN"), cancellable = true)
    private void removeXZCollision(BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (CollisionEvent.getInstance().call()) {
            VoxelShape original = cir.getReturnValue();
            double minY = original.min(Direction.Axis.Y);
            double maxY = original.max(Direction.Axis.Y);

            if (minY >= maxY) {
                maxY = minY + 0.001;
            }

            VoxelShape finalShape = Shapes.or(
                    Shapes.box(0.0, minY, 0.0, 0.0, maxY, 0.0)
            );

            cir.setReturnValue(finalShape);
        }
    }
}
