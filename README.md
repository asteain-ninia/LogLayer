# Easy Log Layer

A tiny Minecraft **1.21.1 / NeoForge** mod that makes it effortless to place logs horizontally.

## What it does

**Sneak while placing a log on the top or bottom of a block and it lies down flat, running
along the direction you are facing.**

- Face east/west → the log runs east–west.
- Face north/south → the log runs north–south.
- Place *without* sneaking for normal vanilla behaviour.
- Placing against a **side face** keeps vanilla behaviour (vanilla already orients the log along
  the clicked face), so you never lose that control.

No items, no recipes, no GUI — just a better placement feel. There is no placement flicker: the
correct horizontal orientation is predicted client-side, so the log appears flat immediately.

## Mod log support

Works on every block in the `#minecraft:logs` tag, which means **all vanilla logs, woods,
stems and hyphae**, plus any modded logs that register into that tag. From the target
modpack that includes **IndustrialUpgrade's rubber / swamp rubber / tropical rubber woods**,
which use a non-standard orientation property — they are handled explicitly (by name, with no
hard dependency, so nothing breaks if the mod is absent or updated).

## Configuration

Generated at `config/loglayer-common.toml` on first launch:

| Option | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master switch. |
| `requireSneak` | `true` | If `true`, only sneak-placed logs are laid flat. Set `false` to lay **every** placed log flat automatically. |
| `affectAllAxisBlocks` | `false` | If `true`, also reorients non-log pillar blocks that use the vanilla axis property (hay bales, basalt, bone blocks, quartz/purpur pillars, deepslate, …). |

## How it works

Everything runs on the logical server via `BlockEvent.EntityPlaceEvent` — there is no
client-only code and no networking, so single-player and dedicated servers behave identically.
When a player places a log-like block, its orientation is set to the player's horizontal facing
axis (`axis` property for standard logs; the custom `state` enum for IndustrialUpgrade woods).

## Building

Requires JDK 21.

```bash
./gradlew build
```

The mod jar is produced at `build/libs/loglayer-1.0.0.jar`.
