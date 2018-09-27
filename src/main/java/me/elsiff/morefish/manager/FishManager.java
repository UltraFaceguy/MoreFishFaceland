package me.elsiff.morefish.manager;

import info.faceland.strife.util.PlayerDataUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import me.elsiff.morefish.pojo.CaughtFish;
import me.elsiff.morefish.pojo.CustomFish;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.pojo.Rarity;
import me.elsiff.morefish.condition.*;
import me.elsiff.morefish.util.IdentityUtils;
import me.elsiff.morefish.util.SkullUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;

import java.text.SimpleDateFormat;
import java.util.*;

public class FishManager {

  private final MoreFish plugin;
  private final Random random = new Random();
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
  private final List<Rarity> rarityList = new ArrayList<>();
  private final Map<String, CustomFish> fishMap = new HashMap<>();
  private final Map<Rarity, List<CustomFish>> rarityMap = new HashMap<>();

  public FishManager(MoreFish plugin) {
    this.plugin = plugin;
    loadFishList();
  }

  public void loadFishList() {
    fishMap.clear();
    rarityMap.clear();

    loadRarities(plugin.getFishConfiguration().getRarityConfig());
    loadFish(plugin.getFishConfiguration().getFishConfig());

    plugin.getLogger().info("Loaded " + rarityList.size() + " fish rarities successfully.");
    plugin.getLogger().info("Loaded " + fishMap.size() + " fish successfully.");
  }

  private void loadRarities(FileConfiguration config) {
    ConfigurationSection rarities = config.getConfigurationSection("rarity-list");

    for (String path : rarities.getKeys(false)) {
      String displayName = rarities.getString(path + ".display-name");
      double weight = rarities.getDouble(path + ".weight");
      double bonusWeight = rarities.getDouble(path + ".level-weight");
      ChatColor color = ChatColor.valueOf(rarities.getString(path + ".color").toUpperCase());

      double additionalPrice = rarities.getDouble(path + ".additional-price", 0D);
      boolean noBroadcast = rarities.getBoolean(path + ".no-broadcast", true);
      boolean noDisplay = rarities.getBoolean(path + ".no-display");
      boolean firework = rarities.getBoolean(path + ".firework", false);

      Rarity rarity = new Rarity(path, displayName, weight, bonusWeight, color, additionalPrice, noBroadcast, noDisplay, firework);

      rarityList.add(rarity);
    }
  }

  private void loadFish(FileConfiguration config) {
    List<Rarity> invalidRarities = new ArrayList<>();
    for (Rarity rarity : rarityList) {
      List<CustomFish> fishList = new ArrayList<>();
      ConfigurationSection section = config.getConfigurationSection("fish-list." + rarity.getName().toLowerCase());
      if (section == null) {
        plugin.getLogger().severe("No section/fish found for rarity " + rarity.getName() + "!");
        invalidRarities.add(rarity);
        continue;
      }
      for (String path : section.getKeys(false)) {
        CustomFish fish = createCustomFish(section, path, rarity);
        fishList.add(fish);
        fishMap.put(fish.getInternalName(), fish);
      }
      rarityMap.put(rarity, fishList);
    }
    for (Rarity r : invalidRarities) {
      rarityList.remove(r);
    }
  }

