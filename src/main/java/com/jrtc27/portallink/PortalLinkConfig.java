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

import org.bukkit.ChatColor;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;

public class PortalLinkConfig {
	private final PortalLink plugin;
	private Map<String, PortalLinkLinkValue> definedLinks = new HashMap<String, PortalLinkLinkValue>();
	private boolean denyEntityPortal;
	//private LinkedList<String> overriddenLines = new LinkedList<String>();

	public PortalLinkConfig(final PortalLink plugin) {
		this.plugin = plugin;
	}

	public Map<String, PortalLinkLinkValue> getUserDefinedLinks() {
		return definedLinks;
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
	}

	private void loadUserDefinedLinks() {
		this.definedLinks.clear();
		try {
			final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(plugin.getDataFolder() + "/links.properties"), "UTF-8"));
			String s = "";
			Integer line = 0;
			while ((s = in.readLine()) != null) {
				line++;
				int length = 0;
				s = s.trim();
				if (s.isEmpty()) continue;
				if (s.endsWith("=")) length++;
				if (s.startsWith("#")) continue;
				final String tempArgs[] = s.split("=");
				length += tempArgs.length;
				final String args[] = new String[length];
				System.arraycopy(tempArgs, 0, args, 0, tempArgs.length);
				if (length > tempArgs.length) {
					args[length - 1] = "";
				}
				args[0] = args[0].trim();
				args[1] = args[1].trim();
				if (args.length > 2) args[2] = args[2].trim();
				boolean twoWay = false;
				if (args[1].equals("") && args.length > 2) {
					args[1] = args[2];
					twoWay = true;
				}
				if (args.length > (twoWay ? 3 : 2)) {
					plugin.logWarning("Only one link can be specified per line - ignoring all other links on the line.");
					//overriddenLines.add(s);
				}
				int whichNether = 0;
				if (args[0].startsWith("<") && args[0].endsWith(">")) {
					whichNether += 1;
					args[0] = args[0].substring(1, args[0].length() - 1);
				}
				if (args[1].startsWith("<") && args[1].endsWith(">")) {
					whichNether += 2;
					args[1] = args[1].substring(1, args[1].length() - 1);
				}
				if (!args[0].equals("")) {
					if (definedLinks.containsKey(args[0])) {
						plugin.logWarning("Overriding previous link for \"" + args[0] + "\".");
					}
					definedLinks.put(args[0], new PortalLinkLinkValue(args[1], whichNether));
				}
				Environment environmentForWorld1 = Environment.NORMAL;
				Environment environmentForWorld2 = Environment.NORMAL;
				switch (whichNether) {
					case 0:
						environmentForWorld1 = Environment.NORMAL;
						environmentForWorld2 = Environment.NORMAL;
						break;
					case 1:
						environmentForWorld1 = Environment.NETHER;
						environmentForWorld2 = Environment.NORMAL;
						break;
					case 2:
						environmentForWorld1 = Environment.NORMAL;
						environmentForWorld2 = Environment.NETHER;
						break;
					case 3:
						environmentForWorld1 = Environment.NETHER;
						environmentForWorld2 = Environment.NETHER;
						break;
					default:
						break;
				}
				if (!args[0].equals("")) plugin.createWorld(args[0], environmentForWorld1);
				if (!args[1].equals("")) plugin.createWorld(args[1], environmentForWorld2);
				if (twoWay) {
					if (whichNether == 2) {
						whichNether--;
					} else if (whichNether == 1) {
						whichNether++;
					}
					if (!args[1].equals("")) {
						if (definedLinks.containsKey(args[1])) {
							plugin.logWarning("Overriding previous link for \"" + args[1] + "\".");
						}
						definedLinks.put(args[1], new PortalLinkLinkValue(args[0], whichNether));
					}
				}
			}
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			File directory = plugin.getDataFolder();
			if (!directory.isDirectory()) {
				if (!directory.mkdir()) {
					plugin.logSevere("Unable to create plugin data directory!");
					return;
				}
			}
			try {
				Writer out = new OutputStreamWriter(new FileOutputStream(plugin.getDataFolder() + "/links.properties"), "UTF-8");
				out.write("# Place any custom links inside this file.\n");
				out.write("# Any world will be automatically linked with its name followed by \"_nether\".\n");
				out.write("# Links should be specified with the format World=<NetherWorld>.\n");
				out.write("# All nether worlds MUST be surrounded by < and > as shown in the above example.\n");
				out.write("# You can link two nethers or two normal worlds if desired.\n");
				out.write("# By default all links are one way (left world to right world) - to link them both\n");
				out.write("# ways, change the \"=\" to \"==\".\n");
				out.write("# A blank link will stop a player from being able to use a portal in that world.\n");
				out.write("# Only one link per line (extra links will be ignored).\n");
				out.write("# Later links will override previous ones.\n");
				out.write("# Lines beginning with a hash (\"#\") are treated as comments and so are ignored.\n");
				out.write("\n");
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
	/*
	public void saveUserDefinedLinks() {
		String outStr = "";
		for (String string : overriddenLines) {
			outStr = outStr.concat(string + "\n");
		}
		Iterator<Map.Entry<String,String>> iterator = definedLinks.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String,String> entry = iterator.next();
			outStr = outStr.concat(entry.getKey() + "=" + entry.getValue() + "\n");
		}
		try {
			Writer out = new OutputStreamWriter(new FileOutputStream(plugin.getDataFolder() + "/links.properties.tmp"), "UTF-8");
			out.write(outStr);
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			plugin.logSevere("The save file could not be accessed!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File tempFile = new File(plugin.getDataFolder() + "/links.properties.tmp");
		File targetFile = new File(plugin.getDataFolder() + "/links.properties");
		if (!tempFile.renameTo(targetFile)) {
			plugin.logSevere("Unable to rename the temporary file!");
		}
	}
	*/

	public void addLink(String str1, String str2, int whichNether) {
		addLink(str1, str2, null, false, whichNether);
	}

	public void addLink(String str1, String str2, CommandSender sender, boolean twoway, int whichNether) {
		String outStr = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(plugin.getDataFolder() + "/links.properties"), "UTF-8"));
			String s = "";
			while ((s = in.readLine()) != null) {
				outStr = outStr.concat(s + "\n");
			}
			in.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}
		String str1Mod = str1;
		String str2Mod = str2;
		switch (whichNether) {
			case 0:
				break;
			case 1:
				str1Mod = "<" + str1Mod + ">";
				break;
			case 2:
				str2Mod = "<" + str2Mod + ">";
				break;
			case 3:
				str1Mod = "<" + str1Mod + ">";
				str2Mod = "<" + str2Mod + ">";
				break;
			default:
				break;
		}
		if (twoway) {
			outStr = outStr.concat(str1Mod + "==" + str2Mod + "\n");
			if (definedLinks.containsKey(str1)) {
				if (sender != null) {
					sender.sendMessage("Overriding previous link for \"" + str1 + "\".");
				}
				plugin.logWarning("Overriding previous link for \"" + str1 + "\".");
			}
			definedLinks.put(str1, new PortalLinkLinkValue(str2, whichNether));
			if (sender != null) {
				sender.sendMessage("Creating link...");
			}
			plugin.logInfo("Creating link...");
			Environment environmentForWorld1 = Environment.NORMAL;
			Environment environmentForWorld2 = Environment.NORMAL;
			switch (whichNether) {
				case 0:
					environmentForWorld1 = Environment.NORMAL;
					environmentForWorld2 = Environment.NORMAL;
					break;
				case 1:
					environmentForWorld1 = Environment.NETHER;
					environmentForWorld2 = Environment.NORMAL;
					break;
				case 2:
					environmentForWorld1 = Environment.NORMAL;
					environmentForWorld2 = Environment.NETHER;
					break;
				case 3:
					environmentForWorld1 = Environment.NETHER;
					environmentForWorld2 = Environment.NETHER;
					break;
				default:
					break;
			}
			if (!str1.equals("")) plugin.createWorld(str1, environmentForWorld1);
			if (!str2.equals("")) plugin.createWorld(str2, environmentForWorld2);
			if (whichNether == 2) {
				whichNether--;
			} else if (whichNether == 1) {
				whichNether++;
			}
			if (definedLinks.containsKey(str2)) {
				if (sender != null) {
					sender.sendMessage("Overriding previous link for \"" + str2 + "\".");
				}
				plugin.logWarning("Overriding previous link for \"" + str2 + "\".");
			}
			definedLinks.put(str2, new PortalLinkLinkValue(str1, whichNether));
		} else {
			outStr = outStr.concat(str1Mod + "=" + str2Mod + "\n");
			if (definedLinks.containsKey(str1)) {
				if (sender != null) {
					sender.sendMessage("Overriding previous link for \"" + str1 + "\".");
				}
				plugin.logWarning("Overriding previous link for \"" + str1 + "\".");
			}
			definedLinks.put(str1, new PortalLinkLinkValue(str2, whichNether));
			if (sender != null) {
				sender.sendMessage("Creating link...");
			}
			plugin.logInfo("Creating link...");
			Environment environmentForWorld1 = Environment.NORMAL;
			Environment environmentForWorld2 = Environment.NORMAL;
			switch (whichNether) {
				case 0:
					environmentForWorld1 = Environment.NORMAL;
					environmentForWorld2 = Environment.NORMAL;
					break;
				case 1:
					environmentForWorld1 = Environment.NETHER;
					environmentForWorld2 = Environment.NORMAL;
					break;
				case 2:
					environmentForWorld1 = Environment.NORMAL;
					environmentForWorld2 = Environment.NETHER;
					break;
				case 3:
					environmentForWorld1 = Environment.NETHER;
					environmentForWorld2 = Environment.NETHER;
					break;
				default:
					break;
			}
			if (!str1.equals("")) plugin.createWorld(str1, environmentForWorld1);
			if (!str2.equals("")) plugin.createWorld(str2, environmentForWorld2);
		}
		try {
			Writer out = new OutputStreamWriter(new FileOutputStream(plugin.getDataFolder() + "/links.properties.tmp"), "UTF-8");
			out.write(outStr);
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			plugin.logSevere("The save file could not be accessed!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File tempFile = new File(plugin.getDataFolder() + "/links.properties.tmp");
		File targetFile = new File(plugin.getDataFolder() + "/links.properties");
		if (!tempFile.renameTo(targetFile)) {
			if (!targetFile.delete()) {

			}
			if (!tempFile.renameTo(targetFile)) {
				plugin.logSevere("Unable to rename the temporary file!");
			} else {
				if (sender != null) {
					sender.sendMessage("Link successfully created!");
				}
				plugin.logInfo("Link successfully created!");
			}
		} else {
			if (sender != null) {
				sender.sendMessage("Link successfully created!");
			}
			plugin.logInfo("Link successfully created!");
		}
	}

	public void removeLink(String str1, String str2, CommandSender sender) {
		BufferedReader in = null;
		Writer out = null;
		boolean removedLink = false;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(plugin.getDataFolder() + "/links.properties"), "UTF-8"));
			String s = "";
			out = new OutputStreamWriter(new FileOutputStream(plugin.getDataFolder() + "/links.properties.tmp"), "UTF-8");
			while ((s = in.readLine()) != null) {
				String line = s;
				line = line.concat("\n");
				s = s.trim();
				if (s.isEmpty() || s.startsWith("#")) {
					out.write(line);
				} else {
					int length = 0;
					boolean twoway = false;
					if (s.endsWith("=")) length++;
					String tempArgs[] = s.split("=");
					length += tempArgs.length;
					String args[] = new String[length];
					System.arraycopy(tempArgs, 0, args, 0, tempArgs.length);
					if (length > tempArgs.length) {
						args[length - 1] = "";
					}
					args[0] = args[0].trim();
					args[1] = args[1].trim();
					if (args.length > 2) args[2] = args[2].trim();
					if (args[1].equals("") && args.length > 2) {
						twoway = true;
						args[1] = args[2];
					}
					if (args[0].startsWith("<") && args[0].endsWith(">")) {
						args[0] = args[0].substring(1, args[0].length() - 1);
					}
					if (args[1].startsWith("<") && args[1].endsWith(">")) {
						args[1] = args[1].substring(1, args[1].length() - 1);
					}
					if ((args[0].equals(str1) && (args[1].equals(str2) || str2.isEmpty())) || (twoway && args[0].equals(str2) && args[1].equals(str1))) {
						out.write("# Removed: " + line);
						removedLink = true;
					}
				}
			}
			in.close();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Unable to find links.properties!");
			}
			plugin.logSevere("Unable to find links.properties!");
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
			File tempFile = new File(plugin.getDataFolder() + "/links.properties.tmp");
			File targetFile = new File(plugin.getDataFolder() + "/links.properties");
			if (!tempFile.renameTo(targetFile)) {
				if (!targetFile.delete()) {

				}
				if (!tempFile.renameTo(targetFile)) {
					plugin.logSevere("Unable to rename the temporary file!");
				} else {
					if (sender != null) {
						sender.sendMessage("Link successfully removed!");
					}
					plugin.logInfo("Link successfully removed!");
				}
			} else {
				if (sender != null) {
					sender.sendMessage("Link successfully removed!!");
				}
				plugin.logInfo("Link successfully removed!");
			}
			definedLinks.clear();
			loadUserDefinedLinks();
		} else {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "No matching links were found!");
			}
			plugin.logWarning("No matching links were found!");
			File tempFile = new File(plugin.getDataFolder() + "/links.properties.tmp");
			if (!tempFile.delete()) {
				plugin.logSevere("Unable to delete the temporary file!");
			}
		}
	}
}
