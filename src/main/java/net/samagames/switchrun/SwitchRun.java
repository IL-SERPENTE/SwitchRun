package net.samagames.switchrun;

import com.google.gson.JsonPrimitive;
import net.samagames.api.SamaGamesAPI;
import net.samagames.survivalapi.game.types.run.RunBasedTeamGame;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * This file is part of SwitchRun.
 *
 * SwitchRun is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SwitchRun is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwitchRun.  If not, see <http://www.gnu.org/licenses/>.
 */
public class SwitchRun extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        int nb = SamaGamesAPI.get().getGameManager().getGameProperties().getOption("playersPerTeam", new JsonPrimitive(2)).getAsInt();

        SamaGamesAPI.get().getGameManager().registerGame(new RunBasedTeamGame<SwitchRunGameLoop>(this, "switchrun", "SwitchRun", "30 minutes pour surpasser les Patricks !", "≈", SwitchRunGameLoop.class, nb));
        SamaGamesAPI.get().getGameManager().setMaxReconnectTime(15);
    }
}
