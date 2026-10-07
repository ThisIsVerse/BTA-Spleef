package com.verse.spleef;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import com.mojang.nbt.NbtIo;
import net.minecraft.core.entity.EntityLightning;
import net.minecraft.core.entity.animal.MobButterfly;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.gamemode.Gamemodes;
import net.minecraft.core.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.world.WorldServer;

import java.io.File;
import java.util.*;

public final class SpleefManager {
	private static final Map<String, Arena> ARENAS = new LinkedHashMap<>();
	private static final Map<String, Round> ROUNDS = new HashMap<>();
	private static final Map<UUID, String> EDITING = new HashMap<>();
	private static File saveFile;
	private static final int COUNTDOWN_TICKS = 5 * 20;
	private static final int RESET_DELAY_TICKS = 3 * 20;
	private static final int ROUND_MAX_TICKS = 20 * 60 * 15;
	private static final Set<EntityLightning> COSMETIC_LIGHTNING = Collections.newSetFromMap(new WeakHashMap<>());

	private SpleefManager() {}

	public static void initialize() { load(); }

	public static void serverStart(MinecraftServer server) { load(server.getMinecraftDir()); }

	private static void load() { load(null); }
	private static void load(File root) {
		try {
			File dir = root == null ? new File(".") : root;
			saveFile = new File(new File(dir, "config"), "bta-spleef-arenas.nbt");
			ARENAS.clear();
			if (!saveFile.isFile()) return;
			CompoundTag rootTag;
			try (java.io.InputStream in = new java.io.FileInputStream(saveFile)) { rootTag = NbtIo.readCompressed(in); }
			ListTag list = rootTag.getList("arenas");
			for (int i = 0; i < list.tagCount(); i++) {
				Arena arena = Arena.load((CompoundTag)list.tagAt(i));
				ARENAS.put(arena.name.toLowerCase(Locale.ROOT), arena);
			}
		} catch (Exception e) { BtaSpleef.LOGGER.error("Could not load Spleef arenas", e); }
	}

	private static void save() {
		try {
			if (saveFile == null) load();
			File parent = saveFile.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Could not create config directory");
			CompoundTag root = new CompoundTag(); ListTag list = new ListTag();
			for (Arena arena : ARENAS.values()) list.addTag(arena.save());
			root.put("arenas", list);
			try (java.io.OutputStream out = new java.io.FileOutputStream(saveFile)) { NbtIo.writeCompressed(root, out); }
		} catch (Exception e) { BtaSpleef.LOGGER.error("Could not save Spleef arenas", e); }
	}

	public static void onServerTick(MinecraftServer server) {
		if (server == null || server.playerList == null) return;
		for (Round round : new ArrayList<>(ROUNDS.values())) tickRound(server, round);
	}

	public static void onPlayerTick(Player player) {
		if (player == null || player.world == null || player.world.isClientSide) return;
		String arenaName = findRoundFor(player.uuid);
		if (arenaName == null) return;
		Round round = ROUNDS.get(arenaName);
		if (round == null || round.failed.contains(player.uuid)) return;
		Arena arena = ARENAS.get(arenaName);
		if (arena == null) { player.setGamemode(Gamemodes.SURVIVAL); return; }
		if (round.state == State.WAITING || round.state == State.COUNTDOWN) {
			Arena.Spawn spawn = spawnFor(round, player.uuid, arena);
			if (spawn != null) {
				player.xd = 0; player.yd = 0; player.zd = 0;
				WorldServer arenaWorld = MinecraftServer.getInstance().getDimensionWorld(arena.dimension);
				if (arenaWorld != null && (player.dimension != arena.dimension || Math.abs(player.x - (spawn.x + .5)) > .05 || Math.abs(player.y - spawn.y) > .05 || Math.abs(player.z - (spawn.z + .5)) > .05)) teleportPlayer((PlayerServer)player, arenaWorld, arena.dimension, spawn.x + .5, spawn.y, spawn.z + .5);
			}
			return;
		}
		if (round.state != State.RUNNING) return;
		if (player.isInLava() || player.isInWater() || player.y <= arena.gameMinY - 2) fail(player, arena, round);
	}

	private static Arena.Spawn spawnFor(Round round, UUID playerId, Arena arena) {
		int index = 0;
		for (UUID id : round.participants) { if (id.equals(playerId)) return index < arena.spawns.size() ? arena.spawns.get(index) : null; index++; }
		return null;
	}

