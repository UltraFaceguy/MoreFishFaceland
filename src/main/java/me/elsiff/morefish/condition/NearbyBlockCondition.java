package me.elsiff.morefish.condition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class NearbyBlockCondition implements Condition {

  private final Set<Material> blockSet = new HashSet<>();
  private Location cachedLocation;
  private List<Block> cachedBlocks;

  public NearbyBlockCondition(Set<Material> materials) {
    this.blockSet.addAll(materials);
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    if (cachedLocation == null || !cachedLocation.equals(fishLocation)) {
      cachedLocation = fishLocation;
      cachedBlocks = getNearbyBlocks(cachedLocation, 5);
    }
    Set<Material> nearbyMaterials = new HashSet<>();
    for (Block b : cachedBlocks) {
      nearbyMaterials.add(b.getType());
    }
    return nearbyMaterials.containsAll(blockSet);
  }

  public static List<Block> getNearbyBlocks(Location location, int radius) {
    List<Block> blocks = new ArrayList<>();
    for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
      for (int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
        for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
          blocks.add(location.getWorld().getBlockAt(x, y, z));
        }
      }
    }
    return blocks;
  }

  @Override
  public String getDescription() {
    return "Near specific blocks";
  }
}
