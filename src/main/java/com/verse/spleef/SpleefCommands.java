package com.verse.spleef;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentTypeInteger;
import com.mojang.brigadier.arguments.ArgumentTypeString;
import com.mojang.brigadier.builder.ArgumentBuilderLiteral;
import com.mojang.brigadier.builder.ArgumentBuilderRequired;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.CommandManager;
import net.minecraft.core.net.command.CommandSource;

import static com.mojang.brigadier.arguments.ArgumentTypeInteger.getInteger;
import static com.mojang.brigadier.arguments.ArgumentTypeString.getString;

public final class SpleefCommands implements CommandManager.CommandRegistry {
	@Override
	public void register(CommandDispatcher<CommandSource> dispatcher) {
		ArgumentBuilderLiteral<CommandSource> root = ArgumentBuilderLiteral.<CommandSource>literal("spleef")
			.executes(ctx -> { if (!requireOp(ctx.getSource())) return 0; help(ctx.getSource()); return 1; });
		root.then(ArgumentBuilderLiteral.<CommandSource>literal("join").executes(ctx -> join(ctx.getSource())));
		root.then(admin("start").executes(ctx -> start(ctx.getSource(), null)).then(arenaArg("arena", SpleefCommands::start)));

		ArgumentBuilderRequired<CommandSource, String> create = ArgumentBuilderRequired.<CommandSource, String>argument("name", ArgumentTypeString.word());
		ArgumentBuilderRequired<CommandSource, Integer> minX = integer("minX");
		ArgumentBuilderRequired<CommandSource, Integer> minY = integer("minY");
		ArgumentBuilderRequired<CommandSource, Integer> minZ = integer("minZ");
		ArgumentBuilderRequired<CommandSource, Integer> maxX = integer("maxX");
		ArgumentBuilderRequired<CommandSource, Integer> maxY = integer("maxY");
		ArgumentBuilderRequired<CommandSource, Integer> maxZ = integer("maxZ");
		ArgumentBuilderRequired<CommandSource, Integer> lobbyX = integer("lobbyX");
		ArgumentBuilderRequired<CommandSource, Integer> lobbyY = integer("lobbyY");
		ArgumentBuilderRequired<CommandSource, Integer> lobbyZ = integer("lobbyZ");
		ArgumentBuilderRequired<CommandSource, Integer> spawnY = integer("spawnY");
		spawnY.executes(ctx -> create(ctx.getSource(), getString(ctx,"name"), getInteger(ctx,"minX"), getInteger(ctx,"minY"), getInteger(ctx,"minZ"), getInteger(ctx,"maxX"), getInteger(ctx,"maxY"), getInteger(ctx,"maxZ"), getInteger(ctx,"lobbyX"), getInteger(ctx,"lobbyY"), getInteger(ctx,"lobbyZ"), getInteger(ctx,"spawnY")));
		lobbyZ.then(spawnY); lobbyY.then(lobbyZ); lobbyX.then(lobbyY); maxZ.then(lobbyX); maxY.then(maxZ); maxX.then(maxY); minZ.then(maxX); minY.then(minZ); minX.then(minY); create.then(minX);
		root.then(admin("create").then(create));
		root.then(admin("setlobby").then(arenaArg("arena", (src,name) -> setLobby(src,name))));
		root.then(admin("addspawn").then(arenaArg("arena", SpleefCommands::addSpawn)));
		root.then(admin("editspawns").then(arenaArg("arena", SpleefCommands::editSpawns)));
		ArgumentBuilderRequired<CommandSource, String> lobbyName = ArgumentBuilderRequired.<CommandSource, String>argument("arena", ArgumentTypeString.word());
		ArgumentBuilderRequired<CommandSource, Integer> lobbyMinX = integer("minX"), lobbyMinY = integer("minY"), lobbyMinZ = integer("minZ");
		ArgumentBuilderRequired<CommandSource, Integer> lobbyMaxX = integer("maxX"), lobbyMaxY = integer("maxY"), lobbyMaxZ = integer("maxZ");
		lobbyMaxZ.executes(ctx -> addLobbyRegion(ctx.getSource(), getString(ctx,"arena"), getInteger(ctx,"minX"),getInteger(ctx,"minY"),getInteger(ctx,"minZ"),getInteger(ctx,"maxX"),getInteger(ctx,"maxY"),getInteger(ctx,"maxZ")));
		lobbyMaxY.then(lobbyMaxZ); lobbyMaxX.then(lobbyMaxY); lobbyMinZ.then(lobbyMaxX); lobbyMinY.then(lobbyMinZ); lobbyMinX.then(lobbyMinY); lobbyName.then(lobbyMinX);
		root.then(admin("addlobbyregion").then(lobbyName));
		root.then(admin("spawn").then(arenaArg("arena", SpleefCommands::spawn)));
		root.then(admin("stopediting").executes(ctx -> stopEditing(ctx.getSource())));
		root.then(admin("list").executes(ctx -> list(ctx.getSource())));
		root.then(admin("delete").then(arenaArg("arena", SpleefCommands::delete)));
		dispatcher.register(root);
	}

	private static ArgumentBuilderRequired<CommandSource, Integer> integer(String name) {
		return ArgumentBuilderRequired.<CommandSource, Integer>argument(name, ArgumentTypeInteger.integer());
	}
	private static ArgumentBuilderLiteral<CommandSource> admin(String name) {
		return ArgumentBuilderLiteral.<CommandSource>literal(name).requires(source -> {
			Player player = source.getSender();
			return player != null && SpleefManager.isOpPublic(player);
		});
	}

