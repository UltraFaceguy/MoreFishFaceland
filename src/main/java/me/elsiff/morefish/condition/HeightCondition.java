package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HeightCondition implements Condition {

  private final int minHeight;
  private final int maxHeight;

  public HeightCondition(int minHeight, int maxHeight) {
    this.minHeight = minHeight;
    this.maxHeight = maxHeight;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    int y = fishLocation.getBlockY();
    return (minHeight <= y) && (y <= maxHeight);
  }
}