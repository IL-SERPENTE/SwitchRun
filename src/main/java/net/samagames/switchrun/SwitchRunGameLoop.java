package net.samagames.switchrun;

import net.samagames.survivalapi.game.SurvivalGame;
import net.samagames.survivalapi.game.SurvivalGameLoop;
import net.samagames.survivalapi.game.SurvivalPlayer;
import net.samagames.survivalapi.game.SurvivalTeam;
import net.samagames.survivalapi.game.types.SurvivalTeamGame;
import net.samagames.survivalapi.game.types.run.RunBasedGameLoop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class SwitchRunGameLoop extends RunBasedGameLoop
{
    private final Random random;

    public SwitchRunGameLoop(JavaPlugin plugin, Server server, SurvivalGame game)
    {
        super(plugin, server, game);

        this.random = new Random();
    }

    @Override
    public void createDamageEvent()
    {
        this.nextEvent = new TimedEvent(1, 0, "Dégats actifs", ChatColor.GREEN, () ->
        {
            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage("Les dégats sont désormais actifs.", true);
            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage("Les équipes seront mélangées dans 14 minutes.", true);
            this.game.enableDamages();

            this.createRollEvent(14, () -> this.createRollEvent(5, this::createTeleportationEvent));
        });
    }

    public void createRollEvent(int time, Runnable callback)
    {
        this.nextEvent = new TimedEvent(time, 0, "Mélange des équipes", ChatColor.YELLOW, () ->
        {
            this.rollTeams();
            callback.run();
        });
    }

    public void createTeleportationEvent()
    {
        super.createTeleportationEvent();
        this.nextEvent = this.nextEvent.copy(5, 0);
    }

    private void rollTeams()
    {
        SurvivalTeamGame teamGame = ((SurvivalTeamGame<SurvivalGameLoop>) this.game);
        ArrayList<UUID> toMove = new ArrayList<>();

        for (SurvivalTeam team : teamGame.getTeams())
        {
            if (this.random.nextInt(100) > 50)
            {
                ArrayList<UUID> players = team.getPlayersUUID().keySet().stream().filter(teamMember -> team.getPlayersUUID().get(teamMember)).collect(Collectors.toCollection(ArrayList::new));
                Collections.shuffle(players, this.random);

                if (players.isEmpty() || players.size() == 1)
                    continue;

                toMove.add(players.get(0));
            }
        }

        Collections.shuffle(toMove, this.random);

        if (toMove.size() % 2 != 0)
            toMove.remove(0);

        while (!toMove.isEmpty())
        {
            UUID one = toMove.get(0);
            UUID two = toMove.get(1);

            SurvivalTeam oneTeam = ((SurvivalPlayer) this.game.getPlayer(one)).getTeam();
            SurvivalTeam twoTeam = ((SurvivalPlayer) this.game.getPlayer(two)).getTeam();

            oneTeam.remove(one, false);
            twoTeam.remove(two, false);

            oneTeam.join(two);
            twoTeam.join(one);

            toMove.remove(one);
            toMove.remove(two);

            Location oneLocation = Bukkit.getPlayer(one).getLocation();
            Location twoLocation = Bukkit.getPlayer(two).getLocation();

            Bukkit.getPlayer(one).teleport(twoLocation);
            Bukkit.getPlayer(two).teleport(oneLocation);

            Collections.shuffle(toMove, this.random);
        }
    }
}
