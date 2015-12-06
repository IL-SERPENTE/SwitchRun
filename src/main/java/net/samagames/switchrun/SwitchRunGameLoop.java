package net.samagames.switchrun;

import net.samagames.api.SamaGamesAPI;
import net.samagames.survivalapi.game.SurvivalGame;
import net.samagames.survivalapi.game.SurvivalGameLoop;
import net.samagames.survivalapi.game.SurvivalPlayer;
import net.samagames.survivalapi.game.SurvivalTeam;
import net.samagames.survivalapi.game.types.SurvivalTeamGame;
import net.samagames.survivalapi.game.types.run.RunBasedGameLoop;
import net.samagames.survivalapi.utils.TimedEvent;
import net.samagames.tools.GameUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

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
        this.nextEvent = new TimedEvent(1, 0, "Dégats actifs", ChatColor.GREEN, false, () ->
        {
            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage("Les dégats sont désormais actifs.", true);
            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage("Les équipes seront mélangées dans 14 minutes.", true);
            this.game.enableDamages();

            this.createRollEvent();
        });
    }

    public void createRollEvent()
    {
        this.nextEvent = new TimedEvent(14, 0, "Mélange des équipes", ChatColor.YELLOW, true, () ->
        {
            SamaGamesAPI.get().getGameManager().setMaxReconnectTime(-1);

            this.rollTeams();
            this.createTeleportationEvent();
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
            if (this.random.nextInt(100) > 35)
            {
                ArrayList<UUID> players = new ArrayList<>();

                for (UUID teamMember : team.getPlayersUUID().keySet())
                    if (!team.getPlayersUUID().get(teamMember))
                        players.add(teamMember);

                if (players.isEmpty())
                    continue;

                Collections.shuffle(players, this.random);

                toMove.add(players.get(0));

                if (((SurvivalTeamGame) this.game).getPersonsPerTeam() > 3 && players.size() > 1)
                    toMove.add(players.get(1));
            }
        }

        Collections.shuffle(toMove, this.random);

        if (toMove.size() % 2 != 0)
            toMove.remove(0);

        GameUtils.broadcastSound(Sound.ENDERMAN_TELEPORT);

        while (!toMove.isEmpty())
        {
            UUID one = toMove.get(0);
            Player onePlayer = Bukkit.getPlayer(one);

            UUID two = toMove.get(1);
            Player twoPlayer = Bukkit.getPlayer(two);

            SurvivalTeam oneTeam = ((SurvivalPlayer) this.game.getPlayer(one)).getTeam();
            SurvivalTeam twoTeam = ((SurvivalPlayer) this.game.getPlayer(two)).getTeam();

            oneTeam.remove(one, false);
            twoTeam.remove(two, false);

            oneTeam.join(two);
            twoTeam.join(one);

            toMove.remove(one);
            toMove.remove(two);

            Location oneLocation = onePlayer.getLocation();
            Location twoLocation = twoPlayer.getLocation();

            onePlayer.teleport(twoLocation);
            twoPlayer.teleport(oneLocation);

            onePlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10, 255));
            onePlayer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 10, 255));
            twoPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10, 255));
            twoPlayer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 10, 255));

            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage(ChatColor.YELLOW + "Le joueur " + oneTeam.getChatColor() + onePlayer.getName() + ChatColor.YELLOW + " a échangé sa place avec le joueur " + twoTeam.getChatColor() + twoPlayer.getName() + ChatColor.YELLOW + ".", true);

            Collections.shuffle(toMove, this.random);
        }
    }
}
