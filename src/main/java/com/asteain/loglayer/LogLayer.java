package com.asteain.loglayer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Easy Log Layer.
 *
 * <p>The decision lives in {@link LogOrientation}. It is applied while a {@code BlockItem} chooses its placement state
 * ({@link com.asteain.loglayer.mixin.BlockItemMixin}), which runs on both the client (placement prediction) and the
 * server, so the log appears flat at once; {@link LogPlacementHandler} is a server-side backstop for placements that do
 * not go through {@code BlockItem}. There is no client-only code and no networking: single-player and dedicated servers
 * behave identically.
 */
@Mod(LogLayer.MOD_ID)
public final class LogLayer {

    public static final String MOD_ID = "loglayer";

    public LogLayer(IEventBus modBus, ModContainer container) {
        // Configuration file: <instance>/config/loglayer-common.toml
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // The backstop for placements outside BlockItem (the main path is the mixin).
        NeoForge.EVENT_BUS.register(new LogPlacementHandler());
    }
}
