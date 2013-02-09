/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jrtc27.portallink;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class PortalLinkListener implements Listener, CommandExecutor {
	private final PortalLink plugin;

	public PortalLinkListener(final PortalLink plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) return false;
		if (args[0].equalsIgnoreCase("link") && args.length > 1) {
			if (!sender.hasPermission("portallink.link")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to define PortalLinks.");
				return true;
			}
			boolean twoWay = false;
			int whichNether = 0;
			int index = 1;
			if (args[1].startsWith("-") && args[1].length() <= 3) {
				if (args[1].contains("b")) {
					twoWay = true;
					index = 2;
				}
				if (args[1].contains("0")) {
					index = 2;
				} else if (args[1].contains("1")) {
					index = 2;
					whichNether = 1;
				} else if (args[1].contains("2")) {
					index = 2;
					whichNether = 2;
				} else if (args[1].contains("3")) {
					index = 2;
					whichNether = 3;
				}
			} /*else if (args[1].startsWith("-N")) {
						String numberStr = String.valueOf(args[1].charAt(2));
						if ("0123".contains(numberStr)) {
							whichNether = Integer.parseInt(numberStr);
						}
						index = 2;
					}*/
			if (args.length > 2) {
				if (args[2].equalsIgnoreCase("-b")) {
					twoWay = true;
					index = 3;
				} else if (args[2].startsWith("-")) {
					String numberStr = String.valueOf(args[2].charAt(2));
					if ("0123".contains(numberStr)) {
						whichNether = Integer.parseInt(numberStr);
					}
					index = 3;
				}
			}
			String str1;
			String str2;
			if (args.length <= index) {
				return false;
			} else {
				str1 = args[index].trim();
				while (str1.endsWith("\\")) {
					index++;
					if (args.length > index) {
						str1 = str1.substring(0, str1.length() - 1).concat(" " + args[index]);
					} else {
						return false;
					}
				}
			}
			if (args.length <= (index + 1)) {
				str2 = "";
			} else {
				str2 = args[index + 1];
				str2 = str2.trim();
				while (str2.endsWith("\\")) {
					index++;
					if (args.length > (index + 1)) {
						str2 = str2.substring(0, str2.length() - 1).concat(" " + args[index + 1]);
					}
				}
			}
			this.plugin.getPortalLinkConfig().addLinkAndSave(str1, str2, (sender instanceof Player) ? sender : null, twoWay, whichNether);
			return true;
		} else if (args[0].equals("unlink") && args.length > 1) {
			if (!sender.hasPermission("portallink.unlink")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to remove PortalLinks.");
				return true;
			}
			int index = 1;
			String str1 = args[index].trim();
			String str2;
			while (str1.endsWith("\\")) {
				index++;
				if (args.length > index) {
					str1 = str1.substring(0, str1.length() - 1).concat(" " + args[index]);
				} else {
					return false;
				}
			}
			if (args.length <= (index + 1)) {
				str2 = "";
			} else {
				str2 = args[index + 1].trim();
				while (str2.endsWith("\\")) {
					index++;
					if (args.length > (index + 1)) {
						str2 = str2.substring(0, str2.length() - 1).concat(" " + args[index + 1]);
					}
				}
			}
			this.plugin.getPortalLinkConfig().removeLink(str1, str2, (sender instanceof Player) ? sender : null);
			return true;
		} else if (args[0].equals("reload") && args.length == 1) {
			if (!sender.hasPermission("portallink.reload")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to reload PortalLink.");
				return true;
			}
			this.plugin.getPortalLinkConfig().load();
			this.plugin.broadcastReload(sender);
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (player.hasPermission("portallink.notify")) {
			final String adminMessage = this.plugin.adminMessage;
			if (adminMessage != null) {
				player.sendMessage(adminMessage);
			}
		}
	}

	@EventHandler(priority =  EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityPortal(final EntityPortalEvent event) {
		// Bukkit currently does not have the required APIs to be able to do this reliably
		if (this.plugin.getPortalLinkConfig().denyEntityPortal()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerPortal(final PlayerPortalEvent event) {
		final TeleportCause portalType = event.getCause();
		if ((event.getTo() != null && event.getTo().getWorld().getEnvironment() == Environment.THE_END) || portalType == TeleportCause.END_PORTAL) {
			processEndPortalEvent(event);
		} else {
			processNetherPortalEvent(event);
		}
	}

	private void processNetherPortalEvent(final PlayerPortalEvent event) {
		final Player player = event.getPlayer();
		final World fromWorld = player.getWorld();
		World toWorld;
		final Environment oldEnvironment;
		final Environment newEnvironment;
		boolean useDimension = true;
		final Map<String, PortalLinkLinkValue> definedLinks = plugin.getPortalLinkConfig().getUserDefinedLinks();
		if (definedLinks.containsKey(player.getWorld().getName())) {
			final PortalLinkLinkValue linkValue = definedLinks.get(fromWorld.getName());
			int whichNether = linkValue.getWhichNether();
			if ((whichNether & 1) == 0) {
				oldEnvironment = Environment.NORMAL;
			} else {
				oldEnvironment = Environment.NETHER;
			}
			if ((whichNether & 2) == 0) {
				newEnvironment = Environment.NORMAL;
			} else {
				newEnvironment = Environment.NETHER;
			}
			if (oldEnvironment == newEnvironment) {
				useDimension = false;
			}
			if (!linkValue.getString().equals("")) {
				toWorld = plugin.getWorld(linkValue.getString());
				if (toWorld == null) {
					player.sendMessage("Loading world... You will be teleported when the world has been loaded.");
					toWorld = plugin.createWorld(linkValue.getString(), newEnvironment);
					player.sendMessage("World loaded! Teleporting...");
				}
			} else {
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "The Nether has been disabled for " + player.getWorld().getName() + ".");
				return;
			}
		} else {
			oldEnvironment = player.getWorld().getEnvironment() == Environment.NETHER ? Environment.NETHER : Environment.NORMAL;
			if (!plugin.getAllowNether(fromWorld)) {
				return;
			}
			final String toWorldName;
			if (oldEnvironment == Environment.NETHER) {
				newEnvironment = Environment.NORMAL;
				toWorldName = player.getWorld().getName().replaceFirst("_nether$", "");
			} else {
				newEnvironment = Environment.NETHER;
				toWorldName = player.getWorld().getName().concat("_nether");
			}
			toWorld = plugin.getWorld(toWorldName);
			if (toWorld == null) {
				player.sendMessage("Loading world... You will be teleported when the world has been loaded.");
				toWorld = plugin.createWorld(toWorldName, newEnvironment);
				player.sendMessage("World loaded! Teleporting...");
			}
			if (toWorld.getEnvironment().equals(fromWorld.getEnvironment())) useDimension = false;
		}
		final double blockRatio = useDimension ? (oldEnvironment == Environment.NETHER ? 8 : 0.125) : 1;

		final Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
		final Location toLocation = new Location(toWorld, (player.getLocation().getX() * blockRatio), player.getLocation().getY(), (player.getLocation().getZ() * blockRatio), player.getLocation().getYaw(), player.getLocation().getPitch());
		event.setTo(toLocation);
		event.setFrom(fromLocation);
		event.useTravelAgent(true);
	}

	private void processEndPortalEvent(PlayerPortalEvent event) {
		final Player player = event.getPlayer();
		final World fromWorld = player.getWorld();
		if (event.getFrom().getWorld().getEnvironment() == Environment.THE_END) {
			World mainOverworld = null;
			for (final World world : this.plugin.getServer().getWorlds()) {
				if (world.getEnvironment() == Environment.NORMAL) {
					mainOverworld = world;
					break;
				}
			}
			if (mainOverworld == null) {
				this.plugin.logSevere("The main overworld could not be found!");
				player.sendMessage(ChatColor.RED + "An internal error has occurred whilst finding the main overworld!");
				return;
			}
			Location toLoc = player.getBedSpawnLocation();
			if (toLoc == null || !mainOverworld.equals(toLoc.getWorld())) {
				toLoc = mainOverworld.getSpawnLocation();
			}
			final Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
			event.setTo(toLoc);
			event.setFrom(fromLocation);
			event.useTravelAgent(false);
		} else {
			World toWorld = null;
			for (final World world : this.plugin.getServer().getWorlds()) {
				if (world.getEnvironment() == Environment.THE_END) {
					toWorld = world;
					break;
				}
			}
			if (toWorld == null) {
				this.plugin.logSevere("The main end could not be found!");
				player.sendMessage(ChatColor.RED + "An internal error has occurred whilst finding the main end!");
				return;
			}
			/*final Map<String, PortalLinkLinkValue> definedLinks = plugin.getPortalLinkConfig().getUserDefinedLinks();
			if (definedLinks.containsKey(player.getWorld().getName())) {
				PortalLinkLinkValue linkValue = definedLinks.get(player.getWorld().getName());
				/*fromWorld = player.getWorld();
				PortalLinkLinkValue linkValue = definedLinks.get(fromWorld.getName());
				Environment environment = Environment.THE_END;
				if (!linkValue.getString().equals("")) {
					toWorld = plugin.getWorld(linkValue.getString());
					if (toWorld == null) {
						player.sendMessage("Loading world... You will be teleported when the world has been loaded.");
						toWorld = plugin.createWorld(linkValue.getString(), environment);
						player.sendMessage("World loaded! Teleporting...");
					}
				} else {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "The End has been disabled for " + player.getWorld().getName() + ".");
					return;
				}*//*
			}
			if (toWorld == null) {
				Environment environment = player.getWorld().getEnvironment();
				for (World world1 : plugin.getServer().getWorlds()) {
					if (world1.getEnvironment().equals(environment)) {
						if (world1.getName().equals(player.getWorld().getName())) {
							fromWorld = world1;
						}
					}
				}
				if (!plugin.getAllowNether(fromWorld)) {
					return;
				}
				if (fromWorld == null) {
					this.plugin.logSevere("Unable To Match A World To The Player's World!");
					return;
				}
				if (environment == Environment.THE_END) {
					toWorld = plugin.getWorld(fromWorld.getName().replaceAll("_nether", "").replaceAll("_the_end", ""));
				} else {
					toWorld = plugin.getWorld(fromWorld.getName().replaceAll("_nether", "").replaceAll("_the_end", "") + "_the_end");
				}
				if (toWorld == null) {
					player.sendMessage("Loading world... You will be teleported when the world has been loaded.");
					if (environment == Environment.THE_END) {
						toWorld = plugin.createWorld(fromWorld.getName().replaceAll("_nether", "").replaceAll("_the_end", ""), Environment.NORMAL);
					} else {
						toWorld = plugin.createWorld(fromWorld.getName().replaceAll("_nether", "").replaceAll("_the_end", "") + "_the_end", Environment.THE_END);
					}
					player.sendMessage("World loaded! Teleporting...");
				}
			}*/

			final Location fromLocation = new Location(fromWorld, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
			final Location toLocation = new Location(toWorld, 100, 50, 0, 90, 0);
			event.setTo(toLocation);
			event.setFrom(fromLocation);
			event.useTravelAgent(true);
		}
	}
	
	/*private String replaceLast(String string, String from, String to) {
	     int lastIndex = string.lastIndexOf(from);
	     if (lastIndex < 0) return string;
	     String tail = string.substring(lastIndex).replaceFirst(from, to);
	     return string.substring(0, lastIndex) + tail;
	}*/
}