  private CustomFish createCustomFish(ConfigurationSection section, String path, Rarity rarity) {
    String displayName = section.getString(path + ".display-name", "");
    double lengthMin = section.getDouble(path + ".length-min");
    double lengthMax = section.getDouble(path + ".length-max");
    double fishingExp = section.getDouble(path + ".fish-exp", 0);
    ItemStack icon = getIcon(section, path);
    boolean skipItemFormat = section.getBoolean(path + ".skip-item-format", false);
    List<String> commands = new ArrayList<>();
    CustomFish.FoodEffects foodEffects = new CustomFish.FoodEffects();
    List<Condition> conditions = new ArrayList<>();

    if (section.contains(path + ".command")) {
      commands.addAll(section.getStringList(path + ".command"));
    }
    if (section.contains(path + ".commands")) {
      commands.addAll(section.getStringList(path + ".commands"));
    }

    if (section.contains(path + ".food-effects")) {
      if (section.contains(path + ".food-effects.points")) {
        foodEffects.setPoints(section.getInt(path + ".food-effects.points"));
      }

      if (section.contains(path + ".food-effects.saturation")) {
        foodEffects.setSaturation((float) section.getDouble(path + ".food-effects.saturation"));
      }

      if (section.contains(path + ".food-effects.commands")) {
        foodEffects.setCommands(section.getStringList(path + ".food-effects.commands"));
      }
    }

    if (section.contains(path + ".conditions")) {
      List<String> list = section.getStringList(path + ".conditions");

      for (String content : list) {
        Condition condition = getCondition(content);
        conditions.add(condition);
      }
    }

    return new CustomFish(path, displayName, lengthMin, lengthMax, icon, skipItemFormat,
        commands, foodEffects, conditions, rarity, fishingExp);
  }

  private ItemStack getIcon(ConfigurationSection section, String path) {
    ItemStack itemStack;

    String id = section.getString(path + ".icon.id");
    Material material = IdentityUtils.getMaterial(id);
    if (material == null) {
      plugin.getLogger().warning("'" + id + "' is invalid item id!");
      return null;
    }

    int amount = 1;
    if (section.contains(path + ".icon.amount")) {
      amount = section.getInt(path + ".icon.amount");
    }

    short durability = 0;
    if (section.contains(path + ".icon.durability")) {
      durability = (short) section.getInt(path + ".icon.durability");
    }

    itemStack = new ItemStack(material, amount, durability);
    ItemMeta meta = itemStack.getItemMeta();

    if (section.contains(path + ".icon.lore")) {
      List<String> lore = new ArrayList<>();
      for (String line : section.getStringList(path + ".icon.lore")) {
        lore.add(ChatColor.translateAlternateColorCodes('&', line));
      }
      meta.setLore(lore);
    }

    if (section.contains(path + ".icon.enchantments")) {
      for (String content : section.getStringList(path + ".icon.enchantments")) {
        String[] values = content.split("\\|");
        Enchantment ench = IdentityUtils.getEnchantment(values[0].toLowerCase());
        int lv = Integer.parseInt(values[1]);
        meta.addEnchant(ench, lv, true);
      }
    }

    if (section.contains(path + ".icon.unbreakable")) {
      boolean value = section.getBoolean(path + ".icon.unbreakable");
      meta.setUnbreakable(value);
    }

    if (section.contains(path + ".icon.skull-name")) {
      SkullMeta skullMeta = (SkullMeta) meta;
      skullMeta.setOwner(section.getString(path + ".icon.skull-name"));
    }

    itemStack.setItemMeta(meta);

    if (section.contains(path + ".icon.skull-texture")) {
      String value = section.getString(path + ".icon.skull-texture");
      itemStack = SkullUtils.setSkullTexture(itemStack, value);
    }

    if (itemStack == null || itemStack.getItemMeta() == null) {
      plugin.getLogger().severe("Item found at section '" + section + "' path '" + path +"' is invalid!");
    }
    return itemStack;
  }

