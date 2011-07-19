package com.twosquaredstudios.PortalLink;

import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalLinkPlayerListener extends PlayerListener {
	private final PortalLink plugin;
	Logger log = Logger.getLogger("Minecraft");
	
	public PortalLinkPlayerListener(PortalLink instance) {
		plugin = instance;
	}
	
	public void onPlayerPortal(PlayerPortalEvent event) {
		int dimension;
		Player player = event.getPlayer();
		Environment environment = player.getWorld().getEnvironment();
		if (environment.equals(Environment.NETHER)) {
			dimension = -1;
		}
		else {
			dimension = 0;
		}
		World fromWorld = null;
		World toWorld = null;
		for (World world1 : plugin.getServer().getWorlds()) {
			if (world1.getEnvironment().equals(environment)) {
				if (world1.getName().equals(player.getWorld().getName())) {
					fromWorld = world1;
				}
			}
			else {
				String worldWorld;
				if (dimension == -1) {
					worldWorld = world1.getName() + "_nether";
				}
				else {
					worldWorld = world1.getName().replaceAll("_nether", "");
				}
				if (worldWorld.equals(player.getWorld().getName())) {
					toWorld = world1;
				}
			}
		}
		if (fromWorld == null) {
			log.warning("Unable To Match A World To The Player's World!");
			return;
		}
		if (toWorld == null) {
			if (dimension == -1) {
				toWorld = plugin.getServer().createWorld(fromWorld.getName().replaceAll("_nether", ""), Environment.NORMAL);
			}
			else {
				toWorld = plugin.getServer().createWorld(fromWorld.getName() + "_nether", Environment.NETHER);
			}
		}
		double blockRatio = dimension == -1 ? 8 : 0.125;
		
		Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
		Location toLocation = new Location(toWorld, (player.getLocation().getX() * blockRatio), player.getLocation().getY(), (player.getLocation().getZ() * blockRatio), player.getLocation().getYaw(), player.getLocation().getPitch());
		event.setTo(toLocation);
		event.setFrom(fromLocation);
	}
}
