package com.twosquaredstudios.PortalLink;

import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

@SuppressWarnings("unused")
public class PortalLink extends JavaPlugin {
	private PluginDescriptionFile pdfFile;
	private final PortalLinkPlayerListener plPlayerListener = new PortalLinkPlayerListener(this);
	Logger log = Logger.getLogger("Minecraft");

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_PORTAL, plPlayerListener, Event.Priority.Highest, this);
		pdfFile = this.getDescription();
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}
	
	@Override
	public void onDisable() {
		log.info(pdfFile.getName() + " is disabled!");
	}

}
