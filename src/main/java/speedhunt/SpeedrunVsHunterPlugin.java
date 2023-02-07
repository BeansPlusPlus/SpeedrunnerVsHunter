package speedhunt;

import beansplusplus.beansgameplugin.BeansGamePlugin;
import beansplusplus.beansgameplugin.GameConfiguration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SpeedrunVsHunterPlugin extends JavaPlugin implements CommandExecutor, Listener {
  private SpeedrunnerVsHunterGame game;

  public void onEnable() {
    BeansGamePlugin beansGamePlugin = (BeansGamePlugin) getServer().getPluginManager().getPlugin("BeansGamePlugin");
    beansGamePlugin.registerGame(getResource("config.yml"), (CommandSender sender, GameConfiguration config) -> {
      List<String> players = config.getValue("runners");
      if (players.size() == 0) {
        sender.sendMessage(ChatColor.RED + "At least 1 runner required to start.");
        sender.sendMessage(ChatColor.WHITE + "/config runners <runners>");

        return null;
      }

      return new SpeedrunnerVsHunterGame(this, config);
    });
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    Player player = e.getPlayer();

    player.sendMessage(ChatColor.BLUE + "Welcome to Speedrunner vs hunter!");
    player.sendMessage(ChatColor.WHITE + "Configure game settings with " + ChatColor.RED + "/config");
    player.sendMessage(ChatColor.WHITE + "You are required to configure who the runners are with " + ChatColor.RED + "/config runners <runners>");
    player.sendMessage(ChatColor.WHITE + "Start game with " + ChatColor.RED + "/start");
  }

}