	public static String join(Player player) {
		if (!(player instanceof PlayerServer) || player.world == null || player.world.isClientSide) return "Join Spleef from a server player. ";
		ArrayList<Arena> empty = new ArrayList<>();
		ArrayList<Arena> active = new ArrayList<>();
		for (Arena arena : ARENAS.values()) {
			if (arena.spawns.isEmpty()) continue;
			Round round = ROUNDS.get(arena.name.toLowerCase(Locale.ROOT));
			if (round != null && round.state == State.WAITING && round.participants.size() < arena.spawns.size()) active.add(arena);
			else if (round == null) empty.add(arena);
		}
		String alreadyJoined = findRoundFor(player.uuid);
		if (alreadyJoined != null) return "You are already waiting for Spleef arena " + ARENAS.get(alreadyJoined).name + ".";
		Arena target = null;
		if (!active.isEmpty()) {
			active.sort((a,b) -> {
				Round ar = ROUNDS.get(a.name.toLowerCase(Locale.ROOT)), br = ROUNDS.get(b.name.toLowerCase(Locale.ROOT));
				int byPlayers = Integer.compare(br.participants.size(), ar.participants.size());
				return byPlayers != 0 ? byPlayers : a.name.compareToIgnoreCase(b.name);
			});
			target = active.get(0);
		} else if (!empty.isEmpty()) {
			empty.sort(Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
			target = empty.get(new Random().nextInt(empty.size()));
		}
		if (target == null) return "No Spleef arena is currently available.";
		Round round = ROUNDS.get(target.name.toLowerCase(Locale.ROOT));
		if (round == null) {
			round = new Round(target.name.toLowerCase(Locale.ROOT));
				round.state = State.WAITING;
			ROUNDS.put(round.arena, round);
		}
		if (round.state == State.FINISHING || round.state == State.RUNNING || round.state == State.COUNTDOWN) return "That Spleef arena is no longer accepting players.";
		if (round.participants.contains(player.uuid)) return "You are already in that Spleef arena.";
		if (round.participants.size() >= target.spawns.size()) return "That Spleef arena is full.";
		round.participants.add(player.uuid); round.names.put(player.uuid, player.username);
		WorldServer arenaWorld = MinecraftServer.getInstance().getDimensionWorld(target.dimension);
		if (arenaWorld == null) { round.participants.remove(player.uuid); round.names.remove(player.uuid); return "That Spleef arena's world is not loaded."; }
		Arena.Spawn spawn = target.spawns.get(round.participants.size() - 1);
		player.setGamemode(Gamemodes.SURVIVAL);
		teleportPlayer((PlayerServer)player, arenaWorld, target.dimension, spawn.x + .5, spawn.y, spawn.z + .5);
		return "Joined Spleef arena " + target.name + ". " + round.participants.size() + "/" + target.spawns.size() + " players.";
	}

	public static void onArenaBlockHit(Player player, net.minecraft.core.world.pos.TilePosc pos) {
		if (player == null || player.world == null || player.world.isClientSide || !isOp(player)) return;
		String name = editing(player);
		if (name == null) return;
		Arena arena = arena(name);
		if (arena == null || !arena.dimensionEquals(player)) return;
		if (arena.containsSaved(pos.x(), pos.y(), pos.z()) && pos.y() == arena.spawnY) {
			addSpawn(arena, pos.x(), arena.spawnY + 1, pos.z());
			player.sendMessage("Added Spleef spawn " + arena.spawns.size() + ".");
		}
	}

	public static void addSpawnFromCommand(Player player, String arenaName) {
		if (player == null || !isOp(player)) return;
		Arena arena = ARENAS.get(arenaName.toLowerCase(Locale.ROOT));
		if (arena == null) throw new IllegalArgumentException("Unknown arena.");
		if (!arena.dimensionEquals(player)) throw new IllegalArgumentException("Go to the arena dimension first.");
		int x = (int)Math.floor(player.x), y = (int)Math.floor(player.y), z = (int)Math.floor(player.z);
		if (!arena.containsSaved(x, y, z)) throw new IllegalArgumentException("Stand inside the saved arena area before setting a spawn.");
		if (arena.spawns.stream().anyMatch(s -> s.x == x && s.z == z)) throw new IllegalArgumentException("That column already has a spawn point.");
		addSpawn(arena, x, y, z);
	}

	public static boolean onBlockHit(Player player, net.minecraft.core.world.pos.TilePosc pos) {
		if (player == null || player.world == null || player.world.isClientSide || !(player instanceof PlayerServer)) return false;
		String editingArena = editing(player);
		if (editingArena != null) {
			Arena editArena = ARENAS.get(editingArena);
			if (editArena != null && editArena.dimensionEquals(player) && editArena.containsSaved(pos.x(), pos.y(), pos.z())) {
				int spawnX = pos.x(), spawnZ = pos.z();
				if (editArena.spawns.stream().noneMatch(s -> s.x == spawnX && s.z == spawnZ)) {
					addSpawn(editArena, spawnX, editArena.spawnY + 1, spawnZ);
					player.sendMessage("Added Spleef spawn " + editArena.spawns.size() + ".");
				}
			}
			return false;
		}
		String activeName = findRoundFor(player.uuid);
		if (activeName == null) return false;
		return false;
	}

	public static boolean isRoundParticipant(Player player) { return player != null && findRoundFor(player.uuid) != null; }

	public static void onPlayerDisconnect(Player player) {
		if (player == null) return;
		String arenaName = findRoundFor(player.uuid);
		if (arenaName == null) return;
		Round round = ROUNDS.get(arenaName);
		if (round == null) return;
		if (round.state == State.RUNNING) {
			if (round.failed.add(player.uuid)) {
				MinecraftServer server = MinecraftServer.getInstance();
				strikeLossEffect(player);
				announce(server, round, player.username + " disconnected and is out.");
				checkWinner(server, round);
			}
		} else if (round.state == State.WAITING) {
			round.participants.remove(player.uuid);
			round.names.remove(player.uuid);
		}
	}

	public static boolean isActiveRoundParticipant(Player player) {
		if (player == null) return false;
		String arenaName = findRoundFor(player.uuid);
		if (arenaName == null) return false;
		Round round = ROUNDS.get(arenaName);
		return round != null && round.state == State.RUNNING && !round.failed.contains(player.uuid);
	}

	public static boolean isInstamineBlock(Player player, int x, int y, int z) {
		if (!isActiveRoundParticipant(player)) return false;
		String arenaName = findRoundFor(player.uuid);
		Arena arena = ARENAS.get(arenaName);
		return arena != null && arena.containsSaved(x, y, z);
	}

	public static boolean isInsideAnyArena(World world, int x, int y, int z) {
		if (world == null) return false;
		for (Arena arena : ARENAS.values()) {
			if (arena.dimension == world.dimension.id && arena.containsSaved(x, y, z)) return true;
		}
		return false;
	}

	public static boolean preventArenaBlockBreak(Player player, int x, int y, int z) {
		if (player == null || player.world == null || player.world.isClientSide) return false;
		for (Arena arena : ARENAS.values()) {
			if (!arena.dimensionEquals(player) || !arena.containsSaved(x,y,z)) continue;
			Round round = ROUNDS.get(arena.name.toLowerCase(Locale.ROOT));
			if (round != null && (round.state == State.WAITING || round.state == State.COUNTDOWN)) return true;
			if (round != null && round.state == State.RUNNING) return !round.participants.contains(player.uuid);
			return !isOp(player);
		}
		return false;
	}

	public static boolean sameActiveRound(Player a, Player b) {
		if (a == null || b == null) return false;
		String arenaA = findRoundFor(a.uuid), arenaB = findRoundFor(b.uuid);
		if (arenaA == null || !arenaA.equals(arenaB)) return false;
		Round round = ROUNDS.get(arenaA);
		return round != null && (round.state == State.COUNTDOWN || round.state == State.RUNNING || round.state == State.FINISHING);
	}


	private static boolean isOp(Player player) {
		MinecraftServer server = MinecraftServer.getInstance();
		return server != null && server.playerList != null && server.playerList.isOp(player.uuid);
	}

	private static String findRoundFor(UUID uuid) {
		for (Map.Entry<String, Round> entry : ROUNDS.entrySet()) if (entry.getValue().participants.contains(uuid)) return entry.getKey();
		return null;
	}

	private static void tickRound(MinecraftServer server, Round round) {
		if (round.state == State.COUNTDOWN) {
			if (--round.ticks <= 0) begin(server, round);
			else if (round.ticks % 20 == 0) announce(server, round, "Spleef starts in " + (round.ticks / 20) + "...");
		} else if (round.state == State.RUNNING) {
			if (++round.ticks > ROUND_MAX_TICKS) finish(server, round, null, "Round ended: time limit reached.");
			else checkWinner(server, round);
		} else if (round.state == State.FINISHING && --round.ticks <= 0) reset(server, round);
	}

	private static void begin(MinecraftServer server, Round round) {
		Arena arena = ARENAS.get(round.arena);
		WorldServer world = server.getDimensionWorld(arena.dimension);
		if (world == null) { finish(server, round, null, "Spleef cancelled: arena dimension unavailable."); return; }
		arena.restore(world);
		arena.prepareSnow(world);
		for (UUID id : round.participants) {
			PlayerServer player = server.playerList.getPlayerEntity(round.names.get(id));
			if (player == null) { round.failed.add(id); continue; }
			player.setGamemode(Gamemodes.SURVIVAL);
			Arena.Spawn spawn = spawnFor(round, id, arena);
			if (spawn != null) teleportPlayer(player, world, arena.dimension, spawn.x + .5, spawn.y, spawn.z + .5);
		}
		round.state = State.RUNNING; round.ticks = 0;
		announce(server, round, "Spleef! Break snow blocks; touching water or lava eliminates you.");
	}

	private static void fail(Player player, Arena arena, Round round) {
		if (!round.failed.add(player.uuid)) return;
		strikeLossEffect(player);
		player.setGamemode(Gamemodes.SPECTATOR);
		WorldServer lobbyWorld = MinecraftServer.getInstance().getDimensionWorld(arena.lobbyDimension);
		if (lobbyWorld != null) teleportPlayer((PlayerServer)player, lobbyWorld, arena.lobbyDimension, arena.lobbyX + .5, arena.lobbyY, arena.lobbyZ + .5);
		player.sendMessage("You are out! You are now spectating.");
	}

	private static void strikeLossEffect(Player player) {
		if (player == null || !(player.world instanceof WorldServer)) return;
		strikeCosmeticLightning((WorldServer) player.world, player.x, player.y, player.z);
	}

	private static void strikeCosmeticLightning(WorldServer world, double x, double y, double z) {
		EntityLightning bolt = new EntityLightning(world, x, y, z);
		COSMETIC_LIGHTNING.add(bolt);
		world.entityJoinedWorld(bolt);
		MobButterfly butterfly = new MobButterfly(world);
		butterfly.moveTo(x, y, z, 0f, 0f);
		butterfly.spawnInit();
		world.entityJoinedWorld(butterfly);
	}

	public static boolean isCosmeticLightning(EntityLightning bolt) { return COSMETIC_LIGHTNING.contains(bolt); }

	private static void teleportPlayer(PlayerServer player, WorldServer world, int dimension, double x, double y, double z) {
		if (player.dimension != dimension || player.world != world) {
			player.setWorld(world);
			player.dimension = dimension;
		}
		player.playerNetServerHandler.teleportAndRotate(x, y, z, player.yRot, player.xRot);
	}

	private static void checkWinner(MinecraftServer server, Round round) {
		List<PlayerServer> alive = new ArrayList<>();
		for (UUID id : round.participants) {
			if (round.failed.contains(id)) continue;
			PlayerServer player = server.playerList.getPlayerEntity(round.names.get(id));
			if (player != null) alive.add(player);
			else round.failed.add(id);
		}
		if (alive.size() <= 1) finish(server, round, alive.isEmpty() ? null : alive.get(0), alive.isEmpty() ? "Spleef ended: no winner." : alive.get(0).username + " wins Spleef!");
	}

	private static void finish(MinecraftServer server, Round round, PlayerServer winner, String message) {
		if (round.state == State.FINISHING) return;
		round.state = State.FINISHING; round.ticks = RESET_DELAY_TICKS;
		announce(server, round, message);
		Arena arena = ARENAS.get(round.arena);
		for (UUID id : round.participants) {
			PlayerServer player = server.playerList.getPlayerEntity(round.names.get(id));
			if (player == null) continue;
			player.setGamemode(Gamemodes.SURVIVAL);
			WorldServer lobbyWorld = server.getDimensionWorld(arena.lobbyDimension);
			if (lobbyWorld != null) teleportPlayer(player, lobbyWorld, arena.lobbyDimension, arena.lobbyX + .5, arena.lobbyY, arena.lobbyZ + .5);
		}
	}


	private static void reset(MinecraftServer server, Round round) {
		Arena arena = ARENAS.get(round.arena);
		WorldServer world = server.getDimensionWorld(arena.dimension);
		if (world != null) arena.restore(world);
		ROUNDS.remove(round.arena);
		announce(server, round, "Arena reset. Players have returned to the lobby.");
	}

	private static void announce(MinecraftServer server, Round round, String message) {
		for (UUID id : round.participants) {
			PlayerServer player = server.playerList.getPlayerEntity(round.names.get(id));
			if (player != null) player.sendMessage(message);
		}
	}

	public static String leave(Player player) {
		if (player == null) return "Leave Spleef from a server player.";
		String arenaName = findRoundFor(player.uuid);
		if (arenaName == null) return "You are not waiting for or playing in a Spleef round.";
		Round round = ROUNDS.get(arenaName);
		if (round == null) return "You are not waiting for or playing in a Spleef round.";
		Arena arena = ARENAS.get(arenaName);
		if (round.state == State.WAITING) {
			round.participants.remove(player.uuid);
			round.names.remove(player.uuid);
			return "You left the Spleef queue.";
		}
		if (round.state == State.COUNTDOWN || round.state == State.RUNNING) {
			if (round.failed.add(player.uuid)) {
				MinecraftServer server = MinecraftServer.getInstance();
				strikeLossEffect(player);
				player.setGamemode(Gamemodes.SURVIVAL);
				if (arena != null) {
					WorldServer lobbyWorld = server.getDimensionWorld(arena.lobbyDimension);
					if (lobbyWorld != null) teleportPlayer((PlayerServer)player, lobbyWorld, arena.lobbyDimension, arena.lobbyX + .5, arena.lobbyY, arena.lobbyZ + .5);
				}
				announce(server, round, player.username + " left the round.");
				if (round.state == State.RUNNING) checkWinner(server, round);
			}
			return "You left the Spleef round.";
		}
		return "You are not waiting for or playing in a Spleef round.";
	}

	public static boolean start(Player starter, String name) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (starter == null) return false;
		boolean op = isOp(starter);
		if (name == null) {
			String joined = findRoundFor(starter.uuid);
			if (joined != null) name = joined;
			else if (op) {
				name = ROUNDS.values().stream().filter(r -> r.state == State.WAITING)
					.sorted((a,b) -> { int count = Integer.compare(b.participants.size(), a.participants.size()); return count != 0 ? count : a.arena.compareTo(b.arena); })
					.map(r -> r.arena).findFirst().orElse(null);
			}
		}
		if (name == null) { starter.sendMessage("No Spleef arena has waiting players."); return false; }
		Arena arena = ARENAS.get(name.toLowerCase(Locale.ROOT));
		if (arena == null) { starter.sendMessage("Unknown Spleef arena."); return false; }
		Round round = ROUNDS.get(name.toLowerCase(Locale.ROOT));
		if (round == null || round.state != State.WAITING) { starter.sendMessage("That arena has no waiting players."); return false; }
		boolean firstInQueue = !round.participants.isEmpty() && round.participants.iterator().next().equals(starter.uuid);
		if (!op && !firstInQueue) { starter.sendMessage("Only the first player in the queue or a server operator can start this round."); return false; }
		if (arena.spawns.isEmpty()) { starter.sendMessage("This arena has no player spawns. Ask an admin to add them."); return false; }
		WorldServer world = server.getDimensionWorld(arena.dimension);
		if (world == null) { starter.sendMessage("Arena dimension is not loaded."); return false; }
		List<PlayerServer> players = new ArrayList<>();
		for (UUID id : round.participants) { PlayerServer player = server.playerList.getPlayerEntity(round.names.get(id)); if (player != null) players.add(player); }
		if (players.size() < 2) { starter.sendMessage("At least two players must be inside the arena to start."); return false; }
		if (players.size() > arena.spawns.size()) { starter.sendMessage("This arena needs at least " + players.size() + " spawn points."); return false; }
		arena.restore(world);
		arena.prepareSnow(world);
		for (PlayerServer player : players) {
			player.setGamemode(Gamemodes.SURVIVAL);
		}
		round.state = State.COUNTDOWN; round.ticks = COUNTDOWN_TICKS;
		ROUNDS.put(round.arena, round);
		announce(server, round, "Spleef starts in 5...");
		return true;
	}

