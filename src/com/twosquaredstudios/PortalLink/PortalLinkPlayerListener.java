package com.twosquaredstudios.PortalLink;

import java.util.Map;
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
		if (event.isCancelled()) {
			return;
		}
		World fromWorld = null;
		World toWorld = null;
		int dimension = 0;
		boolean useDimension = true;
		Player player = event.getPlayer();
		Map<String,PortalLinkLinkValue> definedLinks = plugin.getPortalLinkConfig().getUserDefinedLinks();
		if (definedLinks.containsKey(player.getWorld().getName())) {
			fromWorld = player.getWorld();
			PortalLinkLinkValue linkValue = definedLinks.get(fromWorld.getName());
			switch (linkValue.getWhichNether()) {
				case 0:
					useDimension = false;
					break;
				case 1:
					// Everything set to this by default
					break;
				case 2:
					dimension = -1;
					break;
				case 3:
					dimension = -1;
					useDimension = false;
					break;
				default:
					break;
			}
			Environment environment = dimension == -1 ? Environment.NETHER : Environment.NORMAL;
			if (!linkValue.getString().equals("")) {
				toWorld = plugin.getServer().createWorld(linkValue.getString(), environment);
			} else {
				event.setCancelled(true);
				player.sendMessage("The Nether has been disabled for " + player.getWorld().getName() + ".");
				return;
			}
		} else {
			Environment environment = player.getWorld().getEnvironment();
			if (environment.equals(Environment.NETHER)) {
				dimension = -1;
			}
			else {
				dimension = 0;
			}
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
			if (!plugin.getAllowNether(fromWorld)) {
				return;
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
			if (toWorld.getEnvironment().equals(fromWorld.getEnvironment())) useDimension = false;
		}
		double blockRatio = useDimension ? (dimension == -1 ? 0.125 : 8) : 1; // Flipped (compared to CraftBukkit) because I use dimension the other way round
		
		Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
		Location toLocation = new Location(toWorld, (player.getLocation().getX() * blockRatio), player.getLocation().getY(), (player.getLocation().getZ() * blockRatio), player.getLocation().getYaw(), player.getLocation().getPitch());
		event.setTo(toLocation);
		event.setFrom(fromLocation);
	}
}
