# Mob Bossbar

Server-side boss bar implementation for Minecraft Java 1.21.11.

This project builds two variants:

- Fabric mod: put in `mods/`
- Paper plugin: put in `plugins/`

Both variants watch living non-player entities with the scoreboard/command tag `boss` and show a vanilla boss bar with the entity name and current health.

## Usage

Add the tag to any mob:

```mcfunction
/tag @e[type=minecraft:zombie,limit=1,sort=nearest] add boss
```

Remove the tag to hide the boss bar:

```mcfunction
/tag @e[type=minecraft:zombie,limit=1,sort=nearest] remove boss
```

Players see boss bars for tagged mobs in their current world/dimension.

## Build

Requires JDK 21.

```sh
gradle build
```

Built jars:

- `fabric/build/libs/mob-bossbar-fabric-1.0.0.jar`
- `paper/build/libs/mob-bossbar-paper-1.0.0.jar`

## Release

Create and push a version tag to publish a GitHub Release with both binaries attached:

```sh
git tag v1.0.0
git push origin v1.0.0
```
