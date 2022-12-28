package me.elsiff.morefish.pojo;

import java.util.List;
import java.util.Set;
import lombok.Data;
import me.elsiff.morefish.condition.Condition;
import org.bukkit.block.Biome;

@Data
public class CustomFish {

  private final String id;
  private final String name;
  private final double lengthMin;
  private final double lengthMax;
  private final int modelData;
  private final List<String> lore;
  private final List<String> commands;
  private final List<Condition> conditions;
  private final Rarity rarity;
  private final Set<Biome> biomes;
  private final Set<String> regions;

  public CustomFish(String id, String name, double lengthMin, double lengthMax, int modelData,
      List<String> lore, List<String> commands, List<Condition> conditions, Rarity rarity,
      Set<Biome> biomes, Set<String> regions) {
    this.id = id;
    this.name = name;
    this.lengthMin = lengthMin;
    this.lengthMax = lengthMax;
    this.modelData = modelData;
    this.lore = lore;
    this.commands = commands;
    this.conditions = conditions;
    this.rarity = rarity;
    this.biomes = biomes;
    this.regions = regions;
  }
}
