package me.elsiff.morefish.pojo;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.bukkit.block.Biome;

@Data
public class FishZone {

  private String id;
  private Set<Biome> biome = new HashSet<>();
  private String description;

}
