package com.twosquaredstudios.PortalLink;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent.PortalType;

public class PortalLinkPlayerListener extends PlayerListener {
	private final PortalLink plugin;
	private Method portalTypeMethod = null;
	Logger log = Logger.getLogger("Minecraft");
	
	public PortalLinkPlayerListener(PortalLink instance) {
		plugin = instance;
		Method m = null;
		try {
			m = PlayerPortalEvent.class.getMethod("getPortalType");
		} catch (NoSuchMethodException e) {
			
		}
		portalTypeMethod = m;
	}
	
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.getTo() != null) {
			if (event.getTo().getWorld().getEnvironment().getId() == 1) {
				return;
			}
		}
		Player player = event.getPlayer();
		World fromWorld = null;
		World toWorld = null;
		int dimension = 0;
		PortalType portalType = PortalType.UNKNOWN;
		try {
			portalType = (PortalType)portalTypeMethod.invoke(event);
		} catch (Exception e) {
			
		}
		player.sendMessage("portalType: " + portalType);
		if (portalType == PortalType.END_PORTAL) return;
		boolean useDimension = true;
		Map<String,PortalLinkLinkValue> definedLinks = plugin.getPortalLinkConfig().getUserDefinedLinks();
		if (definedLinks.containsKey(player.getWorld().getName())) {
			fromWorld = player.getWorld();
			PortalLinkLinkValue linkValue = definedLinks.get(fromWorld.getName());
			switch (linkValue.getWhichNether()) {
				case 0:
					dimension = -1;
					useDimension = false;
					break;
				case 1:
					dimension = -1;
					break;
				case 2:
					// Everything set to this by default
					break;
				case 3:
					useDimension = false;
					break;
				default:
					break;
			}
			Environment environment = dimension == -1 ? Environment.NORMAL : Environment.NETHER;
			if (!linkValue.getString().equals("")) {
				toWorld = plugin.getServer().createWorld(linkValue.getString(), environment);
			} else {
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "The Nether has been disabled for " + player.getWorld().getName() + ".");
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
		double blockRatio = useDimension ? (dimension == -1 ? 8 : 0.125) : 1;
		
		Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
		Location toLocation = new Location(toWorld, (player.getLocation().getX() * blockRatio), player.getLocation().getY(), (player.getLocation().getZ() * blockRatio), player.getLocation().getYaw(), player.getLocation().getPitch());
		event.setTo(toLocation);
		event.setFrom(fromLocation);
	}
	
	private String replaceLast(String string, String from, String to) {
	     int lastIndex = string.lastIndexOf(from);
	     if (lastIndex < 0) return string;
	     String tail = string.substring(lastIndex).replaceFirst(from, to);
	     return string.substring(0, lastIndex) + tail;
	}
}
