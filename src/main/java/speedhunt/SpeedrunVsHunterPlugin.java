package speedhunt;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

import beansplusplus.gameconfig.GameConfiguration;
import beansplusplus.gameconfig.ConfigLoader;

public class SpeedrunVsHunterPlugin extends JavaPlugin implements CommandExecutor, Listener {
  private Game game;

  public void onEnable() {
    ConfigLoader.loadFromInput(getResource("config.yml"));
    getServer().getPluginManager().registerEvents(this, this);
    getCommand("start").setExecutor(this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    List<String> players = GameConfiguration.getConfig().getValue("runners");
    if (players.size() == 0) {
      sender.sendMessage(ChatColor.RED + "At least 1 runner required to start.");
      sender.sendMessage(ChatColor.WHITE + "/config runners <runners>");

      return false;
    }

    if (game != null) {
      game.end();
    }
    game = new Game(this);
    game.start();

    return true;
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
