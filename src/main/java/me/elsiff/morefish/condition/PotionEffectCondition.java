package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectCondition implements Condition {

  private final PotionEffectType effectType;
  private final int amplifier;

  public PotionEffectCondition(PotionEffectType effectType, int amplifier) {
    this.effectType = effectType;
    this.amplifier = amplifier;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    return (player.hasPotionEffect(effectType) &&
        player.getPotionEffect(effectType).getAmplifier() >= amplifier);
  }

  @Override
  public String getDescription() {
    return "Effect " + effectType.getName() + " Lv" + amplifier+1 + "+";
  }
}
