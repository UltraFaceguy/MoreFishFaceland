package me.elsiff.morefish.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.util.PlayerDataUtil;
import me.elsiff.morefish.pojo.CaughtFish;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.event.PlayerCatchCustomFishEvent;
import me.elsiff.morefish.manager.ContestManager;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.HashSet;
import java.util.Set;

public class FishingListener implements Listener {

  private final MoreFish plugin;
  private final ContestManager contest;

  private double treasureChance;
  private double treasurePerLevel;
  private int minTreasureGems;
  private int maxTreasureGems;
  private int minTreasureTierItems;
  private int maxTreasureTierItems;
  private double tierRarityBonus;

  private Random random = new Random();

  public FishingListener(MoreFish plugin) {
    this.plugin = plugin;
    this.contest = plugin.getContestManager();

    this.treasureChance = plugin.getConfig().getDouble("treasure.chance");
    this.treasurePerLevel = plugin.getConfig().getDouble("treasure.chance-per-skill");
    this.minTreasureGems = plugin.getConfig().getInt("treasure.loot-items.min-gems", 0);
    this.maxTreasureGems = plugin.getConfig().getInt("treasure.loot-items.max-gems", 2);
    this.minTreasureTierItems = plugin.getConfig().getInt("treasure.loot-items.min-tier-items", 0);
    this.maxTreasureTierItems = plugin.getConfig().getInt("treasure.loot-items.max-tier-items", 2);
    this.tierRarityBonus = plugin.getConfig().getInt("treasure.loot-items.item-rarity-bonus", 9000);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFish(PlayerFishEvent event) {
    if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event
        .getCaught() instanceof Item) {
      if (!contest.hasStarted() && plugin.getConfig()
          .getBoolean("general.no-fishing-unless-contest")) {
        event.setCancelled(true);

        String msg = plugin.getFishConfiguration().getString("no-fishing-allowed");
        event.getPlayer().sendMessage(msg);
        return;
      }
      if (!isFishingEnabled(event)) {
        return;
      }

      event.setExpToDrop(0);
      executeFishingActions(event.getPlayer(), event);
    }
  }

  private boolean isFishingEnabled(PlayerFishEvent event) {
    // Check if the world hasn't disabled
    if (plugin.getConfig().getStringList("general.contest-disabled-worlds")
        .contains(event.getPlayer().getWorld().getName())) {
      return false;
    }

    // Check if the contest is ongoing
    if (plugin.getConfig().getBoolean("general.only-for-contest") &&
        !contest.hasStarted()) {
      return false;
    }

    // Check if the caught is fish
    Material caughtMaterial = ((Item) event.getCaught()).getItemStack().getType();
    return !plugin.getConfig().getBoolean("general.replace-only-fish") ||
        caughtMaterial == Material.TROPICAL_FISH || caughtMaterial == Material.COD
        || caughtMaterial == Material.PUFFERFISH || caughtMaterial == Material.SALMON;
  }

  private void executeFishingActions(Player catcher, PlayerFishEvent event) {
    if (treasureChance + treasurePerLevel * PlayerDataUtil.getEffectiveLifeSkill(
        catcher, LifeSkillType.FISHING, true) > Math.random()) {
      Item caught = (Item) event.getCaught();
      caught.setItemStack(buildTreasure(catcher));
      return;
    }
    CaughtFish fish = plugin.getFishManager().generateRandomFish(catcher);

    PlayerCatchCustomFishEvent customEvent = new PlayerCatchCustomFishEvent(catcher, fish, event);
    plugin.getServer().getPluginManager().callEvent(customEvent);

    if (customEvent.isCancelled()) {
      return;
    }

    if (fish.getFishingExperience() > 0.01) {
      double xp = fish.getFishingExperience() * (contest.hasStarted() ? 2 : 1);
      plugin.getStrifeHooker().addFishingExperience(catcher, xp);
    }

    boolean topFish = contest.hasStarted() && contest.isNew1st(fish);
    boolean newFirst = false;
    if (topFish) {
      newFirst = contest.getRecord(1) == null ||
          contest.getRecord(1).getPlayer() != fish.getCatcher();
    }

    if (fish.getRarity().hasFirework()) {
      launchFirework(catcher.getLocation().add(0, 1, 0));
    }
    if (!fish.getCommands().isEmpty()) {
      executeCommands(catcher, fish);
    }
    if (contest.hasStarted()) {
      contest.addRecord(catcher, fish);
    }

    announceMessages(catcher, fish, newFirst);

    ItemStack itemStack = plugin.getFishManager().getItemStack(fish, event.getPlayer().getName());
    Item caught = (Item) event.getCaught();
    caught.setItemStack(itemStack);
  }

