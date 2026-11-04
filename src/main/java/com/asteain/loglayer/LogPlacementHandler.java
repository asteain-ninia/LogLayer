package com.asteain.loglayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Reorients freshly placed logs so they lie horizontally along the direction the player faces.
 *
 * <p>Triggered by {@link BlockEvent.EntityPlaceEvent}. In NeoForge 1.21.1, for item placement that
 * event is posted only from {@code CommonHooks#onPlaceItemIntoWorld}, which {@code ItemStack#useOn} calls solely when
 * {@code !level.isClientSide}; on the client {@code useOn} goes straight to the vanilla item code.
 * This handler therefore runs on the <em>logical server only</em> and takes no part in the
 * client's placement prediction: the client first shows the vanilla (vertical) log and switches
 * to the flat orientation once the server's block update and placement ack arrive. A brief
 * flicker is thus possible -- usually a few frames in single-player, longer on a high-latency
 * server. The orientation depends only on the player's facing, so the server's correction is
 * always the one the player expects and no extra networking is required.
 */
public final class LogPlacementHandler {

    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!Config.ENABLED.get()) {
            return;
        }

        // Players only (ignores dispensers, falling blocks, world-gen, other entities).
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (Config.REQUIRE_SNEAK.get() && !player.isShiftKeyDown()) {
            return;
        }

        LevelAccessor level = event.getLevel();

        BlockState placed = event.getPlacedBlock();
        if (placed == null || !isLogLike(placed)) {
            return;
        }

        // The horizontal axis the log should run along == the axis the player is facing.
        // Player#getDirection() is derived from yaw and is always one of N/S/E/W, so its
        // axis is always X or Z (never Y) -- exactly what we want for a flat log.
        Direction.Axis targetAxis = player.getDirection().getAxis();

        BlockState reoriented = reorient(placed, targetAxis);
        if (reoriented != null && reoriented != placed) {
            BlockPos pos = event.getPos();
            level.setBlock(pos, reoriented, Block.UPDATE_ALL);
        }
    }

    private static boolean isLogLike(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return true;
        }
        // Opt-in: treat every vanilla-axis pillar block as fair game.
        return Config.AFFECT_ALL_AXIS_BLOCKS.get() && state.hasProperty(BlockStateProperties.AXIS);
    }

    /**
     * Returns {@code state} reoriented horizontally along {@code axis}, or {@code null} if this
     * block exposes no orientation we know how to set, or if vanilla already placed it horizontally.
     *
     * <p>We only touch logs that vanilla placed <em>vertically</em>. Clicking a side face already
     * makes vanilla orient the log along that face (a horizontal axis), so those placements are
     * left untouched -- vanilla behaviour wins for side-face placement.
     */
    private static BlockState reorient(BlockState state, Direction.Axis axis) {
        // 1) Standard pillar logs -- covers virtually all vanilla and modded logs.
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            if (state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
                return null; // already horizontal (side-face placement) -> respect vanilla.
            }
            return state.setValue(BlockStateProperties.AXIS, axis);
        }
        // 2) Blocks that orient via a custom enum "state" property using plain_x / plain_y / plain_z
        //    (e.g. IndustrialUpgrade's rubber woods). Matched by serialized name only, so we need no
        //    compile-time dependency and never break if such a mod is absent or changes.
        return reorientNamedStateProperty(state, axis);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState reorientNamedStateProperty(BlockState state, Direction.Axis axis) {
        try {
            StateDefinition<Block, BlockState> definition = state.getBlock().getStateDefinition();
            Property<?> property = definition.getProperty("state");
            if (property == null) {
                return null;
            }

            String currentName = serializedName(state.getValue(property));
            // Only lay flat a log vanilla placed vertically (plain_y). Side-face placements
            // (plain_x / plain_z) and tapped states (dry_* / wet_*) are left untouched.
            if (!"plain_y".equals(currentName)) {
                return null;
            }

            String wanted = axis == Direction.Axis.X ? "plain_x" : "plain_z";
            for (Object value : property.getPossibleValues()) {
                if (wanted.equals(serializedName(value))) {
                    return state.setValue((Property) property, (Comparable) value);
                }
            }
        } catch (RuntimeException ignored) {
            // Defensive: an unexpected block layout simply means "leave it as placed".
        }
        return null;
    }

    private static String serializedName(Object value) {
        if (value instanceof StringRepresentable representable) {
            return representable.getSerializedName();
        }
        return value == null ? null : value.toString();
    }
}
