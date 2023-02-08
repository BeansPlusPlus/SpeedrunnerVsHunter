package speedhunt;

import beansplusplus.beansgameplugin.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.List;

public class SpeedrunVsHunterPlugin extends JavaPlugin implements GameCreator {
  public void onEnable() {
    BeansGamePlugin beansGamePlugin = (BeansGamePlugin) getServer().getPluginManager().getPlugin("BeansGamePlugin");
    beansGamePlugin.registerGame(this);
  }

  @Override
  public Game createGame(GameConfiguration config, GameState gameState) {
    return new SpeedrunnerVsHunterGame(this, config, gameState);
  }

  @Override
  public boolean isValidSetup(CommandSender sender, GameConfiguration config) {
    List<String> players = config.getValue("runners");
    if (players.size() == 0) {
      sender.sendMessage(ChatColor.RED + "At least 1 runner required to start.");
      sender.sendMessage(ChatColor.WHITE + "/config runners <runners>");

      return false;
    }

    return true;
  }

  @Override
  public InputStream config() {
    return getResource("config.yml");
  }

  @Override
  public List<String> rulePages() {
    return List.of(
        "This game has two teams: Hunters and speedrunners\n\nThe goal of the speedrunners is to kill the ender dragon as fast as possible.\n\nThe goal of the hunters is to kill all other speedrunners.",
        "Hunters get a compass that can track the nearest runner in the same world.\n\nIf " + ChatColor.RED + "auto_compass" + ChatColor.BLACK + " is off, the compass needs to be right clicked to update.",
        "If a speedrunner dies, they can find and right click another surviving team mate to rejoin the team.",
        "Use " + ChatColor.RED + "/config" + ChatColor.BLACK + " to choose teams.\n\nYou can also use it to change the head-start time, nerf bed explosions, disable nether tracking and enable auto compass updating.",
        "In this game, please do not use any glitches that would not be allowed on a glitchless minecraft speedrun, or any glitches that can soft-lock the game. (For example: destroying all the end portals)",
        "Refer to server rules in the lobby for general rules that apply to all gamemodes. Feel free to add your own unwritten rules to this game to make things more interesting!"
    );
  }

  @Override
  public String name() {
    return "Speedrunner vs Hunter";
  }
}
