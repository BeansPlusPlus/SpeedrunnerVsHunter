package speedhunt.code;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SpeedrunVsHunterPlugin extends JavaPlugin implements CommandExecutor, Listener {
  private Game game;
  private int taskId;

  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    getCommand("start").setExecutor(this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (label.equalsIgnoreCase("start")) {
      if (game != null) {
        // cancel previous game
        HandlerList.unregisterAll(game);
        getServer().getScheduler().cancelTask(taskId);
      }
      game = createGame(args);
    }

    return false;
  }

  private Game createGame(String[] args) {
    List<String> runners = new ArrayList<>();

    int headStart = 30;
    int worldBorder = 5000;

    for (String arg : args) {
      if (arg.startsWith("headstart=")) {
        headStart = Integer.parseInt(arg.split("=")[1]);
      } else if (arg.startsWith("border=")) {
        worldBorder = Integer.parseInt(arg.split("=")[1]);
      } else {
        runners.add(arg.toLowerCase());
      }
    }

    return new Game(this, runners, headStart, worldBorder);
  }

  public void setTask(int taskId) {
    this.taskId = taskId;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    Player player = e.getPlayer();

    player.sendMessage(ChatColor.WHITE + "Start game:");
    player.sendMessage(ChatColor.RED + "/start [runners] [settings]");
    player.sendMessage(ChatColor.RED + "Settings: headstart, border");

    player.sendMessage(ChatColor.WHITE + "Runners should be space separated. Setting key and value separated with =");
  }
}
