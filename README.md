# BTA Spleef

A spleef minigame for Better than Adventure 8.0.1 that only needs to be installed on the server. Players just connect with a normal BTA 8.0.1 client. No custom blocks/items/packets, nothing on the client at all.

## Building

Java 17, Gradle 9.3.1. Just run `gradlew.bat build` and grab the jar from `build/libs/`.

## Setting up an arena

Build your arena and lobby in the same dimension first. When you save it, the mod snapshots every block in the cuboid (id and data) so it can reset the arena after each round. That's capped at 100,000 blocks so don't go overboard. `spawnY` is the height of the floor. Any air on that layer gets turned into a snow layer when a round starts, and layers that are already there get left alone. Keep your water/lava pits inside the saved area or they won't reset.

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

Then stand on each player spawn tile and run `/spleef addspawn arena1` for every spawn you want (I used (78, 148, 164) and (78, 148, 149) for my test arena). Arenas get saved to `config/bta-spleef-arenas.nbt`.

## How a round works

`/spleef join` puts you in whatever arena has the most people already waiting, or picks a random empty one if nobody's waiting anywhere. You get teleported straight to your spawn and you're stuck there (can't wander off) until the round actually starts.

Someone with op has to run `/spleef start`, optionally with an arena name, once there's at least 2 people in. That kicks off a 5 second countdown, nobody can move or break anything in the arena until it ends. Once it starts everyone can instamine the blocks. Blocks can't be placed while a round is going, and the snow doesn't drop snowballs when you break it.

Only ops can break blocks in the arena any time it isn't running a round. Fall in water/lava or drop off the bottom and you're out, back to spectator in the lobby. Disconnecting counts the same way, leave while a round's running and you're eliminated. Leaving while you're just waiting for a round to start simply pulls you out of the queue. Last one standing wins, round resets after a few seconds, 15 minute hard cap so nothing gets stuck forever.

Other commands: `/spleef list`, `/spleef delete <name>`.

## Known limitations

- the lobby has to be in the same dimension as the arena, there's no support for cross dimension lobbies
- each arena is one saved region with a 100k block cap

Apache 2.0, see LICENSE.
