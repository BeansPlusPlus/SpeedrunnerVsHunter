package speedhunt.code;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SpeedrunVsHunterPlugin extends JavaPlugin implements CommandExecutor {
  private Game game;
  private int taskId;

  public void onEnable() {
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
}
