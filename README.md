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

No items, no recipes, no GUI — just a better placement feel. The log appears flat immediately, with
no flicker, when the mod is installed on both the client and the server (see *How it works*).

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

When a player places a log-like block, its orientation is set to the player's horizontal facing
axis (`axis` property for standard logs; the custom `state` enum for IndustrialUpgrade woods).

This happens while `BlockItem#getPlacementState` chooses the state to place (a mixin), which runs
both in the client's placement prediction and on the server. Both sides compute the same flat log
from the same inputs (config, sneaking, facing), so the client shows it flat at once. There is no
client-only code and no networking, so single-player and dedicated servers behave identically.

- **Install it on both sides** for flicker-free placement. With the mod on the server only, logs are
  still laid flat, but the client first shows the vertical log until the server's block update
  arrives (a brief flicker). With the mod on the client only, the client predicts a flat log and the
  server's vertical one replaces it.
- The config is read on each side, so keep `requireSneak` and the other options the same on the
  client and the server; otherwise the server's choice wins after a brief flicker.
- A server-side backstop (`BlockEvent.EntityPlaceEvent`, which NeoForge posts on the server only)
  still lays flat logs placed by modded items that do not place through `BlockItem`; those may
  flicker as before.

Up to 1.1.0 the reorientation was done only in that server-side event, after the block was set, so
the client always showed the vertical log first.

## Building

Requires JDK 21.

```bash
./gradlew build
```

The mod jar is produced at `build/libs/loglayer-<version>.jar`, where `<version>` is
`mod_version` from `gradle.properties`.

```bash
./gradlew runClientCheck
```

opens a real client, makes a flat creative world, places oak logs (sneaking facing west and south, and
not sneaking), and checks that the log the client shows right after the click — its own prediction,
before the server answers — already has the server's orientation. The result is written to
`run-clientcheck/loglayer-check.txt` and the game quits. The check lives in `src/clienttest` and is not
part of the mod jar. (With the mixin left out, the two sneaking cases fail: the client predicts `axis=y`.)