  private Condition getCondition(String content) {
    Condition condition;
    String[] values = content.split("\\|");
    String conId = values[0];

    switch (conId) {
      case "raining":
        boolean raining = Boolean.parseBoolean(values[1]);
        condition = new RainingCondition(raining);
        break;
      case "thundering":
        boolean thundering = Boolean.parseBoolean(values[1]);
        condition = new ThunderingCondition(thundering);
        break;
      case "time":
        String time = values[1].toLowerCase();
        condition = new TimeCondition(time);
        break;
      case "biome":
        condition = new BiomeCondition(getBiomes(values));
        break;
      case "enchantment":
        Enchantment ench = IdentityUtils.getEnchantment(values[1].toLowerCase());
        int lv = Integer.parseInt(values[2]);
        condition = new EnchantmentCondition(ench, lv);
        break;
      case "level":
        int level = Integer.parseInt(values[1]);
        condition = new LevelCondition(level);
        break;
      case "contest":
        boolean ongoing = Boolean.parseBoolean(values[1]);
        condition = new ContestCondition(ongoing);
        break;
      case "potioneffect":
        PotionEffectType effectType = IdentityUtils.getPotionEffectType(values[1]);
        int amplfier = Integer.parseInt(values[2]);
        condition = new PotionEffectCondition(effectType, amplfier);
        break;
      case "height":
        int minHeight = Integer.parseInt(values[1]);
        int maxHeight = Integer.parseInt(values[2]);
        condition = new HeightCondition(minHeight, maxHeight);
        break;
      case "worldguard_region":
        String regionId = values[1];
        condition = new WGRegionCondtion(regionId);
        break;
      case "fishing_skill":
        int skill = Integer.parseInt(values[1]);
        condition = new FishingSkillCondition(skill);
        break;
      default:
        return null;
    }
    return condition;
  }

  public CaughtFish generateRandomFish(Player catcher) {
    // TODO: Only set level if strife is loaded
    double rodLuck = getLuckFromPlayer(catcher);
    Rarity rarity = getRandomRarity(PlayerDataUtil.getFishSkill(catcher, true), rodLuck);
    CustomFish type = getRandomFish(rarity, catcher);
    return createCaughtFish(type, catcher, catcher.hasPotionEffect(PotionEffectType.LUCK));
  }

  public CustomFish getCustomFish(String name) {
    return fishMap.get(name);
  }

  public ItemStack getItemStack(CaughtFish fish, String fisher) {
    ItemStack itemStack = fish.getIcon();
    ItemMeta meta = itemStack.getItemMeta();

    if (!fish.hasNoItemFormat()) {
      FileConfiguration config = plugin.getFishConfiguration().getFishConfig();

      String displayName = config.getString("item-format.display-name")
          .replaceAll("%player%", fisher)
          .replaceAll("%rarity%", fish.getRarity().getDisplayName())
          .replaceAll("%rarity_color%", fish.getRarity().getColor() + "")
          .replaceAll("%fish%", fish.getName());
      displayName = ChatColor.translateAlternateColorCodes('&', displayName);
      meta.setDisplayName(displayName + encodeFishData(fish));

      List<String> lore = new ArrayList<>();
      for (String str : config.getStringList("item-format.lore")) {
        String line = str
            .replaceAll("%player%", fisher)
            .replaceAll("%rarity%", fish.getRarity().getDisplayName())
            .replaceAll("%rarity_color%", fish.getRarity().getColor() + "")
            .replaceAll("%length%", fish.getLength() + "")
            .replaceAll("%fish%", fish.getName())
            .replaceAll("%date%", dateFormat.format(new Date()));

        line = ChatColor.translateAlternateColorCodes('&', line);
        lore.add(line);
      }
      if (meta.hasLore()) {
        lore.addAll(meta.getLore());
      }
      meta.setLore(lore);
    }

    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public CaughtFish getCaughtFish(ItemStack itemStack) {
    if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasDisplayName()) {
      return null;
    }

    String displayName = itemStack.getItemMeta().getDisplayName();
    return decodeFishData(displayName);
  }

  public boolean isCustomFish(ItemStack itemStack) {
    return (getCaughtFish(itemStack) != null);
  }

