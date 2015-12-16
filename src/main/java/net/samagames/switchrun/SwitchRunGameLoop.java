package net.samagames.switchrun;

import net.samagames.api.SamaGamesAPI;
import net.samagames.survivalapi.game.SurvivalGame;
import net.samagames.survivalapi.game.SurvivalGameLoop;
import net.samagames.survivalapi.game.SurvivalTeam;
import net.samagames.survivalapi.game.types.SurvivalTeamGame;
import net.samagames.survivalapi.game.types.run.RunBasedGameLoop;
import net.samagames.survivalapi.utils.TimedEvent;
import net.samagames.tools.GameUtils;
import net.samagames.tools.ParticleEffect;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class SwitchRunGameLoop extends RunBasedGameLoop
{
    private Random random;

    public SwitchRunGameLoop(JavaPlugin plugin, Server server, SurvivalGame game)
    {
        super(plugin, server, game);

        try
        {
            this.random = SecureRandom.getInstanceStrong();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            this.random = new Random();
        }
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

            try
            {
                this.rollTeams();
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }

            this.createPreTeleportationEvent();
        });
    }

    public void createPreTeleportationEvent()
    {
        this.nextEvent = new TimedEvent(5, 0, "Duel", ChatColor.RED, true, () ->
        {
            this.game.disableDamages();

            ArrayList<SurvivalTeam> teams = new ArrayList<>(((SurvivalTeamGame) this.game).getTeams());
            ArrayList<Location> spawns = new ArrayList<>(this.game.getSpawns());

            while (!teams.isEmpty())
            {
                Location spawn = spawns.get(this.random.nextInt(spawns.size()));
                spawns.remove(spawn);

                spawn.setX(spawn.getX() / 2);
                spawn.setZ(spawn.getZ() / 2);

                ArrayList<UUID> players = teams.get(0).getPlayersUUID().keySet().stream().filter(player -> !teams.get(0).getPlayersUUID().get(player)).collect(Collectors.toCollection(ArrayList::new));

                teams.remove(0);

                if (!teams.isEmpty())
                {
                    players.addAll(teams.get(0).getPlayersUUID().keySet().stream().filter(player -> !teams.get(0).getPlayersUUID().get(player)).collect(Collectors.toList()));
                    teams.remove(0);
                }

                players.stream().filter(player -> Bukkit.getPlayer(player) != null).forEach(player -> Bukkit.getPlayer(player).teleport(spawn.add((double) this.random.nextInt(20) - 10, 0.0D, (double) this.random.nextInt(20) - 10)));
            }

            this.createDuelEvent();
        });
    }

    public void createDuelEvent()
    {
        this.nextEvent = new TimedEvent(0, 30, "PvP activé", ChatColor.RED, false, () ->
        {
            this.game.enableDamages();
            this.game.enablePVP();

            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage("C'est l'heure du du-du-du-duel !", true);

            this.createTeleportationEvent();
        });
    }

    @Override
    public void createTeleportationEvent()
    {
        this.nextEvent = new TimedEvent(3, 0, "Téléportation", ChatColor.YELLOW, false, () ->
        {
            this.game.disableDamages();
            this.game.disablePVP();

            super.createTeleportationEvent();
            
            this.nextEvent.run();
        });
    }

    private void rollTeams() throws NoSuchAlgorithmException
    {
        SurvivalTeamGame teamGame = (SurvivalTeamGame<SurvivalGameLoop>) this.game;
        ArrayList<UUID> toMove = new ArrayList<>();

        for (SurvivalTeam team : teamGame.getTeams())
        {
            ArrayList<UUID> players = new ArrayList<>();

            for(Map.Entry<UUID, Boolean> entry : team.getPlayersUUID().entrySet())
                if(!entry.getValue() && Bukkit.getPlayer(entry.getKey()) != null)
                    players.add(entry.getKey());

            if (players.isEmpty())
                continue;

            Collections.shuffle(players, this.random);

            toMove.add(players.get(0));

            if (teamGame.getPersonsPerTeam() > 3 && players.size() > 1 && this.random.nextInt(142) == 42)
            {
                Player player = Bukkit.getPlayer(players.get(1));

                if(player != null)
                    player.sendMessage(ChatColor.AQUA  + "" + ChatColor.BOLD + "Bravo! Tu es l'heureux gagnant d'un switch bonus! ");

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

            SurvivalTeam oneTeam = teamGame.getPlayerTeam(one);
            SurvivalTeam twoTeam = teamGame.getPlayerTeam(two);

            oneTeam.removePlayer(one);
            twoTeam.removePlayer(two);

            oneTeam.join(two);
            twoTeam.join(one);

            toMove.remove(one);
            toMove.remove(two);

            Location oneLocation = onePlayer.getLocation();
            Location twoLocation = twoPlayer.getLocation();

            oneLocation.getWorld().strikeLightningEffect(oneLocation);
            twoLocation.getWorld().strikeLightningEffect(twoLocation);

            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
            {
                effect(twoLocation);
                effect(oneLocation);
            });

            onePlayer.teleport(twoLocation);
            twoPlayer.teleport(oneLocation);

            onePlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 255));
            onePlayer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 10, 255));
            twoPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 255));
            twoPlayer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 10, 255));

            this.game.getCoherenceMachine().getMessageManager().writeCustomMessage(ChatColor.YELLOW + "Le joueur " + oneTeam.getChatColor() + onePlayer.getName() + ChatColor.YELLOW + " a échangé sa place avec le joueur " + twoTeam.getChatColor() + twoPlayer.getName() + ChatColor.YELLOW + ".", true);
        }
    }

    private void effect(Location loc)
    {
        for(double y = loc.getY(); y <= loc.getY()+1.7; y += 0.4)
        {
            for(double circle = 0; circle <= Math.PI * 2; circle += 0.6)
            {
                double sin = Math.sin(circle);
                double cos = Math.cos(circle);
                double x = loc.getX() + sin;
                double z = loc.getZ() + cos;

                ParticleEffect.PORTAL.display(new Vector(-sin, 0, -cos), 0.5F, new Location(loc.getWorld(), x, y, z), 50);
            }
        }

    }
}
