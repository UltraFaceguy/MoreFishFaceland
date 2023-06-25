package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TimeCondition implements Condition {

  private final String time;

  public TimeCondition(String time) {
    this.time = time;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    long tick = player.getWorld().getTime();
    return switch (time) {
      case "day" -> (1000 <= tick && tick < 13000);
      case "night" -> (13000 <= tick || tick < 1000);
      default -> false;
    };
  }

  @Override
  public String getDescription() {
    return switch (time) {
      case "day" -> "During the day";
      case "night" -> "Night only";
      default -> "";
    };
  }
}
