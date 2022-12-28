package me.elsiff.morefish.pojo;

import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import lombok.Data;

@Data
public class Rarity {

  private final String id;
  private final String displayName;
  private final double weight;
  private final double xpMult;
  private final boolean broadcast;
  private final int baseTicksLived;
  private final FaceColor color;

  public Rarity(String id, String displayName, double weight, double xpMult,
      boolean broadcast, int baseTicksLived, FaceColor color) {
    this.id = id;
    this.displayName = displayName;
    this.weight = weight;
    this.xpMult = xpMult;
    this.broadcast = broadcast;
    this.baseTicksLived = baseTicksLived;
    this.color = color;
  }
}
