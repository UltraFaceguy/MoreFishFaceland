package me.elsiff.morefish.event;

import lombok.Getter;
import lombok.Setter;
import me.elsiff.morefish.pojo.CaughtFish;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class PlayerCatchCustomFishEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled;
  private final CaughtFish fish;
  @Getter @Setter
  private float xp;
  private final PlayerFishEvent fishEvent;

  public PlayerCatchCustomFishEvent(Player who, CaughtFish fish, PlayerFishEvent fishEvent) {
    super(who);
    this.fish = fish;
    this.fishEvent = fishEvent;
  }

  public PlayerCatchCustomFishEvent(Player who, CaughtFish fish) {
    super(who);
    this.fish = fish;
    this.fishEvent = null;
  }

  public CaughtFish getFish() {
    return fish;
  }

  public PlayerFishEvent getPlayerFishEvent() {
    return fishEvent;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    cancelled = cancel;
  }
}
