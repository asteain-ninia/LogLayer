package com.asteain.loglayertest;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development-only check of the client's placement prediction (never in the mod jar): makes a flat creative world, places
 * oak logs on the top of the ground the way a player does ({@code MultiPlayerGameMode#useItemOn}), and compares the log the
 * client shows right after the click (its own prediction, before any answer from the server) with the server's log.
 * Result: {@code loglayer-check.txt} in the run folder ("PASS ..." or "FAIL ..."); the game then quits.
 */
@Mod(value = "loglayertest", dist = Dist.CLIENT)
public final class ClientCheck {
    private static final String WORLD = "loglayercheck";
    private static final Path RESULT = Path.of("loglayer-check.txt");

    /** One placement: sneaking or not, the yaw the player faces, and the axis expected on both sides. */
    private record Case(String name, boolean sneak, float yaw, Direction.Axis expected, int dx) {}

    private static final Case[] CASES = {
            new Case("sneak facing west", true, 90f, Direction.Axis.X, -2),
            new Case("sneak facing south", true, 0f, Direction.Axis.Z, 0),
            new Case("no sneak facing west", false, 90f, Direction.Axis.Y, 2),
    };

    private final List<String> lines = new ArrayList<>();
    private boolean failed;
    private boolean worldRequested;
    private int ticksInWorld = -1;
    private int caseIndex;
    private int caseTick;
    private BlockPos target;
    private BlockState predicted;

    public ClientCheck() {
        NeoForge.EVENT_BUS.addListener(this::tick);
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(300_000L); } catch (InterruptedException e) { return; }
            try { Files.writeString(RESULT, "FAIL watchdog: no result after 300 s"); } catch (Exception ignored) {}
            Runtime.getRuntime().halt(3);
        }, "loglayertest-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            // Once loading is over, whatever the first screen is (the title, or the accessibility onboarding of a fresh game folder).
            if (!worldRequested && mc.level == null && mc.getOverlay() == null && mc.screen != null) {
                worldRequested = true;
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, WorldOptions.defaultWithRandomSeed(),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.gameMode == null) return;
            ticksInWorld++;
            if (ticksInWorld == 40) {
                ServerPlayer server = serverPlayer(mc);
                mc.getSingleplayerServer().execute(() -> server.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LOG, 64)));
            }
            if (ticksInWorld < 80 || caseIndex >= CASES.length) return;
            step(mc, CASES[caseIndex]);
        } catch (Throwable t) {
            LogUtils.getLogger().error("[LOGLAYERTEST] failure", t);
            finish(mc, "FAIL " + t);
        }
    }

    private void step(Minecraft mc, Case c) throws Exception {
        caseTick++;
        // Hold the sneak key and the facing for a while first, so the server has heard both (as from a real player).
        mc.options.keyShift.setDown(c.sneak());
        mc.player.setYRot(c.yaw());
        mc.player.setXRot(60f);
        if (caseTick == 10) {
            BlockPos ground = mc.player.blockPosition().below().offset(c.dx(), 0, 2);
            target = ground.above();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
            String held = mc.player.getMainHandItem().toString();
            var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            // Read at once: this is the client's own prediction; the server has not answered yet.
            predicted = mc.level.getBlockState(target);
            lines.add("OBS " + c.name() + ": held " + held + ", sneaking " + mc.player.isShiftKeyDown() + ", useItemOn " + result
                    + ", client right after the click " + predicted);
        }
        if (caseTick == 30) {
            BlockState server = mc.getSingleplayerServer().submit(() -> mc.getSingleplayerServer().overworld().getBlockState(target)).join();
            BlockState client = mc.level.getBlockState(target);
            lines.add("OBS " + c.name() + ": server " + server + ", client after the server's answer " + client);
            check(c.name() + ": the client predicts the log along " + c.expected(), axis(predicted) == c.expected());
            check(c.name() + ": the server places the log along " + c.expected(), axis(server) == c.expected());
            check(c.name() + ": client and server agree afterwards", server.equals(client));
            mc.options.keyShift.setDown(false);
            caseIndex++;
            caseTick = 0;
            if (caseIndex == CASES.length) {
                int checks = (int) lines.stream().filter(l -> l.startsWith("ok ")).count();
                finish(mc, (failed ? "FAIL " : "PASS ") + checks + " checks" + System.lineSeparator() + String.join(System.lineSeparator(), lines));
            }
        }
    }

    private static Direction.Axis axis(BlockState state) {
        return state != null && state.hasProperty(BlockStateProperties.AXIS) ? state.getValue(BlockStateProperties.AXIS) : null;
    }

    private void check(String what, boolean ok) {
        lines.add((ok ? "ok " : "NOT ") + what);
        failed |= !ok;
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private void finish(Minecraft mc, String text) {
        caseIndex = CASES.length;
        try { Files.writeString(RESULT, text); } catch (Exception e) { LogUtils.getLogger().error("[LOGLAYERTEST] cannot write result", e); }
        LogUtils.getLogger().info("[LOGLAYERTEST] {}", text);
        mc.stop();
    }
}
