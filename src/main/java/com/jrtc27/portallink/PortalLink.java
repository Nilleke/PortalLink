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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PortalLink extends JavaPlugin {
	private PluginDescriptionFile pdf;
	private final PortalLinkListener plListener = new PortalLinkListener(this);
	public final PortalLinkConfig plConfig = new PortalLinkConfig(this);
	private Logger logger;

	@Override
	public void onEnable() {
		this.logger = this.getLogger();
		this.getCommand("pl").setExecutor(plListener);
		this.pdf = this.getDescription();
		this.plConfig.load();
		final PluginManager pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(plListener, this);
		this.logInfo(pdf.getFullName() + " is enabled!");
	}

	public boolean getAllowNether(final World world) {
		return this.getServer().getAllowNether();
	}

	@Override
	public void onDisable() {
		//plConfig.saveUserDefinedLinks();
		this.logger.info(pdf.getFullName() + " is disabled!");
	}

	public PortalLinkConfig getPortalLinkConfig() {
		return this.plConfig;
	}

	public World createWorld(final String name, final Environment environment) {
		return new WorldCreator(name).environment(environment).createWorld();
	}

	public World getWorld(final String name) {
		return this.getServer().getWorld(name);
	}

	public void log(final Level level, final String message) {
		this.logger.log(level, message);
	}

	public void logInfo(final String message) {
		this.log(Level.INFO, message);
	}

	public void logWarning(final String message) {
		this.log(Level.WARNING, message);
	}

	public void logSevere(final String message) {
		this.log(Level.SEVERE, message);
	}
}
