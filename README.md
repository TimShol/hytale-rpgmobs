# EliteMobs

EliteMobs makes combat more dangerous, rewarding, and dynamic by introducing tiered variants of mobs with special abilities, enhanced stats, visual feedback, and improved loot. It is highly configurable and designed for both casual servers and hardcore experiences, while remaining compatible with other mods.

Currently enhancing 162 NPC types (more to come).

## Features

- **Elite abilities**: Mobs gain special abilities such as leaps, resistances, and conditional effects. Scaling per tier makes higher-tier elites significantly more dangerous.
- **Enhanced combat feedback**: Combat text overlay with clearer damage numbers.
- **Enhanced loot**: Tiered loot tables, armor and weapon equips by tier, and per-item drop chances.
- **Runtime config reload**: Update configs without restarting the server.

## Documentation

Docs live in `docs/` and are built with Mintlify.

## Installation

1. Download the correct EliteMobs `.jar`.
2. Place it in your server `mods` folder.
3. Start the server. Config files will be generated automatically.

## Configuration

Almost everything is configurable under:

`%APPDATA%\\Hytale\\UserData\\Saves\\<save name>\\mods\\EliteMobs`

The `Server/` folder is an asset pack generated at runtime from the `.yml` files.

## Runtime Reload

`/elitemobs reload`

This reloads configuration files without restarting the server. Some changes that affect already-spawned mobs or generated assets may need a restart to fully apply.

## Uninstalling

If you remove the mod, leftover NPCs will still have changed stats, equipment, and nameplates. Do **not** try to kill them. Instead:

`/npc clean --confirm`

Repeat until all remaining EliteMobs are removed.

## Permissions

- `elitemobs.reload` — allows runtime config reload.

## Compatibility

Designed to be compatible with any mod. For mods that scale damage, multipliers can stack and may impact balance.

Recommended companion mod:
- Perfect Parries

## Roadmap

- Advanced abilities with conditional triggers and combos
- Advanced loot systems with better scaling and rare elite drops
- Combat flow improvements (faster potion and food use)
- Smarter difficulty scaling over time

## Local Structure

- `docs/` Mintlify documentation site
- `Server/` generated templates and assets
- `*.yml` core configuration files

## License

MIT License
