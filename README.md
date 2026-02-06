# EliteMobs

## Tiered elites that change combat, loot, and progression

EliteMobs transforms standard NPCs into tiered elites with stronger stats, distinct visuals, and improved rewards. It is configurable, supports runtime reloads, and is built for both casual and hardcore servers.

## Feature Highlights

### Combat

- Tiered health and damage scaling
- Ability mechanics (leaps, heals, summons)
- Clearer damage numbers for readability

### Loot & Gear

- Tiered loot tables and extra drops
- Rarity‑based equipment rolls
- Chance to drop equipped items

### Identity

- Tiered nameplates and family prefixes
- Optional model scaling per tier
- Distinct elite presence in the world

## Quick Start

```text
1. Install EliteMobs
2. Start the server to generate config
3. Edit YAML files and reload
```

## Links

[![CurseForge](https://img.shields.io/badge/CurseForge-EliteMobs-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/hytale/mods/elitemobs)

Compatibility Mods (expandable list):
- [EliteMobs RPG Leveling Compatibility](https://www.curseforge.com/hytale/mods/elitemobs-rpgleveling-compat)

## Documentation

Docs live in `docs/` and are built with Mintlify.

## Installation

1. Download the EliteMobs `.jar`.
2. Place it in your server `mods` folder.
3. Start the server to generate configuration files.

## Configuration

Config files live under:

```
%APPDATA%\Hytale\UserData\Saves\<save name>\mods\EliteMobs
```

Key files:
- `main.yml` global systems, spawning, scaling
- `mobs.yml` NPC rules and weapon overrides
- `abilities.yml` abilities and gating
- `drops.yml` loot rules
- `visuals.yml` nameplates and scaling

## Runtime Reload

```text
/elitemobs reload
```

Reloads YAML configuration without restarting the server. Some changes may require a restart to affect existing elites.

## Uninstalling

If you remove the mod, leftover elite NPCs can remain with modified stats and equipment. Do not kill them directly because it can crash the game. Use:

```text
/npc clean --confirm
```

Repeat until all remaining elite NPCs are removed.

## Compatibility

Elitemobs is designed to work with other mods. If you use RPG Leveling, install the compatibility mod for proper nameplate and tier integration.

## License

MIT License
