package me.elsiff.morefish.hooker;

import info.faceland.loot.LootPlugin;
import info.faceland.loot.api.items.CustomItem;
import info.faceland.loot.api.items.ItemGenerationReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class LootHooker {

  private Random random = new Random();

  public List<ItemStack> getGems(int amount) {
    List<ItemStack> gems = new ArrayList<>();
    while (amount > 0) {
      gems.add(LootPlugin.getInstance().getSocketGemManager().getRandomSocketGemByBonus()
          .toItemStack(1));
      amount--;
    }
    return gems;
  }

  public List<ItemStack> getTierItems(int amount, int level, double bonus) {
    List<ItemStack> items = new ArrayList<>();
    while (amount > 0) {
      items.add(LootPlugin.getInstance().getNewItemBuilder()
          .withTier(LootPlugin.getInstance().getTierManager().getRandomTier())
          .withRarity(LootPlugin.getInstance().getRarityManager().getRandomRarityWithBonus(bonus))
          .withLevel(Math.max(1, Math.min(level - 2 + random.nextInt(5), 100)))
          .withItemGenerationReason(ItemGenerationReason.EXTERNAL)
          .withSpecialStat(false)
          .build().getStack());
      amount--;
    }
    return items;
  }

  public List<ItemStack> getCustomItems(Map<String, Double> customChances) {
    List<ItemStack> items = new ArrayList<>();
    for (String customId : customChances.keySet()) {
      if (customChances.get(customId) > Math.random()) {
        CustomItem ci = LootPlugin.getInstance().getCustomItemManager().getCustomItem(customId);
        if (ci != null) {
          items.add(LootPlugin.getInstance().getCustomItemManager().getCustomItem(customId)
              .toItemStack(1));
        } else {
          Bukkit.getLogger().warning("Invalid custom item id in treasures: " + customId);
        }
      }
    }
    return items;
  }
}
