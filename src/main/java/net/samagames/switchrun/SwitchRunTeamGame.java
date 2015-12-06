package net.samagames.switchrun;

import net.samagames.survivalapi.game.SurvivalTeam;
import net.samagames.survivalapi.game.types.run.RunBasedTeamGame;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class SwitchRunTeamGame extends RunBasedTeamGame<SwitchRunGameLoop>
{
    public SwitchRunTeamGame(JavaPlugin plugin, int personsPerTeam)
    {
        super(plugin, "switchrun", "SwitchRun", "30 minutes pour surpasser les Patricks !", "≈", SwitchRunGameLoop.class, personsPerTeam);
    }

    @Override
    public void teleportDeathMatch()
    {
        Iterator<Location> locationIterator = this.spawns.iterator();
        ArrayList<SurvivalTeam> teams = new ArrayList<>(this.teams);

        while (!teams.isEmpty())
        {
            ArrayList<SurvivalTeam> selectedTeams = new ArrayList<>();
            Location spawn = locationIterator.next();

            selectedTeams.add(teams.get(0));
            teams.remove(0);

            if (!teams.isEmpty())
            {
                selectedTeams.add(teams.get(0));
                teams.remove(0);
            }

            for (SurvivalTeam selectedTeam : selectedTeams)
            {
                for (UUID playerUUID : selectedTeam.getPlayersUUID().keySet())
                {
                    Player player = this.server.getPlayer(playerUUID);

                    if (player == null)
                    {
                        this.gamePlayers.remove(playerUUID);
                    }
                    else
                    {
                        this.removeEffects(player);
                        player.teleport(new Location(spawn.getWorld(), spawn.getX() * 4 / 10, 150.0, spawn.getZ() * 4 / 10));
                    }
                }
            }
        }
    }
}
