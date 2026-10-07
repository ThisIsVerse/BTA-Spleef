# BTA Spleef

A spleef minigame for Better than Adventure 8.0.1 that only needs to be installed on the server. Players just connect with a normal BTA 8.0.1 client. No custom blocks/items/packets, nothing on the client at all.

## Building

Java 17, Gradle 9.3.1. Just run `gradlew.bat build` and grab the jar from `build/libs/`.

## Setting up an arena

Build your arena and lobby in the same dimension first. When you save it, the mod snapshots every block in the cuboid (id and data) so it can reset the arena after each round. That's capped at 100,000 blocks so don't go overboard. `spawnY` just marks one layer that gets auto filled with snow on any air block when a round starts (existing blocks on that layer are left alone), it's a convenience for building a quick flat floor, it's not a restriction on where the arena actually is. Once a round is running, every block inside the saved cuboid is breakable by the players in it, at any height, so arenas can have multiple floors, pits, whatever shape you want. Keep your water/lava pits inside the saved area or they won't reset.

```text
/spleef create <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ> <lobbyX> <lobbyY> <lobbyZ> <spawnY>
/spleef editspawns <name>
```

Example setup, lobby spawn at (101, 144, 157):

```text
/spleef create arena1 66 127 144 91 151 169 101 144 157 147
/spleef addlobbyregion arena1 98 142 153 103 148 161
```

Stand at (101, 144, 157) and run `/spleef setlobby arena1`.

Then for each spawn you want, stand wherever you want the player to start (any height above the floor works, it doesn't have to be right on top of it) and run `/spleef addspawn arena1`. Spawns get saved at whatever height you were standing at, so you can put them up above the arena and let players drop in when the round starts, the lock that holds people in place during the countdown only holds position, not gravity, so the moment the round goes live they fall. Arenas get saved to `config/bta-spleef-arenas.nbt`.

## How a round works

`/spleef join` puts you in whatever arena has the most people already waiting, or picks a random empty one if nobody's waiting anywhere. You get teleported straight to your spawn and you're stuck there (can't wander off) until the round actually starts. `/spleef leave` pulls you back out of the queue if you change your mind.

Once there's at least 2 people in, either the first player who joined or an op can run `/spleef start`, optionally with an arena name. That kicks off a 5 second countdown, nobody can move or break anything in the arena until it ends. Once it starts, any item (even your fist) instamines any block anywhere in the arena for participants, not just one floor layer, so multi layer arenas and pits work fine, no tool needed. Blocks can't be placed while a round is going, broken blocks don't drop anything, fall damage is off, and mobs won't spawn inside the arena.

Only ops can break blocks in the arena any time it isn't running a round, and explosions can't touch arena blocks at all. Fall in water/lava or drop off the bottom and you're out, back to spectator in the lobby, and a lightning bolt strikes right where you lost with a butterfly left behind. The bolt is just the sound and the flash, it doesn't start fires or hurt anyone, so it's purely for show. Disconnecting counts the same way, leave while a round's running and you're eliminated, same as running `/spleef leave` mid round. Leaving while you're just waiting for a round to start simply pulls you out of the queue. Last one standing wins, round resets after a few seconds, 15 minute hard cap so nothing gets stuck forever.

Other commands: `/spleef list`, `/spleef delete <name>`.

## Known limitations

- the lobby has to be in the same dimension as the arena, there's no support for cross dimension lobbies
- each arena is one saved region with a 100k block cap
- 
Apache 2.0, see LICENSE.
