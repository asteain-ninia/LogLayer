package com.asteain.loglayer;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Decides how a log a player is placing should lie. Pure logic on block states, shared by both places that apply it:
 * {@link com.asteain.loglayer.mixin.BlockItemMixin} (the placement itself, on the client and the server alike) and
 * {@link LogPlacementHandler} (a server-side backstop for items that do not place through {@code BlockItem}).
 */
public final class LogOrientation {

    private LogOrientation() {
    }

    /**
     * Returns the state a log placed by {@code player} should end up in: {@code placed} laid flat along the axis the player
     * faces, or {@code placed} unchanged when the mod is off, the player does not sneak (while sneaking is required), the
     * block is not log-like, or vanilla already placed it horizontally.
     *
     * <p>The answer depends only on the config, the player's sneaking and facing, and the state, so the client's placement
     * prediction and the server compute the same result and the log appears flat at once.</p>
     */
    public static BlockState layFlat(BlockState placed, Player player) {
        if (placed == null || player == null || !Config.ENABLED.get()) {
            return placed;
        }
        if (Config.REQUIRE_SNEAK.get() && !player.isShiftKeyDown()) {
            return placed;
        }
        if (!isLogLike(placed)) {
            return placed;
        }
        // The horizontal axis the log should run along == the axis the player is facing.
        // Player#getDirection() is derived from yaw and is always one of N/S/E/W, so its
        // axis is always X or Z (never Y) -- exactly what we want for a flat log.
        BlockState reoriented = reorient(placed, player.getDirection().getAxis());
        return reoriented != null ? reoriented : placed;
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
