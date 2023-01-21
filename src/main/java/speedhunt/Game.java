package speedhunt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import beansplusplus.gameconfig.ConfigLoader;
import beansplusplus.gameconfig.GameConfiguration;

import java.util.*;
import java.util.stream.Collectors;


public class Game implements Listener {
  /**
   * Teams
   */
  private enum Team {
    RUNNER(ChatColor.AQUA), HUNTER(ChatColor.GOLD);

    private ChatColor colour;

    Team(ChatColor colour) {
      this.colour = colour;
    }

    public ChatColor getColour() {
      return colour;
    }

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  private boolean cancelHunterMovement;
  private int taskId;

  private Map<String, Team> assignedPlayers = new HashMap<>();

  private Plugin plugin;

  public Game(SpeedrunVsHunterPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Start game
   */
  public void start() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setHealth(20);
      player.setLevel(0);
      player.setFoodLevel(20);
      player.getInventory().clear();
      player.setGameMode(GameMode.SURVIVAL);

      List<String> runners = GameConfiguration.getConfig().getValue("runners");

      if (runners.contains(player.getName())) {
        addToTeam(player.getName(), Team.RUNNER);
      } else {
        addToTeam(player.getName(), Team.HUNTER);
      }
    }

    World world = Bukkit.getWorld("world");
    world.setTime(1000);

    int headStart = GameConfiguration.getConfig().getValue("headstart");

    cancelHunterMovement = headStart > 0;

    showAllChatMessage(ChatColor.BLUE + "Hunters are not allowed to move for " + headStart + " seconds.");

