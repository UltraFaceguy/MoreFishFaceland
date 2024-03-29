package me.elsiff.morefish.listener;

import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.ItemUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import info.faceland.loot.api.items.CustomItem;
import info.faceland.loot.utils.MaterialUtil;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import land.face.jobbo.util.JobUtil;
import land.face.learnin.LearninBooksPlugin;
import land.face.strife.StrifePlugin;
import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.events.AutoFishEvent;
import land.face.strife.stats.StrifeStat;
import land.face.strife.util.PlayerDataUtil;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.event.PlayerCatchCustomFishEvent;
import me.elsiff.morefish.manager.ContestManager;
import me.elsiff.morefish.pojo.CaughtFish;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class FishingListener implements Listener {

  private final MoreFish plugin;
  private final ContestManager contest;

  private final float treasureChance;
  private final int minTreasureGems;
  private final int maxTreasureGems;
  private final int minTreasureTierItems;
  private final int maxTreasureTierItems;
  private final double tierRarityBonus;

  private final double baseFishXp;
  private final double fishXpPerCm;

  private final Map<String, Double> customItemChances;
  private final Set<UUID> baitOnCast = new HashSet<>();

  private final Random random = new Random();

  private Map<ItemStack, Float> treasureWeightMap = new HashMap<>();
  private float totalTreasureWeight;

  public FishingListener(MoreFish plugin) {
    this.plugin = plugin;
    contest = plugin.getContestManager();
    treasureChance = (float) plugin.getConfig().getDouble("treasure.chance");
    minTreasureGems = plugin.getConfig().getInt("treasure.loot-items.min-gems", 0);
    maxTreasureGems = plugin.getConfig().getInt("treasure.loot-items.max-gems", 2);
    minTreasureTierItems = plugin.getConfig().getInt("treasure.loot-items.min-tier-items", 0);
    maxTreasureTierItems = plugin.getConfig().getInt("treasure.loot-items.max-tier-items", 2);
    tierRarityBonus = plugin.getConfig().getInt("treasure.loot-items.item-rarity-bonus", 9000);

    baseFishXp = plugin.getConfig().getDouble("general.base-xp", 10);
    fishXpPerCm = plugin.getConfig().getDouble("general.xp-per-cm", 0.5);

    customItemChances = new HashMap<>();
    ConfigurationSection section = plugin.getConfig()
        .getConfigurationSection("treasure.loot-items.custom-items");

    for (String item : section.getKeys(false)) {
      System.out.println("loaded" + item + " chance " + section.getDouble(item));
      customItemChances.put(item, section.getDouble(item));
    }

    ConfigurationSection treasureSection = plugin.getConfig().getConfigurationSection("treasure.treasure-items");
    for (String treasureKey : treasureSection.getKeys(false)) {
      float weight = (float) treasureSection.getDouble(treasureKey);
      if ("REAL-CHEST".equals(treasureKey)) {
        ItemStack chest = new ItemStack(Material.SHULKER_SHELL);
        ItemStackExtensionsKt.setDisplayName(chest, FaceColor.RAINBOW + "Treasure Chest");
        ItemStackExtensionsKt.setCustomModelData(chest, 50);
        TextUtils.setLore(chest, List.of(
            FaceColor.GRAY + "A treasure chest! Who knows",
            FaceColor.GRAY + "what lies within!",
            FaceColor.WHITE + "[Hold and right-click to open]"
        ));
        treasureWeightMap.put(chest, weight);
        continue;
      }
      CustomItem ci = plugin.getLootHooker().getCustomItem(treasureKey);
      if (ci == null) {
        Bukkit.getLogger().warning("[MoreFish] Invalid treasure key " + treasureKey);
      } else {
        treasureWeightMap.put(ci.toItemStack(1), weight);
      }
    }
    totalTreasureWeight = 0;
    for (float f : treasureWeightMap.values()) {
      totalTreasureWeight += f;
    }
  }

  @EventHandler
  public void onCast(PlayerFishEvent event) {
    if (event.getState() == State.FISHING) {
      if (event.getPlayer().getEquipment().getItemInOffHand().getType() == Material.WHEAT_SEEDS) {
        baitOnCast.add(event.getPlayer().getUniqueId());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCustomCatch(PlayerCatchCustomFishEvent event) {
    boolean announce = event.getFish().getFish().getRarity().getWeight() < 40;
    boolean newLead = false;
    if (contest.hasStarted()) {
      event.setXp(event.getXp() * 2f);
      if (contest.getTopRecord() == null || contest.isNew1st(event.getFish())) {
        announce = true;
        newLead = true;
      }
    }
    if (announce) {
      Bukkit.getServer().broadcastMessage(getMessage("catch-fish", event.getPlayer(), event.getFish()));
    }
    if (newLead) {
      if (contest.getTopRecord() == null || !event.getPlayer().getUniqueId().equals(contest.getTopRecord().getUuid())) {
        Bukkit.getServer().broadcastMessage(getMessage("get-1st", event.getPlayer(), event.getFish()));
      } else {
        Bukkit.getServer().broadcastMessage(getMessage("extend-lead", event.getPlayer(), event.getFish()));
      }
    }
    if (contest.hasStarted()) {
      contest.addRecord(event.getPlayer(), event.getFish());
    }
  }

  @EventHandler
  public void onFish(PlayerFishEvent event) {
    if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH &&
        event.getCaught() instanceof Item) {
      if (baitOnCast.contains(event.getPlayer().getUniqueId()) &&
          event.getPlayer().getEquipment().getItemInOffHand().getType() != Material.WHEAT_SEEDS) {
        baitOnCast.remove(event.getPlayer().getUniqueId());
        event.setCancelled(true);
        return;
      }
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

  @EventHandler
  public void onAutoFish(AutoFishEvent event) {
    Player catcher = (Player) event.getMob().getEntity();

    if (baitOnCast.contains(catcher.getUniqueId()) &&
        catcher.getEquipment().getItemInOffHand().getType() != Material.WHEAT_SEEDS) {
      baitOnCast.remove(catcher.getUniqueId());
      return;
    }

    CaughtFish fish = plugin.getFishManager().generateRandomFish(catcher, event.getLocation());

    if (fish == null) {
      return;
    }

    float xp = (float) (baseFishXp + fishXpPerCm * fish.getLength());
    xp *= fish.getFish().getRarity().getXpMult();
    xp *= 0.75;

    PlayerCatchCustomFishEvent customEvent = new PlayerCatchCustomFishEvent(catcher, fish);
    customEvent.setXp(xp);
    plugin.getServer().getPluginManager().callEvent(customEvent);

    if (customEvent.isCancelled()) {
      return;
    }

    plugin.getStrifeHooker().addFishingExperience(catcher, event.getLocation(), customEvent.getXp());

    if (!fish.getFish().getCommands().isEmpty()) {
      executeCommands(catcher, fish);
    }

    JobUtil.bumpTaskProgress(catcher, "mf_fish", fish.getFish().getId());
    int fishScore = (int) (10 * Math.floor(fish.getLength() / 10));
    while (fishScore > 0) {
      JobUtil.bumpTaskProgress(catcher, "mf_fish_length", fishScore + "+");
      fishScore -= 10;
    }
    JobUtil.bumpTaskProgress(catcher, "mf_fish_rarity", fish.getFish().getRarity().getId());
    LearninBooksPlugin.instance.getKnowledgeManager().incrementKnowledge(catcher, fish.getFish().getId());

    ItemStack result = plugin.getFishManager().buildItemFromFish(fish, catcher.getName());

    if (Math.random() < 0.02) {
      MaterialUtil.depleteEnchantment(catcher.getEquipment().getItemInOffHand(),
          (Player) event.getMob().getEntity());
    }

    if (plugin.getTogglePickupsPlugin() != null &&
        !plugin.getTogglePickupsPlugin().getApi().playerCanPickUpItem(catcher, result)) {
      ItemUtils.dropItem(catcher.getLocation(), result, catcher,
          fish.getFish().getRarity().getBaseTicksLived(), null, null, false);
    } else {
      ItemUtils.giveOrDrop(catcher, fish.getFish().getRarity().getBaseTicksLived(), result);
    }

    if (catcher.getEquipment().getItemInOffHand().getType() == Material.WHEAT_SEEDS) {
      baitOnCast.add(catcher.getUniqueId());
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
    float treasureBonus = PlayerDataUtil.getSkillLevels(catcher, LifeSkillType.FISHING, true).getLevel() +
        StrifePlugin.getInstance().getStrifeMobManager().getStatMob(catcher).getStat(StrifeStat.FISHING_TREASURE);
    float totalTreasureChance = treasureChance * (1 + (treasureBonus / 100));
    if (Math.random() < totalTreasureChance) {
      Item caught = (Item) event.getCaught();
      caught.setItemStack(buildTreasure(catcher));
      JobUtil.bumpTaskProgress(event.getPlayer(), "FISH", "mf_fish", "INTERNAL_TREASURE");
      return;
    }

    CaughtFish fish = plugin.getFishManager().generateRandomFish(catcher, event.getCaught().getLocation());

    if (fish == null) {
      event.setCancelled(true);
      return;
    }

    float xp = (float) (baseFishXp + fishXpPerCm * fish.getLength());
    xp *= fish.getFish().getRarity().getXpMult();

    PlayerCatchCustomFishEvent customEvent = new PlayerCatchCustomFishEvent(catcher, fish, event);
    customEvent.setXp(xp);

    plugin.getServer().getPluginManager().callEvent(customEvent);

    if (customEvent.isCancelled()) {
      return;
    }

    plugin.getStrifeHooker().addFishingExperience(catcher, event.getHook().getLocation(), customEvent.getXp());

    if (!fish.getFish().getCommands().isEmpty()) {
      executeCommands(catcher, fish);
    }

    ItemStack itemStack = plugin.getFishManager().buildItemFromFish(fish, event.getPlayer().getName());

    if (itemStack == null) {
      event.setCancelled(true);
      return;
    }

    Item caught = (Item) event.getCaught();
    caught.setItemStack(itemStack);
    caught.setTicksLived(fish.getFish().getRarity().getBaseTicksLived());

    if (Math.random() < 0.02) {
      MaterialUtil.depleteEnchantment(catcher.getEquipment().getItemInOffHand(), event.getPlayer());
    }

    JobUtil.bumpTaskProgress(event.getPlayer(), "mf_fish", fish.getFish().getId());
    int fishScore = (int) (10 * Math.floor(fish.getLength() / 10));
    while (fishScore > 0) {
      JobUtil.bumpTaskProgress(event.getPlayer(), "mf_fish_length", fishScore + "+");
      fishScore -= 10;
    }
    JobUtil.bumpTaskProgress(event.getPlayer(), "mf_fish_rarity", fish.getFish().getRarity().getId());
    LearninBooksPlugin.instance.getKnowledgeManager().incrementKnowledge(catcher, fish.getFish().getId());
  }

  private String getMessage(String path, Player player, CaughtFish fish) {
    String message = plugin.getFishConfiguration().getString(path);

    message = message.replaceAll("%player%", player.getName())
        .replaceAll("%length%", fish.getLength() + "")
        .replaceAll("%rarity%", fish.getFish().getRarity().getDisplayName())
        .replaceAll("%rarity_color%", fish.getFish().getRarity().getColor() + "")
        .replaceAll("%fish%", fish.getFish().getName());

    message = ChatColor.translateAlternateColorCodes('&', message);

    return message;
  }

  private void executeCommands(Player player, CaughtFish fish) {
    for (String command : fish.getFish().getCommands()) {
      String str = command.replaceAll("@p", player.getName())
          .replaceAll("%fish%", fish.getFish().getName())
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
    if (true == true) {
      float maxWeight = (float) (Math.random() * totalTreasureWeight);
      float currentWeight = 0;
      for (Entry<ItemStack, Float> entry : treasureWeightMap.entrySet()) {
        currentWeight += entry.getValue();
        if (currentWeight >= maxWeight) {
          return entry.getKey().clone();
        }
      }
    }
    return null;
  }

  public List<ItemStack> getLootItems(Player player) {
    List<ItemStack> items = new ArrayList<>();
    items.addAll(plugin.getLootHooker().getGems(minTreasureGems +
        random.nextInt(maxTreasureGems - minTreasureGems + 1)));
    items.addAll(plugin.getLootHooker().getTierItems(minTreasureTierItems +
            random.nextInt(maxTreasureTierItems - minTreasureTierItems + 1), player.getLevel(),
        tierRarityBonus));
    items.addAll(plugin.getLootHooker().getCustomItems(customItemChances));
    return items;
  }
}
