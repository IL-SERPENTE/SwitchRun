package net.samagames.switchrun;

import com.google.gson.JsonPrimitive;
import net.samagames.api.SamaGamesAPI;
import net.samagames.survivalapi.game.types.run.RunBasedTeamGame;
import org.bukkit.plugin.java.JavaPlugin;

public class SwitchRun extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        int nb = SamaGamesAPI.get().getGameManager().getGameProperties().getOption("playersPerTeam", new JsonPrimitive(2)).getAsInt();

        SamaGamesAPI.get().getGameManager().setMaxReconnectTime(20);
        SamaGamesAPI.get().getGameManager().registerGame(new RunBasedTeamGame<SwitchRunGameLoop>(this, "switchrun", "SwitchRun", "L’expérience d’un UHC en 30 minutes", "≈", SwitchRunGameLoop.class, nb));
    }
}
