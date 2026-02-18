# Changelog

All notable changes to RPGMobs will be documented in this file.

## [2.0.1] - 2026-02-18

### Removed

- Consumable override system (templates, config, and feature) — RPGMobs no longer overrides vanilla potion and food interactions, preventing conflicts with other mods

### Fixed

- Asset generation (could be) failing on startup due to `${...}` placeholders being split across multiple lines in all template files, causing unresolved placeholders in the generated JSON

## [2.0.0] - 2026-02-18

### Changed

- Rebranded to RPGMobs.

## [1.1.1] - 2026-02-18

### Changed

- Removed the combat overlay text as this can clash with other mods and is an addition that does not really belong to
  RPGMobs.

## [1.1.0] - 2026-02-17

### Added

- Event-driven ability system with three distinct abilities: Charge Leap, Heal Potion, and Undead Summon
- Heal Potion ability allowing elites to drink a healing potion when health drops below a threshold
- Undead Summon ability allowing skeleton and zombie elites to spawn reinforcement minions during combat
- Summoned minions automatically despawn when their summoner dies
- Ability gating system to restrict abilities by mob family, weapon type, and tier
- Ability cooldowns configurable per tier
- Random health variance so no two elites of the same tier have identical health pools
- Distance-based progression with health and damage bonuses that scale with distance from spawn
- Projectile resistance status effect for higher-tier elites with visual particles
- Consumable drops (food items and potions) with tier-based availability
- Config version tracking with automatic config file regeneration on major version changes
- Reconciliation system to sync existing elites with updated config after a live reload
- Component migration system for seamless upgrades from 1.0.0 saves
- Debug mode with granular logging controls for server admins
- Per-zone tier distribution for all Hytale environment zones
- 200+ predefined mob rules with per-mob weapon overrides and ability restrictions
- Developer API module for other mods to integrate with RPGMobs (see API changelog)

### Changed

- Reworked internal architecture from monolithic systems to modular, event-driven features
- Split the single leap ability into separate Charge Leap (offensive) and Heal Potion (defensive) abilities
- Each configurable feature is now independently togglable without affecting other systems
- Health and damage scaling now uses the Hytale stat modifier system instead of direct value overrides
- Improved health verification after spawn to correctly handle both health increases and decreases
- Ability triggers are now event-driven instead of polling every tick
- Spawn system no longer triggers unnecessary reconciliation passes
- All non-essential log output is now gated behind the debug flag for cleaner production logs

### Fixed

- Health scaling verification no longer fails for tiers with multipliers below 1.0
- Elites no longer briefly spin during the heal ability animation
- Elites no longer attempt abilities without a valid target
- Reconciliation no longer cascades when multiple elites spawn in quick succession

## [1.0.0] - 2026-01-25

### Added

- 5-tier elite system transforming standard NPCs into progressively stronger enemies
- Per-tier health and damage scaling with configurable multipliers
- Per-tier model scaling for visually distinct elite presence
- Randomized armor and weapon equipment based on tier
- Shield and utility item equipping with tier-based probability
- Gear durability randomization on spawn
- Tiered loot tables with vanilla drop multipliers from 0x to 6x
- Weapon, armor, and off-hand drop chances on elite death
- 30+ custom loot drops including ores, bars, gems, potions, and Life Essence
- Charge Leap ability for melee elites with slam damage and knockback
- Dual nameplate modes: Simple and Ranked Role with tier indicators
- Environment-based progression using Hytale zone tags
- 10 YAML configuration files for full server customization
- Live config reload via `/rpgmobs reload` command
- Automatic asset generation for tier-specific NPC visuals
- Mob rule system for per-NPC elite transformation control
