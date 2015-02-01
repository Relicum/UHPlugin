/**
 *  Plugin UltraHardcore Reloaded (UHPlugin)
 *  Copyright (C) 2013 azenet
 *  Copyright (C) 2014-2015 Amaury Carrade
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see [http://www.gnu.org/licenses/].
 */

package com.relicum.UHPlugin;

import com.relicum.UHPlugin.i18n.I18n;
import com.relicum.UHPlugin.integration.UHDynmapIntegration;
import com.relicum.UHPlugin.integration.UHProtocolLibIntegrationWrapper;
import com.relicum.UHPlugin.integration.UHSpectatorPlusIntegration;
import com.relicum.UHPlugin.integration.UHWorldBorderIntegration;
import com.relicum.UHPlugin.listeners.UHCraftingListener;
import com.relicum.UHPlugin.listeners.UHGameListener;
import com.relicum.UHPlugin.listeners.UHGameplayListener;
import com.relicum.UHPlugin.task.UpdateTimerTask;
import com.relicum.titleapi.TitleApi;
import com.relicum.titleapi.TitleMaker;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class UHPlugin extends JavaPlugin {

    private UHPluginCommand commandManager = null;
    private UHTabCompleter tabCompleter = null;

    private UHTeamManager teamManager = null;
    private UHSpawnsManager spawnsManager = null;
    private UHGameManager gameManager = null;
    private UHScoreboardManager scoreboardManager = null;
    private UHMOTDManager motdManager = null;
    private UHBorderManager borderManager = null;
    private UHRecipeManager recipeManager = null;
    private UHTeamChatManager teamChatManager = null;
    private UHTimerManager timerManager = null;

    private UHRuntimeCommandsExecutor runtimeCommandsExecutor = null;

    private UHFreezer freezer = null;

    private UHProTipsSender protipsSender = null;

    private UHWorldBorderIntegration wbintegration = null;
    private UHSpectatorPlusIntegration spintegration = null;
    private UHDynmapIntegration dynmapintegration = null;
    private UHProtocolLibIntegrationWrapper protocollibintegrationwrapper = null;
    private TitleMaker titleMaker;
    private I18n i18n = null;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        if (getConfig().getString("lang") == null) {
            i18n = new I18n(this);
        } else {
            i18n = new I18n(this, getConfig().getString("lang"));
        }

        teamManager = new UHTeamManager(this);
        gameManager = new UHGameManager(this);
        spawnsManager = new UHSpawnsManager(this);
        borderManager = new UHBorderManager(this);
        recipeManager = new UHRecipeManager(this);
        teamChatManager = new UHTeamChatManager(this);
        timerManager = new UHTimerManager();

        runtimeCommandsExecutor = new UHRuntimeCommandsExecutor(this);

        freezer = new UHFreezer(this);

        protipsSender = new UHProTipsSender(this);

        scoreboardManager = new UHScoreboardManager(this);
        motdManager = new UHMOTDManager(this);

        wbintegration = new UHWorldBorderIntegration(this);
        spintegration = new UHSpectatorPlusIntegration(this);
        dynmapintegration = new UHDynmapIntegration(this);

        if (getConfig().getBoolean("titles.enabled")) {

            if (getServer().getPluginManager().isPluginEnabled("TitleApi")) {

                try {
                    titleMaker = ((TitleApi) getServer().getPluginManager().getPlugin("TitleApi")).getTitleApi(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Needed to avoid a NoClassDefFoundError.
        // I don't like this way of doing this, but else, the plugin will not load without ProtocolLib.
        protocollibintegrationwrapper = new UHProtocolLibIntegrationWrapper(this);

        commandManager = new UHPluginCommand(this);
        tabCompleter = new UHTabCompleter(this);

        getCommand("uh").setExecutor(commandManager);
        getCommand("t").setExecutor(commandManager);
        getCommand("g").setExecutor(commandManager);
        getCommand("togglechat").setExecutor(commandManager);
        getCommand("join").setExecutor(commandManager);
        getCommand("leave").setExecutor(commandManager);

        getCommand("uh").setTabCompleter(tabCompleter);
        getCommand("togglechat").setTabCompleter(tabCompleter);
        getCommand("join").setTabCompleter(tabCompleter);

        getServer().getPluginManager().registerEvents(new UHGameListener(this), this);
        getServer().getPluginManager().registerEvents(new UHGameplayListener(this), this);
        getServer().getPluginManager().registerEvents(new UHCraftingListener(this), this);
        // The freezer listener is registered by the freezer when it is needed.

        recipeManager.registerRecipes();
        gameManager.initEnvironment();

        motdManager.updateMOTDBeforeStart();

        // In case of reload
        for (Player player : getServer().getOnlinePlayers()) {
            gameManager.initPlayer(player);
        }

        // Imports spawnpoints from the config.
        this.spawnsManager.importSpawnPointsFromConfig();

        // Imports teams from the config.
        this.teamManager.importTeamsFromConfig();

        // Starts the task that updates the timers.
        // Started here, so a timer can be displayed before the start of the game
        // (example: countdown before the start).
        new UpdateTimerTask(this).runTaskTimer(this, 20l, 20l);

        // Schedule commands
        runtimeCommandsExecutor.registerCommandsInScheduler(UHRuntimeCommandsExecutor.AFTER_SERVER_START);

        getLogger().info(i18n.t("load.loaded"));
    }

    /**
     * Get Title Maker.
     *
     * @return the title maker
     */
    public TitleMaker getTitleMaker() {
        return titleMaker;
    }

    /**
     * Returns the team manager.
     *
     * @return
     */
    public UHTeamManager getTeamManager() {
        return teamManager;
    }

    /**
     * Returns the game manager.
     *
     * @return
     */
    public UHGameManager getGameManager() {
        return gameManager;
    }

    /**
     * Returns the scoreboard manager.
     *
     * @return
     */
    public UHScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Returns the MOTD manager.
     *
     * @return
     */
    public UHMOTDManager getMOTDManager() {
        return motdManager;
    }

    /**
     * Returns the spawns points manager.
     *
     * @return
     */
    public UHSpawnsManager getSpawnsManager() {
        return spawnsManager;
    }

    /**
     * Returns the command manager.
     *
     * @return
     */
    public UHPluginCommand getCommandManager() {
        return commandManager;
    }

    /**
     * Returns the border manager.
     *
     * @return
     */
    public UHBorderManager getBorderManager() {
        return borderManager;
    }

    /**
     * Returns the recipe manager.
     *
     * @return
     */
    public UHRecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * Returns the team-chat manager.
     *
     * @return
     */
    public UHTeamChatManager getTeamChatManager() {
        return teamChatManager;
    }

    /**
     * Returns the timer manager.
     *
     * @return
     */
    public UHTimerManager getTimerManager() {
        return timerManager;
    }

    /**
     * Returns the manager used to manage the commands executed after the start/the end of the
     * game (or any other moment using the generic API).
     *
     * @return
     */
    public UHRuntimeCommandsExecutor getRuntimeCommandsExecutor() {
        return runtimeCommandsExecutor;
    }

    /**
     * Returns the freezer.
     *
     * @return
     */
    public UHFreezer getFreezer() {
        return freezer;
    }

    /**
     * Returns the ProTips sender.
     *
     * @return
     */
    public UHProTipsSender getProtipsSender() {
        return protipsSender;
    }

    /**
     * Returns the representation of the WorldBorder integration in the plugin.
     *
     * @return
     */
    public UHWorldBorderIntegration getWorldBorderIntegration() {
        return wbintegration;
    }

    /**
     * Returns the representation of the SpectatorPlus integration in the plugin.
     *
     * @return
     */
    public UHSpectatorPlusIntegration getSpectatorPlusIntegration() {
        return spintegration;
    }

    /**
     * Returns the representation of the dynmap integration in the plugin.
     *
     * @return
     */
    public UHDynmapIntegration getDynmapIntegration() {
        return dynmapintegration;
    }

    /**
     * Returns a wrapper of the representation of the ProtocolLib integration in the plugin.
     *
     * @return
     */
    public UHProtocolLibIntegrationWrapper getProtocolLibIntegrationWrapper() {
        return protocollibintegrationwrapper;
    }

    /**
     * Returns the internationalization manager.
     *
     * @return
     */
    public I18n getI18n() {
        return i18n;
    }
}