	public static void markEditing(Player player, String name) { EDITING.put(player.uuid, name.toLowerCase(Locale.ROOT)); }
	public static String editing(Player player) { return EDITING.get(player.uuid); }
	public static void clearEditing(Player player) { EDITING.remove(player.uuid); }
	public static void createArena(Player player, String name, int ax, int ay, int az, int bx, int by, int bz, int lobbyX, int lobbyY, int lobbyZ, int spawnY) {
		String key = name.toLowerCase(Locale.ROOT);
		Arena arena = Arena.capture(name, player.dimension, Math.min(ax,bx), Math.min(ay,by), Math.min(az,bz), Math.max(ax,bx), Math.max(ay,by), Math.max(az,bz), lobbyX,lobbyY,lobbyZ,spawnY,player.world);
		arena.lobbyDimension = player.dimension;
		ARENAS.put(key, arena); save();
	}
	public static void setLobby(String name,int x,int y,int z){Arena arena=ARENAS.get(name.toLowerCase(Locale.ROOT));if(arena==null)throw new IllegalArgumentException("Unknown arena.");arena.lobbyX=x;arena.lobbyY=y;arena.lobbyZ=z;save();}
	public static void includeSavedRegion(String name, int ax, int ay, int az, int bx, int by, int bz, World world) {
		Arena arena = ARENAS.get(name.toLowerCase(Locale.ROOT));
		if (arena == null) throw new IllegalArgumentException("Unknown arena.");
		if (arena.dimension != world.dimension.id) throw new IllegalArgumentException("Lobby and arena must be in the same dimension.");
		int minX = Math.min(ax,bx), minY = Math.min(ay,by), minZ = Math.min(az,bz);
		int maxX = Math.max(ax,bx), maxY = Math.max(ay,by), maxZ = Math.max(az,bz);
		if (!world.areBlocksLoaded(minX,minY,minZ,maxX,maxY,maxZ)) throw new IllegalArgumentException("Load the entire lobby region before adding it.");
		long volume = (long)(Math.max(arena.maxX,maxX)-Math.min(arena.minX,minX)+1)
			* (Math.max(arena.maxY,maxY)-Math.min(arena.minY,minY)+1)
			* (Math.max(arena.maxZ,maxZ)-Math.min(arena.minZ,minZ)+1);
		if (volume > 100_000L) throw new IllegalArgumentException("Combined arena and lobby snapshot exceeds 100,000 blocks.");
		ArrayList<Arena.BlockState> newBlocks = new ArrayList<>();
		for (int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++) {
			if (!arena.containsSaved(x,y,z)) newBlocks.add(new Arena.BlockState(x,y,z,world.getBlockId(x,y,z),world.getBlockData(new net.minecraft.core.world.pos.TilePos(x,y,z))));
		}
		arena.snapshot.addAll(newBlocks);
		arena.includeRegion(minX,minY,minZ,maxX,maxY,maxZ);
		save();
	}
	public static Arena arena(String name) { return ARENAS.get(name.toLowerCase(Locale.ROOT)); }
	public static boolean inUse(String name) { return ROUNDS.containsKey(name.toLowerCase(Locale.ROOT)); }
	public static Collection<Arena> arenas() { return Collections.unmodifiableCollection(ARENAS.values()); }
	public static void addSpawn(Arena arena, int x, int y, int z) { arena.spawns.add(new Arena.Spawn(x,y,z)); save(); }
	public static boolean isOpPublic(Player player) { return isOp(player); }
	public static boolean removeArena(String name) { if (ROUNDS.containsKey(name.toLowerCase(Locale.ROOT))) return false; ARENAS.remove(name.toLowerCase(Locale.ROOT)); EDITING.values().removeIf(value -> value.equals(name.toLowerCase(Locale.ROOT))); save(); return true; }

	private enum State { WAITING, COUNTDOWN, RUNNING, FINISHING }
	private static final class Round {
		final String arena; final Set<UUID> participants = new LinkedHashSet<>(); final Map<UUID,String> names = new HashMap<>(); final Set<UUID> failed = new HashSet<>(); State state; int ticks;
		Round(String arena) { this.arena = arena; }
	}
}








