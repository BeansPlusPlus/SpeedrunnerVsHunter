package speedhunt;

import beansplusplus.beansgameplugin.Game;
import beansplusplus.beansgameplugin.GameConfiguration;
import beansplusplus.beansgameplugin.GameState;
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
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

public class SpeedrunnerVsHunterGame implements Listener, Game {
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

  private int headStart;
  private double bedDamageMultiplier;
  private boolean cancelHunterMovement;
  private boolean autoCompass;
  private boolean netherCompass;
  private GameState state;
  private List<String> runners;

  private Map<String, Team> assignedPlayers = new HashMap<>();

  private Plugin plugin;

  public SpeedrunnerVsHunterGame(SpeedrunVsHunterPlugin plugin, GameConfiguration config, GameState state) {
    this.plugin = plugin;
    this.state = state;

    headStart = (int) ((double) config.getValue("headstart_minutes") * 60.0);

    String bedDamage = config.getValue("bed_explosion_damage");
    if (bedDamage.equals("enabled")) {
      bedDamageMultiplier = 1;
    } else if (bedDamage.equals("nerfed")) {
      bedDamageMultiplier = 0.33;
    } else {
      bedDamageMultiplier = 0;
    }

    autoCompass = config.getValue("auto_compass");
    cancelHunterMovement = headStart > 0;
    runners = config.getValue("runners");
    netherCompass = config.getValue("nether_compass");
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


      if (runners.contains(player.getName())) {
        addToTeam(player.getName(), Team.RUNNER);
      } else {
        addToTeam(player.getName(), Team.HUNTER);
      }
    }

    World world = Bukkit.getWorld("world");
    world.setTime(1000);

    showAllChatMessage(ChatColor.BLUE + "Hunters are not allowed to move for " + headStart + " seconds.");

    if (cancelHunterMovement) {
      Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
        showAllChatMessage(ChatColor.BLUE + "Hunters can move now");
        cancelHunterMovement = false;
      }, 20 * headStart);
    }

    if (autoCompass) {
      Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::centreAllCompasses, 20, 20);
    }

    Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

    refreshScoreboard();
  }

  @Override
  public void cleanUp() {
    HandlerList.unregisterAll(this);
    Bukkit.getServer().getScheduler().cancelTasks(plugin);
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

  private void centreAllCompasses() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (getPlayerTeam(player.getName()) != Team.HUNTER) continue;

      for (ItemStack item : player.getInventory().getContents()) {
        if (item != null && item.getType() == Material.COMPASS) centreCompass(player, item);
      }
    }
  }

  private void centreCompass(Player player, ItemStack item) {
    Player runner = nearestRunner(player.getLocation());

    if (runner == null) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + "No-one to track..."));
    } else {
      if (netherCompass) {
        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
        compassMeta.setLodestoneTracked(false);
        compassMeta.setLodestone(runner.getLocation());
        item.setItemMeta(compassMeta);
      } else {
        player.setCompassTarget(runner.getLocation());
      }

      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "Tracking: " + runner.getName()));
    }
  }

  private Player nearestRunner(Location hunterLocation) {
    Player nearestRunner = null;
    double distance = Integer.MAX_VALUE;

    for (Player runner : getOnlineForTeam(Team.RUNNER)) {
      Location runnerLocation = runner.getLocation();

      if (!hunterLocation.getWorld().equals(runnerLocation.getWorld())) continue;

      double newDistance = runnerLocation.distance(hunterLocation);

      if (newDistance < distance) {
        distance = newDistance;
        nearestRunner = runner;
      }
    }

    return nearestRunner;
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
        state.stopGame();
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

      state.stopGame();
    }
  }

  @EventHandler
  public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent e) {
    if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      e.setDamage(e.getDamage() * bedDamageMultiplier);
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
        hunter.getInventory().getItemInMainHand() == null ||
        hunter.getInventory().getItemInMainHand().getType() != Material.COMPASS ||
        (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) {
      return;
    }

    ItemStack itemStack = hunter.getInventory().getItemInMainHand();

    centreCompass(hunter, itemStack);
  }
}
