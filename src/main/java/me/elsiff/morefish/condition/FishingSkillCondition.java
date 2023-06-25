package me.elsiff.morefish.condition;

import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.util.PlayerDataUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FishingSkillCondition implements Condition {

  private final int level;

  public FishingSkillCondition(int level) {
    this.level = level;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    return PlayerDataUtil.getLifeSkillLevel(player, LifeSkillType.FISHING) >= level;
  }

  @Override
  public String getDescription() {
    return "Skill Lv" + level + "+";
  }
}
