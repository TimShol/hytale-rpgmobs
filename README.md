# EliteMobs

## Tiered elites that change combat, loot, and progression

EliteMobs was built to transform standard NPCs into tiered elites with stronger stats, distinct visuals, and improved rewards. It is configurable, supports runtime reloads, and is built for both casual and hardcore servers.

## Downloads
<table>
<tr>
<td align="center" width="50%">
<img src="icons/EliteMobs-Icon-128.png" alt="EliteMobs Plugin" width="150"/><br/><br/>
<strong>EliteMobs Plugin</strong><br/><br/>
<a href="https://www.curseforge.com/hytale/mods/elitemobs">
<img src="https://img.shields.io/badge/Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="Download EliteMobs"/>
</td>
</tr>
</table>

## Documentation

You can find all the documentation, configuration guides, and developer references on the docs site:

[![Docs](https://img.shields.io/badge/Documentation-2dc26b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.elitemobs.frotty27.com/)

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
- `core.yml` global systems, spawning, diagnostics
- `stats.yml` health and damage multipliers
- `gear.yml` equipment catalogs and restrictions
- `mobs.yml` NPC rules and weapon overrides
- `abilities.yml` abilities and gating
- `loot.yml` loot rules
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

EliteMobs is designed to work with other mods. If RPG Leveling is used, install the compatibility mod for proper nameplate and tier integration.

## License

MIT License
