package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnchantmentCondition implements Condition {

  private final Enchantment enchantment;
  private final int level;

  public EnchantmentCondition(Enchantment enchantment, int level) {
    this.enchantment = enchantment;
    this.level = level;
  }

  @Override
  public boolean isSatisfying(Player player, Location fishLocation) {
    ItemStack hand = player.getInventory().getItemInMainHand();
    return (hand.containsEnchantment(enchantment)
        && hand.getEnchantmentLevel(enchantment) >= level);
  }

  @Override
  public String getDescription() {
    return "Needs a certain enchantment";
  }
}