    if (cancelHunterMovement) {
      taskId = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
        showAllChatMessage(ChatColor.BLUE + "Hunters can move now");
        cancelHunterMovement = false;
      }, 20 * headStart);
    }

    Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

    refreshScoreboard();
  }

  /**
   * End game
   */
  public void end() {
    HandlerList.unregisterAll(this);
    Bukkit.getServer().getScheduler().cancelTask(taskId);

    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setGameMode(GameMode.SPECTATOR);
    }
  }

  /**
   * Get a set of all online players in a given team
   *
   * @param team
   * @return
   */
  private Set<Player> getOnlineForTeam(Team team) {
    return Bukkit.getOnlinePlayers()
        .stream()
        .filter((player) -> getPlayerTeam(player.getName()) == team)
        .collect(Collectors.toSet());
  }


  private void removePlayer(String playerName) {
    assignedPlayers.remove(playerName);
  }


  /**
   * Get the team a player is on
   *
   * @param playerName
   * @return
   */
  private Team getPlayerTeam(String playerName) {
    if (!assignedPlayers.containsKey(playerName)) return null;
    return assignedPlayers.get(playerName);
  }

  /**
   * Get online number of players for a team
   *
   * @param team
   * @return
   */
  private int getTeamSize(Team team) {
    return getOnlineForTeam(team).size();
  }

  /**
   * Add a player into a team
   *
   * @param playerName
   * @param team
   */
  private void addToTeam(String playerName, Team team) {
    if (assignedPlayers.containsKey(playerName)) return;

    Player player = Bukkit.getPlayer(playerName);

    if (player == null) return;

    assignedPlayers.put(player.getName(), team);

    showAllChatMessage(team.getColour() + player.getName() + " is a " + team);

    if (team == Team.HUNTER) {
      player.getInventory().addItem(new ItemStack(Material.COMPASS));
    }
  }

  public Scoreboard getScoreboard() {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    Objective obj = scoreboard.registerNewObjective("hvr", "hvr", "Hunter Vs Speedrunner");
    obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);

    org.bukkit.scoreboard.Team hunterTeam = scoreboard.registerNewTeam(Team.HUNTER.getColour() + Team.HUNTER.toString());
    org.bukkit.scoreboard.Team runnerTeam = scoreboard.registerNewTeam(Team.RUNNER.getColour() + Team.RUNNER.toString());

    for (Player player : getOnlineForTeam(Team.HUNTER)) {
      hunterTeam.addEntry(player.getName());
      obj.getScore(player.getName()).setScore(1);
    }
    for (Player player : getOnlineForTeam(Team.RUNNER)) {
      runnerTeam.addEntry(player.getName());
      obj.getScore(player.getName()).setScore(1);
    }

    hunterTeam.setPrefix(Team.HUNTER.getColour() + "[HUNTER] ");
    runnerTeam.setPrefix(Team.RUNNER.getColour() + "[RUNNER] ");


    return scoreboard;

  }

  private void refreshScoreboard() {
    Scoreboard scoreboard = getScoreboard();

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.setScoreboard(scoreboard);
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent e) {
    String pName = e.getEntity().getName();
    if (getPlayerTeam(pName) == Team.RUNNER) {
      removePlayer(pName);
      showAllChatMessage(Team.RUNNER.getColour() + "A runner has died: " + pName);
      refreshScoreboard();
      if (getTeamSize(Team.RUNNER) == 0) {
        showAllChatMessage(Team.HUNTER.getColour() + "Hunters win!");

      } else {
        e.getEntity().sendMessage(ChatColor.BLUE + "Right click an alive runner to rejoin the running team.");
      }
    }
  }

  /**
   * Join someone's team by right clicking them
   *
   * @param e
   */
  @EventHandler
  public void onEntityInteract(PlayerInteractEntityEvent e) {
    Entity entity = e.getRightClicked();
    Player player = e.getPlayer();

    if (entity instanceof Player) {
      Player clickedPlayer = (Player) entity;

      Team yourTeam = getPlayerTeam(player.getName());
      Team theirTeam = getPlayerTeam(clickedPlayer.getName());

      if (yourTeam == null && theirTeam != null) {
        // join team
        addToTeam(player.getName(), theirTeam);
        refreshScoreboard();
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    if (cancelHunterMovement && getPlayerTeam(e.getPlayer().getName()) == Team.HUNTER) e.setCancelled(true);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e) {
    if (cancelHunterMovement && getPlayerTeam(e.getPlayer().getName()) == Team.HUNTER) {
      e.getTo().setX(e.getFrom().getX());
      e.getTo().setZ(e.getFrom().getZ());
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    refreshScoreboard();
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    refreshScoreboard();
  }

  /**
   * Runners win when the ender dragon is killed
   *
   * @param e
   */
  @EventHandler
  public void onEntityDeath(EntityDeathEvent e) {
    if (e.getEntity().getType() == EntityType.ENDER_DRAGON) {
      showAllChatMessage(Team.RUNNER.getColour() + "Runners win!");

      end();
    }
  }

  /**
   * Give hunters compass on respawn
   *
   * @param e
   */
  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent e) {
    Player player = e.getPlayer();

    if (getPlayerTeam(player.getName()) == Team.HUNTER) {
      player.getInventory().addItem(new ItemStack(Material.COMPASS));
    }
  }

  /**
   * Show a message to everyone in chat
   *
   * @param message
   */
  public void showAllChatMessage(String message) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(message);
    }
  }

  /**
   * Right click with a compass
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player hunter = e.getPlayer();

    if (getPlayerTeam(hunter.getName()) != Team.HUNTER ||
        hunter.getInventory().getItemInMainHand().getType() != Material.COMPASS ||
        (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) {
      return;
    }

    Location hunterLocation = hunter.getLocation();

    Location compassLocation = null;
    double distance = Integer.MAX_VALUE;
    String tracking = null;

    for (Player runner : getOnlineForTeam(Team.RUNNER)) {
      Location runnerLocation = runner.getLocation();

      if (!hunterLocation.getWorld().equals(runnerLocation.getWorld())) continue;

      double newDistance = runnerLocation.distance(hunterLocation);

      if (newDistance < distance) {
        distance = newDistance;
        compassLocation = runnerLocation;
        tracking = runner.getName();
      }
    }

    if (compassLocation == null) {
      hunter.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + "No-one to track..."));
    } else {
      hunter.setCompassTarget(compassLocation);
      hunter.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "Tracking: " + tracking));
    }
  }
}
