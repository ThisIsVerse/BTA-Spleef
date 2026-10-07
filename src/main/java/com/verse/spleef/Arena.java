package com.verse.spleef;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;

import java.util.ArrayList;
import java.util.List;

public final class Arena {
	public final String name;
	public final int dimension;
	public int minX, minY, minZ, maxX, maxY, maxZ;
	public int gameMinX, gameMinY, gameMinZ, gameMaxX, gameMaxY, gameMaxZ;
	public int lobbyX, lobbyY, lobbyZ;
	public final int spawnY;
	public int lobbyDimension;
	public final List<Spawn> spawns = new ArrayList<>();
	public final List<BlockState> snapshot = new ArrayList<>();

	public Arena(String name, int dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
	             int lobbyX, int lobbyY, int lobbyZ, int spawnY) {
		this.name = name;
		this.dimension = dimension;
		this.minX = minX; this.minY = minY; this.minZ = minZ;
		this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
		this.gameMinX = minX; this.gameMinY = minY; this.gameMinZ = minZ;
		this.gameMaxX = maxX; this.gameMaxY = maxY; this.gameMaxZ = maxZ;
		this.lobbyX = lobbyX; this.lobbyY = lobbyY; this.lobbyZ = lobbyZ;
		this.spawnY = spawnY;
	}

	public static Arena capture(String name, int dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
	                             int lobbyX, int lobbyY, int lobbyZ, int spawnY, World world) {
		long volume = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		if (volume > 100_000L) throw new IllegalArgumentException("Arena snapshot exceeds 100,000 blocks.");
		if (!world.areBlocksLoaded(minX, minY, minZ, maxX, maxY, maxZ)) {
			throw new IllegalArgumentException("Load the whole arena area before saving it.");
		}
		Arena arena = new Arena(name, dimension, minX, minY, minZ, maxX, maxY, maxZ, lobbyX, lobbyY, lobbyZ, spawnY);
		for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
			int id = world.getBlockId(x, y, z);
			int data = world.getBlockData(new TilePos(x, y, z));
			arena.snapshot.add(new BlockState(x, y, z, id, data));
		}
		return arena;
	}

	public boolean contains(int x, int y, int z) {
		return x >= gameMinX && x <= gameMaxX && y >= gameMinY && y <= gameMaxY && z >= gameMinZ && z <= gameMaxZ;
	}

	public boolean containsSaved(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	public void includeRegion(int regionMinX, int regionMinY, int regionMinZ, int regionMaxX, int regionMaxY, int regionMaxZ) {
		this.minX = Math.min(minX, regionMinX); this.minY = Math.min(minY, regionMinY); this.minZ = Math.min(minZ, regionMinZ);
		this.maxX = Math.max(maxX, regionMaxX); this.maxY = Math.max(maxY, regionMaxY); this.maxZ = Math.max(maxZ, regionMaxZ);
	}
	public boolean dimensionEquals(net.minecraft.core.entity.player.Player player) { return player.dimension == dimension; }

	public void restore(World world) {
		for (BlockState state : snapshot) {
			world.setBlockAndMetadataWithNotify(state.x, state.y, state.z, state.id, state.data);
		}
	}

	public void prepareSnow(World world) {
		for (BlockState state : snapshot) {
			if (state.y == spawnY && state.id == net.minecraft.core.block.Blocks.LAYER_SNOW.id()) continue;
			if (state.y == spawnY && state.id == 0) world.setBlockAndMetadataWithNotify(state.x,state.y,state.z,net.minecraft.core.block.Blocks.LAYER_SNOW.id(),0);
		}
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("name", name); tag.putInt("dimension", dimension);
		tag.putInt("minX", minX); tag.putInt("minY", minY); tag.putInt("minZ", minZ);
		tag.putInt("maxX", maxX); tag.putInt("maxY", maxY); tag.putInt("maxZ", maxZ);
		tag.putInt("gameMinX", gameMinX); tag.putInt("gameMinY", gameMinY); tag.putInt("gameMinZ", gameMinZ);
		tag.putInt("gameMaxX", gameMaxX); tag.putInt("gameMaxY", gameMaxY); tag.putInt("gameMaxZ", gameMaxZ);
		tag.putInt("lobbyX", lobbyX); tag.putInt("lobbyY", lobbyY); tag.putInt("lobbyZ", lobbyZ); tag.putInt("spawnY", spawnY);
		tag.putInt("lobbyDimension", lobbyDimension);
		ListTag spawnList = new ListTag();
		for (Spawn spawn : spawns) {
			CompoundTag entry = new CompoundTag(); entry.putInt("x", spawn.x); entry.putInt("y", spawn.y); entry.putInt("z", spawn.z); spawnList.addTag(entry);
		}
		tag.put("spawns", spawnList);
		ListTag blocks = new ListTag();
		for (BlockState block : snapshot) {
			CompoundTag entry = new CompoundTag(); entry.putInt("x", block.x); entry.putInt("y", block.y); entry.putInt("z", block.z); entry.putInt("id", block.id); entry.putInt("data", block.data); blocks.addTag(entry);
		}
		tag.put("blocks", blocks);
		return tag;
	}

	public static Arena load(CompoundTag tag) {
		Arena arena = new Arena(tag.getString("name"), tag.getInteger("dimension"), tag.getInteger("minX"), tag.getInteger("minY"), tag.getInteger("minZ"),
			tag.getInteger("maxX"), tag.getInteger("maxY"), tag.getInteger("maxZ"), tag.getInteger("lobbyX"), tag.getInteger("lobbyY"), tag.getInteger("lobbyZ"), tag.getInteger("spawnY"));
		ListTag spawnList = tag.getList("spawns");
		arena.lobbyDimension = tag.getInteger("lobbyDimension");
		if (tag.containsKey("gameMinX")) {
			arena.gameMinX = tag.getInteger("gameMinX"); arena.gameMinY = tag.getInteger("gameMinY"); arena.gameMinZ = tag.getInteger("gameMinZ");
			arena.gameMaxX = tag.getInteger("gameMaxX"); arena.gameMaxY = tag.getInteger("gameMaxY"); arena.gameMaxZ = tag.getInteger("gameMaxZ");
		}
		for (int i = 0; i < spawnList.tagCount(); i++) { CompoundTag entry = (CompoundTag)spawnList.tagAt(i); arena.spawns.add(new Spawn(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z"))); }
		ListTag blocks = tag.getList("blocks");
		for (int i = 0; i < blocks.tagCount(); i++) { CompoundTag entry = (CompoundTag)blocks.tagAt(i); arena.snapshot.add(new BlockState(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z"), entry.getInteger("id"), entry.getInteger("data"))); }
		return arena;
	}

	public static final class Spawn {
		public final int x, y, z;
		public Spawn(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
		public TilePos tile() { return new TilePos(x, y, z); }
	}

	public static final class BlockState {
		public final int x, y, z, id, data;
		public BlockState(int x, int y, int z, int id, int data) { this.x = x; this.y = y; this.z = z; this.id = id; this.data = data; }
	}
}




