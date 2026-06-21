package com.asteain.loglayer;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for Easy Log Layer. Generated at
 * {@code <instance>/config/loglayer-common.toml} on first run.
 */
public final class Config {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue REQUIRE_SNEAK;
    public static final ModConfigSpec.BooleanValue AFFECT_ALL_AXIS_BLOCKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ENABLED = builder
                .comment("Master switch. When false the mod does nothing and logs place vanilla-style.")
                .define("enabled", true);

        REQUIRE_SNEAK = builder
                .comment(
                        "When true (default) logs are laid flat only while you sneak as you place them;",
                        "place normally for vanilla behaviour.",
                        "When false EVERY log you place is automatically laid flat along the direction you face.")
                .define("requireSneak", true);

        AFFECT_ALL_AXIS_BLOCKS = builder
                .comment(
                        "When true, also reorient non-log pillar blocks that use the vanilla axis property",
                        "(hay bales, basalt, bone blocks, quartz/purpur pillars, deepslate, etc.).",
                        "When false (default) only blocks in the #minecraft:logs tag are affected,",
                        "which covers every vanilla log/wood/stem and modded logs that register into that tag.")
                .define("affectAllAxisBlocks", false);

        SPEC = builder.build();
    }

    private Config() {
    }
}
