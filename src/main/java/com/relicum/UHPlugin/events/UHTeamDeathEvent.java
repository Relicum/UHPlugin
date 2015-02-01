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

package com.relicum.UHPlugin.events;

import com.relicum.UHPlugin.UHTeam;

/**
 * Event fired when the last member of a team die.
 */
public class UHTeamDeathEvent extends UHEvent {

    private UHTeam team;

    public UHTeamDeathEvent(UHTeam team) {
        this.team = team;
    }

    /**
     * Returns the now-dead team.
     *
     * @return The team.
     */
    public UHTeam getTeam() {
        return team;
    }
}