	private static ArgumentBuilderRequired<CommandSource, String> arenaArg(String name, java.util.function.BiFunction<CommandSource, String, Integer> action) {
		return ArgumentBuilderRequired.<CommandSource, String>argument(name, ArgumentTypeString.word())
			.executes(ctx -> { if (!requireOp(ctx.getSource())) return 0; return action.apply(ctx.getSource(), getString(ctx, name)); });
	}

	private static boolean requireOp(CommandSource source) { Player p=source.getSender(); if(p==null){source.sendMessage("Run this command in game as an OP.");return false;} if(!SpleefManager.isOpPublic(p)){p.sendMessage("Only server operators can view or use Spleef admin commands.");return false;} return true; }
	private static int join(CommandSource source) { Player p=source.getSender(); if(p==null){source.sendMessage("Join Spleef in game.");return 0;} String message=SpleefManager.join(p);p.sendMessage(message);return message.startsWith("Joined Spleef")?1:0; }

	private static int create(CommandSource source, String name, int minX,int minY,int minZ,int maxX,int maxY,int maxZ,int lobbyX,int lobbyY,int lobbyZ,int spawnY) {
		Player player = requireOpPlayer(source); if (player == null) return 0;
		try {
			SpleefManager.createArena(player,name,minX,minY,minZ,maxX,maxY,maxZ,lobbyX,lobbyY,lobbyZ,spawnY);
			player.sendMessage("Saved arena '"+name+"'. Add spawn points with /spleef editspawns "+name+", then hit each arena floor block.");
			return 1;
		} catch (Exception e) { player.sendMessage("Could not save arena: " + e.getMessage()); return 0; }
	}
	private static int setLobby(CommandSource source,String name){Player player=requireOpPlayer(source);if(player==null)return 0;try{SpleefManager.setLobby(name,(int)Math.floor(player.x),(int)Math.floor(player.y),(int)Math.floor(player.z));player.sendMessage("Saved current position as lobby spawn for '"+name+"'.");return 1;}catch(Exception e){player.sendMessage("Could not set lobby: "+e.getMessage());return 0;}}
	private static int addSpawn(CommandSource source,String name){Player player=requireOpPlayer(source);if(player==null)return 0;try{SpleefManager.addSpawnFromCommand(player,name);player.sendMessage("Saved player spawn " + SpleefManager.arena(name).spawns.size() + " for '"+name+"'.");return 1;}catch(Exception e){player.sendMessage("Could not add spawn: "+e.getMessage());return 0;}}
	private static int spawn(CommandSource source, String name) {
		Player player=requireOpPlayer(source); if(player==null)return 0;
		Arena arena=SpleefManager.arena(name); if(arena==null){player.sendMessage("Unknown arena.");return 0;}
		SpleefManager.markEditing(player,name); player.sendMessage("Hit each arena floor block for '"+name+"'. Use /spleef stopediting when done.");return 1;
	}
	private static int editSpawns(CommandSource source,String name){return spawn(source,name);}
	private static int addLobbyRegion(CommandSource source,String name,int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
		Player player=requireOpPlayer(source); if(player==null)return 0;
		try { SpleefManager.includeSavedRegion(name,minX,minY,minZ,maxX,maxY,maxZ,player.world); player.sendMessage("Included lobby cuboid in arena snapshot for '"+name+"'."); return 1; }
		catch(Exception e){player.sendMessage("Could not add lobby region: "+e.getMessage());return 0;}
	}
	private static int stopEditing(CommandSource source){Player player=requireOpPlayer(source);if(player==null)return 0;SpleefManager.clearEditing(player);player.sendMessage("Stopped editing Spleef spawns.");return 1;}
	private static int start(CommandSource source,String arena){Player player=requireOpPlayer(source);if(player==null)return 0;boolean started=SpleefManager.start(player,arena);if(started)player.sendMessage("Spleef countdown started.");return started?1:0;}
	private static int list(CommandSource source){Player player=requireOpPlayer(source);if(player==null)return 0;source.sendMessage("Saved Spleef arenas:");for(Arena arena:SpleefManager.arenas())source.sendMessage("- "+arena.name+" ("+arena.spawns.size()+" spawns)");return 1;}
	private static int delete(CommandSource source,String name){Player player=requireOpPlayer(source);if(player==null)return 0;if(SpleefManager.arena(name)==null){player.sendMessage("Unknown arena.");return 0;}if(!SpleefManager.removeArena(name)){player.sendMessage("Stop the active round before deleting this arena.");return 0;}player.sendMessage("Deleted arena '"+name+"'.");return 1;}
	private static Player requireOpPlayer(CommandSource source){Player player=source.getSender();if(player==null){source.sendMessage("Run this command in game as an OP.");return null;}if(!SpleefManager.isOpPublic(player)){player.sendMessage("Only server operators can use Spleef setup commands.");return null;}return player;}
	private static void help(CommandSource source){source.sendMessage("Players: /spleef join.");source.sendMessage("OPs: /spleef start [arena], /spleef create <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ> <lobbyX> <lobbyY> <lobbyZ> <spawnY>.");source.sendMessage("Setup: /spleef addlobbyregion <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>, /spleef addspawn <name>, /spleef setlobby <name>.");source.sendMessage("Other OP commands: /spleef editspawns, stopediting, list, delete.");}
}
