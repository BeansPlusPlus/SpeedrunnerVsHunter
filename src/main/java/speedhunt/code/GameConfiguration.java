package speedhunt.code;

import java.util.ArrayList;
import java.util.List;

public class GameConfiguration {
  private static final GameConfiguration config = new GameConfiguration();

  public static GameConfiguration getInstance() {
    return config;
  }

  private GameConfiguration() {
  }

  private List<String> runners = new ArrayList<>();
  private int worldBorder = 5000;
  private int headStart = 30;

  public List<String> getRunners() {
    return runners;
  }

  public void setRunners(List<String> runners) {
    this.runners = runners;
  }

  public int getWorldBorder() {
    return worldBorder;
  }

  public void setWorldBorder(int worldBorder) {
    this.worldBorder = worldBorder;
  }

  public int getHeadStart() {
    return headStart;
  }

  public void setHeadStart(int headStart) {
    this.headStart = headStart;
  }
}
