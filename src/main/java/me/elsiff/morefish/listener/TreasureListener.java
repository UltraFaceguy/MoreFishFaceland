package me.elsiff.morefish.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class TreasureListener implements Listener {

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlaceTreasure(BlockPlaceEvent event) {
    if (event.getBlock().getType() == Material.BLACK_SHULKER_BOX) {
      ItemMeta meta = event.getItemInHand().getItemMeta();
      if (meta != null && meta.getCustomModelData() == 1337) {
        event.setCancelled(true);
        event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
        BlockStateMeta im = (BlockStateMeta) meta;
        ShulkerBox shulker = (ShulkerBox) im.getBlockState();
        Location location = event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
        location.getWorld().playSound(location, Sound.AMBIENT_UNDERWATER_ENTER, 1, 1);
        location.getWorld().playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
        for (ItemStack i : shulker.getInventory()) {
          if (i == null || i.getType() == Material.AIR) {
            continue;
          }
          Item item = location.getWorld().dropItemNaturally(location, i);
          item.setOwner(event.getPlayer().getUniqueId());
        }
      }
    }
  }
}
