package com.asteain.loglayer.mixin;

import com.asteain.loglayer.LogOrientation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lays the log flat while its placement state is being chosen, before {@code BlockItem#canPlace} checks it.
 *
 * <p>{@code BlockItem#getPlacementState} runs on the client (placement prediction) and on the server, so both put down
 * the same flat log and the client never shows the vertical one first. (NeoForge's {@code EntityPlaceEvent} fires on the
 * server only, after the block is set: reorienting there made the log flicker; 1.1.0 and before.)</p>
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @ModifyExpressionValue(method = "getPlacementState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState loglayer$layFlat(BlockState placed, BlockPlaceContext context) {
        return LogOrientation.layFlat(placed, context.getPlayer());
    }
}
