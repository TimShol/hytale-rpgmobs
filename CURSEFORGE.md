# RPGMobs

_(Rebranded from EliteMobs)_

### Adds tiered elite variants to every _configured_ NPC — with <span style="color:#3598db">scaling</span>, <span style="color:#2dc26b">abilities</span>, <span style="color:#f1c40f">loot</span> and <span style="color:#e67e23">equipment</span> to your world!

![RPGMobs Ability](https://media.forgecdn.net/attachments/1495/784/eliteleapability-gif.gif)

Currently enhancing <span style="color:#2dc26b">165 NPC types</span> — and more to come!

## Documentation / Guide

You can find all the documentation, configuration guides, and developer references on my docs site.

[![Docs](https://img.shields.io/badge/Documentation_/_Guide-3da15b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.rpgmobs.frotty27.com/)

[![GitHub](https://img.shields.io/badge/GitHub-RPGMobs-3da15b?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TimShol/hytale-rpgmobs)

***

## Combat

*   5 power tiers with independent health and damage scaling
*   Random damage and health variance for less predictable encounters
*   Ability gating by tier, mob family, and weapon type
*   Projectile resistance for higher-tier elites with visual particles

### Abilities

Elites use an event-driven ability system — abilities trigger contextually based on combat state, not on a timer. There are currently 3 abilities:

*   **Charge Leap** — Gap-closing attack. Triggers when a target is within range.
*   **Heal Leap** — Defensive retreat with self-heal. Triggers when health drops below a threshold.
*   **Undead Summon** — Spawns reinforcements. Triggers periodically during sustained combat.

More abilities and new AI behaviours are actively in development, including conditional combos, environmental awareness, and tier-exclusive mechanics that will make each encounter feel distinct.

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/1498/352/tiers-png.png" alt="Tiers"></p></div>

## Loot & Gear

*   Tiered loot tables with configurable drop multipliers (0x to 6x)
*   Rarity-weighted equipment from curated weapon and armor catalogs
*   Chance to drop equipped items on death
*   Consumable drops including food, potions, gems, and materials
*   30+ custom loot entries (ores, bars, gems, life essence, …)

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/1498/353/loot-png.png" alt="Loot"></p></div>

## Visual Identity

*   Tiered nameplates with rank indicators and family prefixes via [NameplateBuilder](https://github.com/TimShol/hytale-nameplate-builder)
*   Model scaling per tier for distinct visual presence
*   Ability and status effects with particle systems

## Progression

There are 3 progression styles currently:

*   **Environment (zone-based):** Tier distribution is determined by the Hytale zone the mob spawns in. Each zone (Zone 1 through Zone 4) has configurable tier weights, letting you create smooth difficulty curves across the world map. For example, Zone 1 might heavily favor Tier 1 and 2, while Zone 4 spawns mostly Tier 4 and 5 elites.
*   **Distance from Spawn:** Tier selection and stat bonuses scale based on how far the mob spawns from the world origin. The further out players explore, the stronger the elites become. Health and damage bonuses increase gradually with distance, creating a natural difficulty gradient without relying on zone tags.
*   **Random (None):** Any tier can spawn anywhere with equal probability. Useful for arena-style servers or testing.

## Configuration

<span style="color:#2dc26b"><strong>Almost everything is configurable!</strong></span> RPGMobs generates 10 YAML configuration files under:

```
%APPDATA%\Hytale\UserData\Saves\(your save name)\mods\RPGMobs
```

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/1498/351/configdirectory-png.png" alt="Config Directory"></p></div>

*   Live reload via `/rpgmobs reload` with automatic reconciliation of existing elites
*   Every feature is independently toggleable
*   Config version tracking with automatic regeneration on major updates
*   Missing keys are regenerated automatically — no need to start fresh after an update

## Asset Generation

*   The generated `Server` folder inside the mod directory is the runtime asset pack — no need to edit it manually

## Permissions

*   **`rpgmobs.reload`** — Allows the player to reload RPGMobs configuration at runtime

***

## Installation

*   Download the RPGMobs `.jar` from CurseForge
*   Place it in your server's `mods` folder
*   Start the server to generate default configuration
*   Edit the YAML files under `%APPDATA%\Hytale\UserData\Saves\(your save name)\mods\RPGMobs`
*   Run `/rpgmobs reload` to apply changes without restarting

## Uninstalling

If you remove the mod, leftover elite NPCs can remain with modified stats and equipment. Do **not** try to kill them — the game will crash since it can no longer find the mod's code.

Instead, use `/npc clean --confirm` and repeat until all remaining elite NPCs are removed.

## Compatibility

RPGMobs (should) work alongside other Hytale mods. Nameplate rendering is handled by [NameplateBuilder](https://www.curseforge.com/hytale/mods/nameplatebuilder).

Custom weapons and armor from other mods can be added to the equipment catalogs in `gear.yml`.

✅ **Strongly recommended to play alongside:**

**[Perfect Parries](https://www.curseforge.com/hytale/mods/perfect-parries)** — Adds more depth and skill-based combat, especially against elite enemies.

**[RPGLeveling](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats)** — Adds experience, levels, zones, (even more) difficulty scaling and stat-based progression.

***

## Development Status

<span style="color:#2dc26b"><strong>RPGMobs is actively in development.</strong></span>

*   Expect frequent updates and improvements
*   Feedback and suggestions are very welcome
*   Balancing and content will continue to evolve

Solo developer project — this mod is developed and maintained by one person (me), alongside a full-time job.

***

## FAQ

<span style="color:#843fa1"><strong>Which Hytale version is supported?</strong></span>

<span style="color:#2dc26b"><em>Hytale <strong>Update 3</strong>.</em></span>

<span style="color:#843fa1"><strong>Does this work on existing worlds?</strong></span>

<span style="color:#2dc26b"><em>Yes. RPGMobs works on existing worlds and affects both current and newly spawned mobs.</em></span>

<span style="color:#843fa1"><strong>Does upgrading from 1.0.0 require any manual steps?</strong></span>

<span style="color:#2dc26b"><em>No. RPGMobs includes a migration system that automatically upgrades saved entities to the latest format.</em></span>

<span style="color:#843fa1"><strong>Can I configure what I don't like?</strong></span>

<span style="color:#2dc26b"><em>Yes. Almost everything is configurable — feel free to change whatever you want.</em></span>

<span style="color:#843fa1"><strong>Can I reload the config?</strong></span>

<span style="color:#2dc26b"><em>Yes — use <code>/rpgmobs reload</code>. Restart if changes don't fully apply.</em></span>

<span style="color:#843fa1"><strong>Is the source code available?</strong></span>

<span style="color:#2dc26b"><em>Yes. The full source is available on <a href="https://github.com/TimShol/hytale-rpgmobs" target="_blank" rel="nofollow">GitHub</a>. The mod is MIT licensed.</em></span>

***

## For Developers

RPGMobs ships a separate `rpgmobs-api` artifact for mod developers. Add it as a compile-time dependency to listen to events, query RPG mob state, or modify loot and damage at runtime.

*   Event-driven API with 12 event types (spawn, death, damage, abilities, aggro, loot)
*   Read-only Query API for inspecting any RPG mob's tier, scaling, combat state, and more
*   Cancellable events for spawn blocking, damage modification, and loot customization
*   Lightweight API artifact — no dependency on the full plugin

See the [API documentation](https://docs.rpgmobs.frotty27.com/api/overview) for setup instructions and integration examples.

### Damage System Overview

RPGMobs is compatible with other damage-modifying mods. Multipliers stack, so balance may need tuning — but as long as you know how to read and edit config files, you're in the clear.

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/description/1444529/description_2c56d0ab-f22e-4ae9-a7d0-b65e6134caab.png" alt="Damage Pipeline"></p></div>