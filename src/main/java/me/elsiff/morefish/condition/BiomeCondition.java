package me.elsiff.morefish.condition;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class BiomeCondition implements Condition {

  private final List<Biome> biomeList = new ArrayList<>();

  public BiomeCondition(List<Biome> biomeList) {
    this.biomeList.addAll(biomeList);
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    return (biomeList.contains(player.getWorld().getBiome(fishLocation.getBlockX(),
        fishLocation.getBlockY(), fishLocation.getBlockZ())));
  }
}
