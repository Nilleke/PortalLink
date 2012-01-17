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

package com.twosquaredstudios.PortalLink;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class PortalLink extends JavaPlugin {
	private PluginDescriptionFile pdfFile;
	private final PortalLinkPlayerListener plPlayerListener = new PortalLinkPlayerListener(this);
	private final PortalLinkConfig plConfig = new PortalLinkConfig(this);
	private Method allowNetherMethod = null;
	private Map<World,Boolean> storedAllowNether = new HashMap<World,Boolean>();
	Logger log = Logger.getLogger("Minecraft");

	@Override
	public void onEnable() {
		getCommand("pl").setExecutor(new CommandExecutor() {
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (args.length == 0) return false;
				if (args[0].equalsIgnoreCase("link") && args.length > 1) {
					if (!sender.hasPermission("portallink.link")) {
						sender.sendMessage(ChatColor.RED + "You do not have permission to define PortalLinks.");
						return true;
					}
					boolean twoway = false;
					int whichNether = 0;
					int index = 1;
					if (args[1].startsWith("-") && args[1].length() <= 3) {
						if (args[1].contains("b")) {
							twoway = true;
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
							twoway = true;
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
						str1 = args[index];
						str1 = str1.trim();
						boolean stillEndsInBckSlsh = str1.endsWith("\\");
						while(stillEndsInBckSlsh) {
							index++;
							if (args.length > index) {
								str1 = str1.substring(0, str1.length() - 1);
								str1 = str1.concat(" " + args[index]);
								stillEndsInBckSlsh = str1.endsWith("\\");
							} else {
								return false;
							}
						}
					}
					if (args.length <= (index+1)) {
						str2 = "";
					} else {
						str2 = args[index+1];
						str2 = str2.trim();
						boolean stillEndsInBckSlsh = str2.endsWith("\\");
						while(stillEndsInBckSlsh) {
							index++;
							if (args.length > (index+1)) {
								str2 = str2.substring(0, str2.length() - 1);
								str2 = str2.concat(" " + args[index+1]);
								stillEndsInBckSlsh = str2.endsWith("\\");
							}
						}
					}
					plConfig.addLink(str1, str2, (sender instanceof Player) ? sender : null, twoway, whichNether);
					return true;
				} else if (args[0].equals("unlink") && args.length > 1) {
					if (!sender.hasPermission("portallink.unlink")) {
						sender.sendMessage(ChatColor.RED + "You do not have permission to remove PortalLinks.");
						return true;
					}
					int index = 1;
					String str1;
					String str2;
					str1 = args[index];
					str1 = str1.trim();
					boolean stillEndsInBckSlsh = str1.endsWith("\\");
					while(stillEndsInBckSlsh) {
						index++;
						if (args.length > index) {
							str1 = str1.substring(0, str1.length() - 1);
							str1 = str1.concat(" " + args[index]);
							stillEndsInBckSlsh = str1.endsWith("\\");
						} else {
							return false;
						}
					}
					if (args.length <= (index+1)) {
						str2 = "";
					} else {
						str2 = args[index+1];
						str2 = str2.trim();
						boolean stillEndsInBckSlsh1 = str2.endsWith("\\");
						while(stillEndsInBckSlsh1) {
							index++;
							if (args.length > (index+1)) {
								str2 = str2.substring(0, str2.length() - 1);
								str2 = str2.concat(" " + args[index+1]);
								stillEndsInBckSlsh1 = str2.endsWith("\\");
							}
						}
					}
					plConfig.removeLink(str1, str2, (sender instanceof Player) ? sender : null);
					return true;
				} else if (args[0].equals("reload") && args.length == 1) {
					if (!sender.hasPermission("portallink.reload")) {
						sender.sendMessage(ChatColor.RED + "You do not have permission to reload PortalLink.");
						return true;
					}
					plConfig.loadUserDefinedLinks();
					if (sender instanceof Player) {
						sender.sendMessage("PortalLink has been reloaded!");
					}
					logInfo("PortalLink has been reloaded!");
					return true;
				}
				return false;
			}
		});
		pdfFile = this.getDescription();
		plConfig.loadUserDefinedLinks();
		Method m = null;
		try {
			m = World.class.getMethod("getAllowNether");
		} catch (NoSuchMethodException e) {
			
		}
		allowNetherMethod = m;
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_PORTAL, plPlayerListener, Event.Priority.Highest, this);
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}
	
	public boolean getAllowNether(World world) {
		if (storedAllowNether.containsKey(world)) {
			return storedAllowNether.get(world).booleanValue();
		}
		if (allowNetherMethod != null) {
			try {
				Boolean bool = (Boolean)allowNetherMethod.invoke(world);
				storedAllowNether.put(world, bool);
				return bool.booleanValue();
			} catch (Exception e) {
				
			}
		}
		storedAllowNether.put(world, true);
		return true;
	}
	
	@Override
	public void onDisable() {
		//plConfig.saveUserDefinedLinks();
		storedAllowNether.clear();
		log.info(pdfFile.getName() + " is disabled!");
	}
	
	public PortalLinkConfig getPortalLinkConfig() {
		return plConfig;
	}
	
	public void logInfo(String string) {
		log.info("[" + pdfFile.getName() + "] " + string);
	}
	
	public void logWarning(String string) {
		log.warning("[" + pdfFile.getName() + "] " + string);
	}
	
	public void logError(String string) {
		log.severe("[" + pdfFile.getName() + "] " + string);
	}
}
