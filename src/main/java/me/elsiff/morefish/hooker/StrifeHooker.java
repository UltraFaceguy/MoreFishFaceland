package me.elsiff.morefish.hooker;

import land.face.strife.StrifePlugin;
import land.face.strife.data.champion.LifeSkillType;
import org.bukkit.entity.Player;

public class StrifeHooker {

  private StrifePlugin strifePlugin;

  public StrifeHooker(StrifePlugin plugin) {
    this.strifePlugin = plugin;
  }

  public void addFishingExperience(Player player, double amount) {
    strifePlugin.getSkillExperienceManager()
        .addExperience(player, LifeSkillType.FISHING, amount, false);
  }
}
