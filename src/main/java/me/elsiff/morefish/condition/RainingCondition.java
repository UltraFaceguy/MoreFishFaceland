package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RainingCondition implements Condition {

  private final boolean raining;

  public RainingCondition(boolean raining) {
    this.raining = raining;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    return (raining == player.getWorld().hasStorm());
  }

  @Override
  public String getDescription() {
    return raining ? "Raining" : "Clear skies";
  }
}
