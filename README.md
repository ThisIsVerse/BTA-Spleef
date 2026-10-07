# BTA Spleef

Spleef minigame for Better than Adventure 8.0.1 that requires no modifications on the client side at all except installation of the mod on the server. Just use a normal BTA 8.0.1 client to connect.

## Building

Requires Java 17, Gradle 9.3.1. Simply run `gradlew.bat build` and grab the resulting jar from `build/libs/`.

## Arena creation

First build both arena and lobby within the same dimension. Once you save the arena, the mod snapshots the cuboid (block id and data) which enables resetting of the arena after each match. This is limited to 100,000 blocks so do not abuse. `spawnY` is the height of the floor. Any air block on that level is converted to the snow block upon the round starting and existing layers are preserved. Be sure to keep water/lava pits within the saved region.

```text
/spleef create <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ> <lobbyX> <lobbyY> <lobbyZ> <spawnY>
/spleef editspawns <name>
```

For example, setup with the lobby spawn at (101, 144, 157)

```text
/spleef create arena1 66 127 144 91 151 169 101 144 157 147
/spleef addlobbyregion arena1 98 142 153 103 148 161
```

Now, stand on (101, 144, 157) and execute `/spleef setlobby arena1`. 

Afterwards, stand on each of the spawn tiles and execute `/spleef addspawn arena1` (I used (78, 148, 164) and (78, 148, 149) for my test arena). Arenas are stored in `config/bta-spleef-arenas.nbt`.

## Round flow

Using `/spleef join` command, the player joins the arena with the highest amount of already waiting players or picks a random empty arena if nobody has already joined. You are instantly teleported to the spawn point and remain there (cannot wander around) until the actual round starts.

Once there are at least two people waiting, someone with op permissions should start the match using `/spleef start` command, optionally specifying arena name, and initiates a 5 second countdown. During the countdown, nobody can move or break blocks in the arena. The round starts by giving all players a shovel, placing blocks is disabled during the round, and breaking the snow block does not yield snowball drops.

Only ops can break blocks in the arena at any time other than the active round. Falling in water/lava or falling below the bottom of the arena causes immediate elimination and switching the player to the spectator mode in the lobby. Disconnection is also considered as such, leaving the arena during the round results in elimination. Leaving the arena while waiting for a round starts removes the player from the queue. Last remaining player wins the round, the arena is reset afterwards, 15 minutes hardcap for the match.

Other commands: `/spleef list`, `/spleef delete <name>`.

## Known limitations

- lobby has to be within the same dimension as the arena, there's no cross dimension lobby support
- each arena is one saved region with 100k block cap

Apache 2.0, see LICENSE.