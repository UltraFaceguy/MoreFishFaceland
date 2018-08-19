package me.elsiff.morefish.hooker;

import info.faceland.strife.StrifePlugin;
import org.bukkit.entity.Player;

public class StrifeHooker {

  private StrifePlugin strifePlugin;

  public StrifeHooker(StrifePlugin plugin) {
    this.strifePlugin = plugin;
  }

  public void addFishingExperience(Player player, double amount) {
    strifePlugin.getFishExperienceManager().addExperience(player, amount, false);
  }
}