  private CaughtFish createCaughtFish(CustomFish fish, OfflinePlayer catcher, boolean lucky) {
    double length;

    if (fish.getLengthMax() <= fish.getLengthMin()) {
      length = fish.getLengthMax();
    } else {
      double sizeMultiplier = lucky ? random.nextDouble() : Math.pow(random.nextDouble(), 2);
      length = fish.getLengthMin() + ((fish.getLengthMax() - fish.getLengthMin()) * sizeMultiplier);
      length = new BigDecimal(length).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    return new CaughtFish(fish, length, catcher);
  }

  private Rarity getRandomRarity(double level, double luck) {
    int skillAndLuckBonus = (int) (level + luck);
    double cur = 0.0D;
    double randomVar = random.nextDouble() * getTotalRarity(skillAndLuckBonus);
    for (Rarity rarity : rarityList) {
      cur += getAdjustedWeight(rarity, skillAndLuckBonus);
      if (cur >= randomVar) {
        return rarity;
      }
    }
    return null;
  }

  private CustomFish getRandomFish(Rarity rarity, Player player) {
    List<CustomFish> fishList = new ArrayList<>();
    for (CustomFish fish : rarityMap.get(rarity)) {
      if (fish.getConditions().isEmpty()) {
        fishList.add(fish);
        continue;
      }
      boolean meetsConditions = true;
      for (Condition condition : fish.getConditions()) {
        if (!condition.isSatisfying(player)) {
          meetsConditions = false;
        }
      }
      if (meetsConditions) {
        fishList.add(fish);
      }
    }
    return fishList.get(random.nextInt(fishList.size()));
  }

  private String encodeFishData(CaughtFish fish) {
    String data = "|"
        .concat("name:" + fish.getInternalName() + "|")
        .concat("length:" + fish.getLength() + "|")
        .concat("catcher:" + fish.getCatcher().getUniqueId())
        .replaceAll("", "§");
    data = data.substring(0, data.length() - 1);
    return data;
  }

  private CaughtFish decodeFishData(String displayName) {
    String[] split = displayName.replaceAll("§", "").split("\\|");
    if (split.length < 1) {
      return null;
    }

    String name = null;
    double length = 0.0D;
    OfflinePlayer catcher = null;

    for (int i = 1; i < split.length; i++) {
      String[] arr = split[i].split(":");
      if (arr.length < 2) {
        break;
      }

      String key = arr[0];
      String value = arr[1];

      switch (key) {
        case "name":
          name = value;
          break;
        case "length":
          length = Double.parseDouble(value);
          break;
        case "catcher":
          catcher = plugin.getServer().getOfflinePlayer(UUID.fromString(value));
          break;
      }
    }

    if (name == null) {
      return null;
    }

    CustomFish fish = getCustomFish(name);
    if (fish == null) {
      return null;
    }

    return new CaughtFish(fish, length, catcher);
  }

  private List<Biome> getBiomes(String[] values) {
    List<Biome> biomes = new ArrayList<>();
    for (int i=1; values.length < i; i++) {
      try {
        biomes.add(Biome.valueOf(values[i].toUpperCase()));
      } catch (Exception e) {
        plugin.getLogger().severe("Error! Fish has invalid biome condition '" + values[i] + "'!");
      }
    }
    return biomes;
  }

  private double getTotalRarity(int level) {
    double total = 0;
    for (Rarity r : rarityList) {
      total += r.getWeight() + r.getBonusWeight() * level;
    }
    return total;
  }

  private double getAdjustedWeight(Rarity rarity, int bonus) {
    return rarity.getWeight() + rarity.getBonusWeight() * bonus;
  }

  private double getLuckFromPlayer(Player player) {
    ItemStack mainHand = player.getEquipment().getItemInMainHand();
    ItemStack offHand = player.getEquipment().getItemInOffHand();
    if (mainHand != null && mainHand.getType() == Material.FISHING_ROD) {
      return getTotalLuck(mainHand);
    }
    if (offHand != null && offHand.getType() == Material.FISHING_ROD) {
      return getTotalLuck(offHand);
    }
    return 0;
  }

  private double getTotalLuck(ItemStack itemStack) {
    double minLuck = itemStack.getEnchantmentLevel(Enchantment.LUCK) * 3;
    double bonusLuck = random.nextDouble() * itemStack.getEnchantmentLevel(Enchantment.LUCK) * 2;
    return minLuck + bonusLuck;
  }
}
