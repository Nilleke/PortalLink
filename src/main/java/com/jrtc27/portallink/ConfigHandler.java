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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;

public class ConfigHandler {
	private static final String LINKS_FILE_NAME = "links.properties";
	private static final String LINKS_TEMP_FILE_NAME = LINKS_FILE_NAME + ".tmp";
	private final Pattern LINK_PATTERN = Pattern.compile("(^(?==)|(?<==)(?=[^=])|(?<=[^=])(?==)|(?<==)$)");

	private final PortalLink plugin;

	private Map<String, LinkEntry> definedLinks = new HashMap<String, LinkEntry>();
	private boolean denyEntityPortal;

	public ConfigHandler(final PortalLink plugin) {
		this.plugin = plugin;
	}

	public Map<String, LinkEntry> getUserDefinedLinks() {
		return this.definedLinks;
	}

	public boolean denyEntityPortal() {
		return this.denyEntityPortal;
	}

	public void load() {
		this.loadConfig();
		this.loadUserDefinedLinks();
	}

	private void loadConfig() {
		this.plugin.saveDefaultConfig();
		final MemoryConfiguration config = this.plugin.getConfig();
		this.denyEntityPortal = config.getBoolean("deny-entity-portal", true);
		this.plugin.checkForUpdates = config.getBoolean("check-updates", true);
	}

