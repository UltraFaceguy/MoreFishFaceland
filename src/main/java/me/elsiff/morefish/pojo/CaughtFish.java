package me.elsiff.morefish.pojo;

import lombok.Data;
import org.bukkit.OfflinePlayer;

@Data
public class CaughtFish {

  private final CustomFish fish;
  private final double length;
  private final OfflinePlayer catcher;

  public CaughtFish(CustomFish fish, double length, OfflinePlayer catcher) {
    this.fish = fish;
    this.length = length;
    this.catcher = catcher;
  }
}