  private void announceMessages(Player catcher, CaughtFish fish, boolean new1st) {
    String msgFish = getMessage("catch-fish", catcher, fish);
    String msgContest = getMessage("get-1st", catcher, fish);
    int ancFish = plugin.getConfig().getInt("messages.announce-catch");
    int ancContest = plugin.getConfig().getInt("messages.announce-new-1st");

    if (fish.getRarity().isNoBroadcast()) {
      ancFish = 0;
    }
    if (new1st) {
      ancFish = ancContest;
    }

    getMessageReceivers(ancFish, catcher)
        .forEach(player -> player.sendMessage(msgFish));

    if (new1st) {
      getMessageReceivers(ancContest, catcher)
          .forEach(player -> player.sendMessage(msgContest));
    }
  }

  private String getMessage(String path, Player player, CaughtFish fish) {
    String message = plugin.getFishConfiguration().getString(path);

    message = message.replaceAll("%player%", player.getName())
        .replaceAll("%length%", fish.getLength() + "")
        .replaceAll("%rarity%", fish.getRarity().getDisplayName())
        .replaceAll("%rarity_color%", fish.getRarity().getColor() + "")
        .replaceAll("%fish%", fish.getName())
        .replaceAll("%fish_with_rarity%",
            ((fish.getRarity().isNoDisplay()) ? "" : fish.getRarity().getDisplayName() + " ") + fish
                .getName());

    message = ChatColor.translateAlternateColorCodes('&', message);

    return message;
  }

  private Set<Player> getMessageReceivers(int announceValue, Player catcher) {
    Set<Player> players = new HashSet<>();

    switch (announceValue) {
      case 0:
        break;
      case -1:
        players.addAll(plugin.getServer().getOnlinePlayers());
        break;
      default:
        Location loc = catcher.getLocation();

        for (Player player : catcher.getWorld().getPlayers()) {
          if (player.getLocation().distance(loc) <= announceValue) {
            players.add(player);
          }
        }
    }

    if (plugin.getConfig().getBoolean("messages.only-announce-fishing-rod")) {
      players.removeIf(
          player -> player.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD);
    }

    return players;
  }

  private void executeCommands(Player player, CaughtFish fish) {
    for (String command : fish.getCommands()) {
      String str = command.replaceAll("@p", player.getName())
          .replaceAll("%fish%", fish.getName())
          .replaceAll("%length%", fish.getLength() + "");

      str = ChatColor.translateAlternateColorCodes('&', str);

      plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), str);
    }
  }

  private void launchFirework(Location loc) {
    Firework firework = loc.getWorld().spawn(loc, Firework.class);
    FireworkMeta meta = firework.getFireworkMeta();
    FireworkEffect effect = FireworkEffect.builder()
        .with(FireworkEffect.Type.BALL_LARGE)
        .withColor(Color.AQUA)
        .withFade(Color.BLUE)
        .withTrail()
        .withFlicker()
        .build();
    meta.addEffect(effect);
    meta.setPower(1);
    firework.setFireworkMeta(meta);
  }

  private ItemStack buildTreasure(Player player) {
    ItemStack item = new ItemStack(Material.SHULKER_BOX);
    if (item.getItemMeta() instanceof BlockStateMeta) {
      BlockStateMeta im = (BlockStateMeta) item.getItemMeta();
      if (im.getBlockState() instanceof ShulkerBox) {
        ShulkerBox shulker = (ShulkerBox) im.getBlockState();
        ItemStack[] stacks = new ItemStack[27];
        Set<Integer> freeSlots = getFreeSpace(stacks);
        for (ItemStack stack : getLootItems(player)) {
          if (freeSlots.isEmpty()) {
            break;
          }
          int slot = new Random().nextInt(freeSlots.size());
          stacks[slot] = stack;
          freeSlots.remove(slot);
        }
        shulker.getInventory().setContents(stacks);
        im.setBlockState(shulker);
        item.setItemMeta(im);
      }
    }
    return item;
  }

  private List<ItemStack> getLootItems(Player player) {
    List<ItemStack> items = new ArrayList<>();
    items.addAll(plugin.getLootHooker().getGems(minTreasureGems +
        random.nextInt(maxTreasureGems - minTreasureGems + 1)));
    items.addAll(plugin.getLootHooker().getTierItems(minTreasureTierItems +
        random.nextInt(maxTreasureTierItems - minTreasureTierItems + 1), player.getLevel(),
        tierRarityBonus));
    items.addAll(plugin.getLootHooker().getCustomItems());
    return items;
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
