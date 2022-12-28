package me.elsiff.morefish.listener;

import me.elsiff.morefish.MoreFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
}
