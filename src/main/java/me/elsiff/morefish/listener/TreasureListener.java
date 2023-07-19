package me.elsiff.morefish.listener;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import me.elsiff.morefish.MoreFish;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TreasureListener implements Listener {

  private final MoreFish plugin;

  public TreasureListener(MoreFish plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlaceTreasure(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    if (event.getAction() != Action.RIGHT_CLICK_AIR &&
        event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if (event.getInteractionPoint() == null) {
      return;
    }
    ItemStack stack = event.getPlayer().getEquipment().getItemInMainHand();
    if (stack.getType() == Material.SHULKER_SHELL) {
      ItemMeta meta = stack.getItemMeta();
      if (meta != null && meta.getCustomModelData() == 50) {
        event.setCancelled(true);
        stack.setAmount(stack.getAmount() - 1);
        Location location = event.getInteractionPoint().clone();
        location.getWorld().playSound(location, Sound.AMBIENT_UNDERWATER_ENTER, 1, 1);
        location.getWorld().playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);

        ItemStack[] stacks = new ItemStack[27];
        Set<Integer> freeSlots = getFreeSpace(stacks);
        for (ItemStack loot : plugin.getFishingListener().getLootItems(event.getPlayer())) {
          if (freeSlots.isEmpty()) {
            break;
          }
          int slot = new Random().nextInt(freeSlots.size());
          stacks[slot] = loot;
          freeSlots.remove(slot);
        }
        for (ItemStack i : stacks) {
          if (i == null) {
            continue;
          }
          Item item = location.getWorld().dropItemNaturally(location, i);
          item.setOwner(event.getPlayer().getUniqueId());
        }
      }
    }
  }

  private Set<Integer> getFreeSpace(ItemStack[] slots) {
    Set<Integer> openSlots = new HashSet<>();
    for (int i = 0; i < slots.length; i++) {
      if (slots[i] == null || slots[i].getType() == Material.AIR) {
        openSlots.add(i);
      }
    }
    return openSlots;
  }
}
