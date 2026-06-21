package com.asteain.loglayer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Easy Log Layer.
 *
 * <p>The mod's whole behaviour lives in {@link LogPlacementHandler}, which listens for block
 * placements on the game event bus. There is no client-only code and no networking: everything
 * runs on the logical server, so it works identically in single-player and on dedicated servers.
 */
@Mod(LogLayer.MOD_ID)
public final class LogLayer {

    public static final String MOD_ID = "loglayer";

    public LogLayer(IEventBus modBus, ModContainer container) {
        // Configuration file: <instance>/config/loglayer-common.toml
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Gameplay behaviour is driven by events fired on the game (NeoForge) bus.
        NeoForge.EVENT_BUS.register(new LogPlacementHandler());
    }
}
