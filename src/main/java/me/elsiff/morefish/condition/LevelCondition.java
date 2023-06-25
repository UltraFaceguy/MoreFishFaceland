package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LevelCondition implements Condition {

  private final int level;

  public LevelCondition(int level) {
    this.level = level;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    return (player.getLevel() >= level);
  }

  @Override
  public String getDescription() {
    return "Requires Lv" + level + "+";
  }
}
