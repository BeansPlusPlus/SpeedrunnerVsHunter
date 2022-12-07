package speedhunt.code;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommandExecutor implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      if (args.length < 2) {
        showConfiguration(sender);
      } else if (args[0].equalsIgnoreCase("runners")) {
        String argsJoined = Arrays.stream(args).skip(1).collect(Collectors.joining(" ")).replace(",", " ");
        List<String> runners = Arrays.asList(argsJoined.split("\\s+"));
        GameConfiguration.getInstance().setRunners(runners);

        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "runners" + ChatColor.WHITE + " set to " + ChatColor.RED +
              GameConfiguration.getInstance().getRunners().stream().collect(Collectors.joining(", ")));
        }
      } else if (args[0].equalsIgnoreCase("border")) {
        int value = Integer.parseInt(args[1]);
        GameConfiguration.getInstance().setWorldBorder(value);

        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "border" + ChatColor.WHITE + " set to " + ChatColor.RED + value);
        }
      } else if (args[0].equalsIgnoreCase("headstart")) {
        int value = Integer.parseInt(args[1]);
        GameConfiguration.getInstance().setHeadStart(value);

        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "headstart" + ChatColor.WHITE + " set to " + ChatColor.RED + value);
        }
      } else {
        showConfiguration(sender);
      }
    } catch(NumberFormatException e) {
      sender.sendMessage(ChatColor.DARK_RED + "Invalid number entered");
    }

    return true;
  }

  private void showConfiguration(CommandSender sender) {
    sender.sendMessage("[Current Game Configuration]");
    sender.sendMessage(ChatColor.YELLOW + "runners: " + ChatColor.RED +
        GameConfiguration.getInstance().getRunners().stream().collect(Collectors.joining(", ")));
    sender.sendMessage(ChatColor.YELLOW + "border: " + ChatColor.RED + GameConfiguration.getInstance().getWorldBorder());
    sender.sendMessage(ChatColor.YELLOW + "headstart: " + ChatColor.RED + GameConfiguration.getInstance().getHeadStart());
  }
}
