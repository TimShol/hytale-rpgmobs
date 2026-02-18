# RPGMobs

## Tiered elites that change combat, loot, and progression

RPGMobs transforms standard Hytale NPCs into tiered elites with scaling stats, combat abilities, tiered loot, and
distinct visuals. Fully configurable with runtime reloads, event-driven modding API, and support for both casual and
hardcore servers.

## Downloads

<table>
<tr>
<td align="center" width="50%">
<img src="icons/RPGMobs-Icon-128.png" alt="Plugin" width="128"/>
<br/><br/>
<strong>Plugin</strong>
<br/><br/>
<a href="https://www.curseforge.com/hytale/mods/rpgmobs">
<img src="https://img.shields.io/badge/Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="Download RPGMobs"/>
</a>
</td>
<td align="center" width="50%">
<img src="icons/RPGMobs-Icon-128.png" alt="API" width="128"/>
<br/><br/>
<strong>API</strong>
<br/><br/>
<a href="https://www.curseforge.com/hytale/mods/rpgmobs-api">
<img src="https://img.shields.io/badge/Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="Download RPGMobs API"/>
</a>
</td>
</tr>
</table>

## Documentation

Full configuration guides, developer API reference, and troubleshooting:

[![Docs](https://img.shields.io/badge/Documentation-2dc26b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.RPGMobs.frotty27.com/)

## Feature Highlights

### Combat

- 5 power tiers with independent health and damage scaling
- Combat abilities: Charge Leap, Heal Potion, Undead Summon
- Random damage and health variance for less predictable encounters
- Ability gating by tier, mob family, and weapon type

### Loot & Gear

- Tiered loot tables with configurable drop multipliers
- Rarity-weighted equipment from curated weapon and armor catalogs
- Chance to drop equipped items on death
- Consumable drops (potions, gems, materials)

### Identity

- Tiered nameplates with rank indicators and family prefixes
- Model scaling per tier for distinct visual presence
- Status effects with particle systems

### Progression

- Three progression styles: Environment (zone-based), Distance from Spawn, or Random
- Per-zone tier distribution with configurable weights
- Distance-based stat bonuses for smooth difficulty curves

### For Developers

- Event-driven API with 12 event types (spawn, death, damage, abilities, aggro, loot)
- Read-only Query API for inspecting any RPG mob's state
- Cancellable events for spawn blocking, damage modification, and loot customization
- Separate API artifact for compile-time dependency

## Quick Start

```text
1. Download RPGMobs and place the JAR in your server's mods folder
2. Start the server to generate default configuration
3. Edit the YAML files under your save's mods/RPGMobs directory
4. Run /RPGMobs reload to apply changes without restarting
```

## Installation

1. Download the RPGMobs `.jar` from CurseForge.
2. Place it in your server `mods` folder.
3. Start the server to generate configuration files.
4. Configuration files are created under:

```
%APPDATA%\Hytale\UserData\Saves\<save name>\mods\RPGMobs
```

## Configuration

| File              | Purpose                                              |
|:------------------|:-----------------------------------------------------|
| `core.yml`        | Global systems, reconciliation, debug, compatibility |
| `stats.yml`       | Health and damage multipliers per tier               |
| `spawning.yml`    | Progression style, spawn chances, zone distributions |
| `gear.yml`        | Equipment catalogs, rarity rules, armor materials    |
| `loot.yml`        | Drop rates, loot multipliers, extra drops            |
| `abilities.yml`   | Ability toggles, cooldowns, per-tier scaling, gating |
| `visuals.yml`     | Nameplates, model scaling                            |
| `effects.yml`     | Status effects and particles                         |
| `consumables.yml` | Consumable drop definitions                          |
| `mobrules.yml`    | NPC rules, weapon overrides, family assignments      |

## Runtime Reload

```text
/rpgmobs reload
```

Reloads all YAML configuration from disk. Spawn logic updates immediately. Existing elites are reconciled over a
configurable tick window.

## API Overview

RPGMobs ships a separate `rpgmobs-api` artifact for mod developers. Add it as a compile-time dependency to listen to
events, query RPG mob state, or modify loot and damage.

See the [API documentation](https://docs.RPGMobs.frotty27.com/api/overview) for integration details.

## Uninstalling

If you remove the mod, leftover elite NPCs can remain with modified stats and equipment. Use:

```text
/npc clean --confirm
```

Repeat until all remaining elite NPCs are removed. Do not kill them directly as it can crash the game.

## Compatibility

RPGMobs works alongside other Hytale mods. Nameplate rendering is handled by
[NameplateBuilder](https://github.com/TimShol/hytale-nameplate-builder), which provides the tiered nameplate
display used by RPGMobs. Mod developers looking to extend or interact with RPGMobs should use the
`rpgmobs-api` artifact — see the [API documentation](https://docs.RPGMobs.frotty27.com/api/overview) for
integration details.

[![GitHub](https://img.shields.io/badge/GitHub-NameplateBuilder-7C3AED?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TimShol/hytale-nameplate-builder)

## License

This project is licensed under the [MIT License](LICENSE).
