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

package me.azenet.UHPlugin;

import me.azenet.UHPlugin.i18n.I18n;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sends a ProTip to a player.
 * <p/>
 * All ProTips are sent only once.
 * <p/>
 * The name of a protip (the value of the static attribute representing it) is:
 * - an identifier;
 * - the name of the key in the translation files (protips.{name});
 * - the name of the key to disable it in the config file (protips.{name} too).
 *
 * @author Amaury Carrade
 */
public class UHProTipsSender {

    UHPlugin p = null;
    I18n i = null;

    Map<String, ArrayList<UUID>> protipsGiven = new HashMap<String, ArrayList<UUID>>();

    UHSound proTipsSound = null;

    public static final String PROTIP_LOCK_CHAT = "teamchat.lock";
    public static final String PROTIP_USE_G_COMMAND = "teamchat.useGCommand";
    public static final String PROTIP_USE_T_COMMAND = "teamchat.useTCommand";

    public static final String PROTIP_CRAFT_GOLDEN_HEAD = "crafts.goldenHead";
    public static final String PROTIP_CRAFT_COMPASS_EASY = "crafts.compassEasy";
    public static final String PROTIP_CRAFT_COMPASS_MEDIUM = "crafts.compassMedium";
    public static final String PROTIP_CRAFT_COMPASS_HARD = "crafts.compassHard";
    public static final String PROTIP_CRAFT_GLISTERING_MELON = "crafts.glisteringMelon";
    public static final String PROTIP_CRAFT_NO_ENCHANTED_GOLDEN_APPLE = "crafts.noEnchGoldenApple";

    public static final String PROTIP_STARTUP_INVINCIBILITY = "start.invincibility";

    public UHProTipsSender(UHPlugin p) {
        this.p = p;
        this.i = p.getI18n();

        // Initialization of the "protips" map
        protipsGiven.put(PROTIP_LOCK_CHAT, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_USE_G_COMMAND, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_USE_T_COMMAND, new ArrayList<UUID>());

        protipsGiven.put(PROTIP_CRAFT_GOLDEN_HEAD, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_CRAFT_COMPASS_EASY, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_CRAFT_COMPASS_MEDIUM, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_CRAFT_COMPASS_HARD, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_CRAFT_GLISTERING_MELON, new ArrayList<UUID>());
        protipsGiven.put(PROTIP_CRAFT_NO_ENCHANTED_GOLDEN_APPLE, new ArrayList<UUID>());

        protipsGiven.put(PROTIP_STARTUP_INVINCIBILITY, new ArrayList<UUID>());

        // Sound
        proTipsSound = new UHSound(p.getConfig().getConfigurationSection("protips.sound"));
    }

    /**
     * Sends a ProTip to a player.
     * A ProTip is only given one time to a given player.
     *
     * @param player The player
     * @param protip The ProTip to send to this player.
     * @return true if the ProTip was sent, false else (already sent or disabled by the config).
     * @throws IllegalArgumentException if the ProTip is not registered.
     */
    public boolean sendProtip(Player player, String protip) {

        if (!protipsGiven.containsKey(protip)) {
            throw new IllegalArgumentException("Unknown ProTip");
        }

        if (!isProtipEnabled(protip)) {
            return false;
        }

        if (wasProtipSent(player, protip)) {
            return false;
        }

        protipsGiven.get(protip).add(player.getUniqueId());

        player.sendMessage(i.t("protips.base") + " " + ChatColor.RESET + i.t("protips." + protip));
        proTipsSound.play(player);

        return false;
    }

    /**
     * Checks if a ProTip was already sent to a player.
     *
     * @param player The player.
     * @param protip The ProTip.
     * @return true if the ProTip was sent; false else.
     * @throws IllegalArgumentException if the ProTip is not registered.
     */
    public boolean wasProtipSent(Player player, String protip) {
        if (!protipsGiven.containsKey(protip)) {
            throw new IllegalArgumentException("Unknown ProTip");
        }

        return protipsGiven.get(protip).contains(player.getUniqueId());
    }

    /**
     * Checks if the given ProTip is enabled in the config.
     *
     * @param protip The ProTip to check.
     * @return true if the ProTip is enabled.
     */
    public boolean isProtipEnabled(String protip) {
        return p.getConfig().getBoolean("protips." + protip);
    }
}
