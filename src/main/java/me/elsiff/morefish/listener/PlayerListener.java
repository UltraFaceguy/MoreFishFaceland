package me.elsiff.morefish.listener;

import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import info.faceland.loot.data.PawnDeal;
import info.faceland.loot.events.PawnDealCreateEvent;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.pojo.CustomFish;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

  private final MoreFish plugin;

  public PlayerListener(MoreFish plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event.getPlayer().isOp() && plugin.getConfig().getBoolean("general.check-update") &&
        !plugin.getUpdateChecker().isUpToDate()) {
      for (String msg : plugin.getFishConfiguration().getStringList("new-version")) {
        event.getPlayer().sendMessage(String.format(msg, plugin.getUpdateChecker().getNewVersion()));
      }
    }
  }

  @EventHandler
  public void onLeave(PlayerQuitEvent event) {
    FishHook fishHook = event.getPlayer().getFishHook();
    if (fishHook != null && fishHook.isValid()) {
      fishHook.remove();
    }
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    FishHook fishHook = event.getPlayer().getFishHook();
    if (fishHook != null && fishHook.isValid()) {
      fishHook.remove();
    }
  }

  @EventHandler
  public void fishDeal(PawnDealCreateEvent event) {
    if (!"fishmonger".equals(event.getShopId())) {
      return;
    }
    switch (event.getDealSlot()) {
      case 1 -> {
        CustomFish cf = plugin.getFishManager()
            .getRandomFish(plugin.getFishManager().getRarity("uncommon"));
        event.setResult(new PawnDeal(
            FaceColor.BLUE + cf.getName(), Material.KELP, cf.getModelData(),
            1,
            1.5f + (float) Math.random(),
            3 + (int) (Math.random() * 3)
        ));
      }
      case 2 -> {
        CustomFish cf = plugin.getFishManager()
            .getRandomFish(plugin.getFishManager().getRarity("rare"));
        event.setResult(new PawnDeal(
            FaceColor.PURPLE + cf.getName(), Material.KELP, cf.getModelData(),
            1,
            2.5f + (float) Math.random(),
            17 + (int) (Math.random() * 6)
        ));
      }
      case 3 -> {
        CustomFish cf = plugin.getFishManager()
            .getRandomFish(plugin.getFishManager().getRarity("epic"));
        event.setResult(new PawnDeal(
            FaceColor.RED + cf.getName(), Material.KELP, cf.getModelData(),
            1,
            3.5f + (float) Math.random(),
            60 + (int) (Math.random() * 40)
        ));
      }
    }
  }
}