	private void loadUserDefinedLinks() {
		this.definedLinks.clear();
		try {
			final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.plugin.getDataFolder(), LINKS_FILE_NAME)), "UTF-8"));
			String s;
			int line = 0;
			while ((s = in.readLine()) != null) {
				line++;
				s = s.trim();
				if (s.isEmpty()) continue;
				if (s.startsWith("#")) continue;
				final String[] args = LINK_PATTERN.split(s, -1); // -1 means we get the trailing space
				if (args.length < 3) {
					this.plugin.logWarning("Missing \"=\" sign(s) on line " + line + " of " + LINKS_FILE_NAME + ".");
					continue;
				} else if (args.length > 3) {
					this.plugin.logWarning("Only one link can be specified per line - ignoring all other links on line " + line + " of " + LINKS_FILE_NAME + ".");
				}
				args[0] = args[0].trim();
				args[2] = args[2].trim();
				boolean twoWay = args[1].equals("==");
				int whichNether = 0;
				if (args[0].startsWith("<") && args[0].endsWith(">")) {
					whichNether |= 1;
					args[0] = args[0].substring(1, args[0].length() - 1);
				}
				if (args[2].startsWith("<") && args[2].endsWith(">")) {
					whichNether |= 2;
					args[2] = args[2].substring(1, args[2].length() - 1);
				}
				this.addLink(args[0], args[2], null, twoWay, whichNether, false);
			}
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			File directory = this.plugin.getDataFolder();
			if (!directory.isDirectory()) {
				if (!directory.mkdir()) {
					this.plugin.logSevere("Unable to create plugin data directory!");
					return;
				}
			}
			try {
				Writer out = new OutputStreamWriter(new FileOutputStream(new File(this.plugin.getDataFolder(), LINKS_FILE_NAME)), "UTF-8");
				out.write("# Place any custom links inside this file.\n");
				out.write("# Any world will be automatically linked with its name followed by \"_nether\".\n");
				out.write("# Links should be specified with the format World=<NetherWorld>.\n");
				out.write("# All nether worlds MUST be surrounded by < and > as shown in the above example.\n");
				out.write("# You can link two nether worlds or two normal worlds if desired.\n");
				out.write("# By default all links are one way (left world to right world) - to link them both\n");
				out.write("# ways, change the \"=\" to \"==\".\n");
				out.write("# A blank link will stop a player from being able to use a portal in that world.\n");
				out.write("# Only one link per line (extra links will be ignored).\n");
				out.write("# Later links will override previous ones.\n");
				out.write("# Lines beginning with a hash (\"#\") are treated as comments and so are ignored.\n");
				out.close();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addLink(final String str1, final String str2, final CommandSender sender, final boolean twoWay, int whichNether, final boolean announceCreate) {
		if (!str1.equals("")) {
			if (this.definedLinks.containsKey(str1)) {
				if (sender != null) {
					sender.sendMessage("Overriding previous link for \"" + str1 + "\".");
				}
				this.plugin.logWarning("Overriding previous link for \"" + str1 + "\".");
			}
			this.definedLinks.put(str1, new LinkEntry(str2, whichNether));
		}
		if (announceCreate) {
			if (sender != null) {
				sender.sendMessage("Creating link...");
			}
			this.plugin.logInfo("Creating link...");
		}
		final Environment environmentForWorld1 = (whichNether & 1) == 0 ? Environment.NORMAL : Environment.NETHER;
		final Environment environmentForWorld2 = (whichNether & 2) == 0 ? Environment.NORMAL : Environment.NETHER;
		if (!str1.equals("")) this.plugin.createWorld(str1, environmentForWorld1);
		if (!str2.equals("")) this.plugin.createWorld(str2, environmentForWorld2);
		if (twoWay) {
			whichNether = (whichNether & 1) << 1 | (whichNether & 2) >> 1; // Swap bits 0 and 1
			if (!str2.equals("")) {
				if (this.definedLinks.containsKey(str2)) {
					if (sender != null) {
						sender.sendMessage("Overriding previous link for \"" + str2 + "\".");
					}
					this.plugin.logWarning("Overriding previous link for \"" + str2 + "\".");
				}
				this.definedLinks.put(str2, new LinkEntry(str1, whichNether));
			}
		}
		if (announceCreate) {
			if (sender != null) {
				sender.sendMessage("Created link!");
			}
			this.plugin.logInfo("Created link!");
		}
	}

	public void addLinkAndSave(String str1, String str2, final CommandSender sender, final boolean twoWay, int whichNether) {
		this.addLink(str1, str2, sender, twoWay, whichNether, true);
		if ((whichNether & 1) != 0) {
			str1 = "<" + str1 + ">";
		}
		if ((whichNether & 2) != 0) {
			str2 = "<" + str2 + ">";
		}
		final String outStr = "\n" + str1 + (twoWay ? "==" : "=") + str2;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(this.plugin.getDataFolder(), LINKS_FILE_NAME), true), "UTF-8"));
			out.write(outStr);
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			this.plugin.logSevere("The save file could not be accessed!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void removeLink(String str1, String str2, CommandSender sender) {
		BufferedReader in = null;
		Writer out = null;
		boolean removedLink = false;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(this.plugin.getDataFolder(), LINKS_FILE_NAME)), "UTF-8"));
			out = new OutputStreamWriter(new FileOutputStream(new File(this.plugin.getDataFolder(), LINKS_TEMP_FILE_NAME)), "UTF-8");
			String s;
			while ((s = in.readLine()) != null) {
				final String line = s + "\n";
				s = s.trim();
				if (s.isEmpty() || s.startsWith("#")) {
					out.write(line);
				} else {
					final String[] args = LINK_PATTERN.split(s, -1); // -1 means we get the trailing space
					args[0] = args[0].trim();
					args[2] = args[2].trim();
					boolean twoWay = args[1].equals("==");
					if (args[0].startsWith("<") && args[0].endsWith(">")) {
						args[0] = args[0].substring(1, args[0].length() - 1);
					}
					if (args[2].startsWith("<") && args[2].endsWith(">")) {
						args[2] = args[2].substring(1, args[2].length() - 1);
					}
					if ((args[0].equals(str1) && (args[2].equals(str2) || str2.isEmpty())) || (twoWay && args[0].equals(str2) && args[2].equals(str1))) {
						out.write("# Removed: " + line);
						removedLink = true;
					} else {
						out.write(line);
					}
				}
			}
			in.close();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Unable to find " + LINKS_FILE_NAME + "!");
			}
			this.plugin.logSevere("Unable to find " + LINKS_FILE_NAME + "!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (removedLink) {
			File tempFile = new File(this.plugin.getDataFolder(), LINKS_TEMP_FILE_NAME);
			File targetFile = new File(this.plugin.getDataFolder(), LINKS_FILE_NAME);
			if (!tempFile.renameTo(targetFile)) {
				if (!targetFile.delete()) {
					// Do nothing
				}
				if (!tempFile.renameTo(targetFile)) {
					this.plugin.logSevere("Unable to rename the temporary file!");
				} else {
					if (sender != null) {
						sender.sendMessage("Link successfully removed!");
					}
					this.plugin.logInfo("Link successfully removed!");
				}
			} else {
				if (sender != null) {
					sender.sendMessage("Link successfully removed!!");
				}
				this.plugin.logInfo("Link successfully removed!");
			}
			this.definedLinks.clear();
			loadUserDefinedLinks();
		} else {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "No matching links were found!");
			}
			this.plugin.logWarning("No matching links were found!");
			File tempFile = new File(this.plugin.getDataFolder(), LINKS_TEMP_FILE_NAME);
			if (!tempFile.delete()) {
				this.plugin.logSevere("Unable to delete the temporary file!");
			}
		}
	}
}
