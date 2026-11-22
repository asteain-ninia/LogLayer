package com.asteain.loglayer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Server-side backstop: lays flat a log that was placed without going through {@code BlockItem#getPlacementState}
 * (for example by a modded item class that overrides it), where {@link com.asteain.loglayer.mixin.BlockItemMixin}
 * cannot reach.
 *
 * <p>Triggered by {@link BlockEvent.EntityPlaceEvent}. In NeoForge 1.21.1, for item placement that
 * event is posted only from {@code CommonHooks#onPlaceItemIntoWorld}, which {@code ItemStack#useOn} calls solely when
 * {@code !level.isClientSide}; so this runs on the <em>logical server only</em>, after the block is set. For such logs
 * the client first shows the vanilla (vertical) log and switches once the server's block update arrives (a brief flicker).
 * Logs placed through {@code BlockItem} already arrive here flat, and are left as they are.
 */
public final class LogPlacementHandler {

    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        // Players only (ignores dispensers, falling blocks, world-gen, other entities).
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        BlockState reoriented = LogOrientation.layFlat(placed, player);
        if (reoriented != placed) {
            event.getLevel().setBlock(event.getPos(), reoriented, Block.UPDATE_ALL);
        }
    }
}
