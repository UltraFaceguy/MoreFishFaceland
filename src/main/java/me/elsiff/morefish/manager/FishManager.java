package me.elsiff.morefish.manager;

import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang.WordUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import land.face.learnin.LearninBooksPlugin;
import land.face.learnin.objects.LoadedKnowledge;
import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.util.PlayerDataUtil;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.condition.Condition;
import me.elsiff.morefish.condition.ContestCondition;
import me.elsiff.morefish.condition.EnchantmentCondition;
import me.elsiff.morefish.condition.FishingSkillCondition;
import me.elsiff.morefish.condition.HeightCondition;
import me.elsiff.morefish.condition.LevelCondition;
import me.elsiff.morefish.condition.NearbyBlockCondition;
import me.elsiff.morefish.condition.PotionEffectCondition;
import me.elsiff.morefish.condition.RainingCondition;
import me.elsiff.morefish.condition.ThunderingCondition;
import me.elsiff.morefish.condition.TimeCondition;
import me.elsiff.morefish.hooker.WorldGuardHooker;
import me.elsiff.morefish.pojo.CaughtFish;
import me.elsiff.morefish.pojo.CustomFish;
import me.elsiff.morefish.pojo.FishZone;
import me.elsiff.morefish.pojo.Rarity;
import me.elsiff.morefish.util.IdentityUtils;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class FishManager {

  private final MoreFish plugin;
  private final Random random = new Random();
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

  private final Map<String, Rarity> rarityMap = new HashMap<>();
  private final Map<String, FishZone> fishZoneMap = new HashMap<>();
  private final Map<String, CustomFish> fishMap = new HashMap<>();

  private final Map<Integer, Double> totalWeightMap = new HashMap<>();

  private final double smallestRarityWeight;

  public FishManager(MoreFish plugin) {
    this.plugin = plugin;
    fishZoneMap.clear();
    fishMap.clear();
    rarityMap.clear();

    loadZones(plugin.getFishConfiguration().getFishZoneYaml());
    plugin.getLogger().info("Loaded " + fishZoneMap.size() + " zones successfully.");

    loadRarities(plugin.getConfig());
    plugin.getLogger().info("Loaded " + rarityMap.size() + " fish rarities successfully.");

    loadFish();
    plugin.getLogger().info("Loaded " + fishMap.size() + " fish successfully.");

    buildFishKnowledge();

    double newSmallest = Integer.MAX_VALUE;
    for (Rarity r : rarityMap.values()) {
      if (r.getWeight() < newSmallest) {
        newSmallest = r.getWeight();
      }
      Set<CustomFish> fishes = new HashSet<>(fishMap.values());
      filterByRarity(fishes, r);
      for (Biome b : Biome.values()) {
        Set<CustomFish> fishes2 = new HashSet<>(fishes);
        filterByBiome(fishes2, b);
        if (fishes2.size() < 2) {
          Bukkit.getLogger().warning("Only " + fishes2.size() + " possible outcomes for biome:" + b + " with rarity:" + r.getId());
        }
      }
    }
    smallestRarityWeight = newSmallest;
  }

  private void loadZones(FileConfiguration config) {
    ConfigurationSection zones = config.getConfigurationSection("biome-zones");
    for (String key : zones.getKeys(false)) {
      FishZone zone = new FishZone();
      zone.setId(key);
      zone.setDescription(zones.getString(key + ".description", "Uhhhhhhhhh??"));
      for (String s : zones.getStringList(key + ".biomes")) {
        zone.getBiome().add(Biome.valueOf(s));
      }
      fishZoneMap.put(key, zone);
    }
  }

  private void loadRarities(FileConfiguration config) {
    ConfigurationSection rarities = config.getConfigurationSection("rarity");

    for (String key : rarities.getKeys(false)) {
      String displayName = rarities.getString(key + ".text");
      double weight = rarities.getDouble(key + ".weight");
      double xpMult = rarities.getDouble(key + ".xp");
      FaceColor color = FaceColor.valueOf(rarities.getString(key + ".color").toUpperCase());
      boolean broadcast = rarities.getBoolean(key + ".broadcast", false);
      int baseTicksLived = rarities.getInt(key + ".base-ticks-lived", 1);

      Rarity rarity = new Rarity(key, displayName, weight, xpMult, broadcast, baseTicksLived, color);
      rarityMap.put(key, rarity);
    }
  }

  private void loadFish() {
    File fishFolder = new File(plugin.getDataFolder(), "fish");
    File[] files = fishFolder.listFiles();
    for (File f : files) {
      String fileName = f.getName().replace(".yml", "");

      if (!rarityMap.containsKey(fileName)) {
        Bukkit.getLogger().warning("[FaceFish] Fish file " + fileName +
            " does not match an existing rarity. Skipping....");
        continue;
      }
      YamlConfiguration config = new YamlConfiguration();
      try {
        config.load(f);
      } catch (Exception e) {
        continue;
      }
      ConfigurationSection section = config.getConfigurationSection("");
      for (String fishId : section.getKeys(false)) {
        Rarity rarity = rarityMap.get(fileName);
        CustomFish fish = createCustomFish(section, fishId, rarity);
        fishMap.put(fishId, fish);
      }
    }
  }

  private void buildFishKnowledge() {
    LearninBooksPlugin.instance.getKnowledgeManager().purgeKnowledge("more-fish");
    List<LoadedKnowledge> knowledges = new ArrayList<>();
    for (CustomFish fish : fishMap.values()) {
      String name = FaceColor.CYAN + fish.getName();

      // LORE 1 BUILD
      String lore1 = StringUtils.join(PaletteUtil.color(List.of(
          "|teal||b||ul|" + fish.getName(),
          "",
          "|black|Rarity: |none|" + fish.getRarity().getDisplayName(),
          "",
          "|black|Min. Size: " + fish.getLengthMin() + "cm",
          "|black|Max. Size: " + fish.getLengthMax() + "cm"
      )), "\n");

      // LORE 2 BUILD
      List<String> lore2builder = new ArrayList<>(List.of(
          "|teal||b||ul|" + fish.getName(),
          "",
          "|black|Water Conditions:"
      ));
      for (String s : fish.getBiomeDescs()) {
        lore2builder.add("|black|- " + s);
      }
      lore2builder.add("");
      lore2builder.add("|black|Specific Regions:");
      if (fish.getRegions().isEmpty()) {
        lore2builder.add("|black|- None");
      } else {
        for (String s : fish.getRegions()) {
          lore2builder.add("|black|- " + WordUtils.capitalizeFully(s.replace("-", " ").replace("_", " ")));
        }
      }
      String lore2 = StringUtils.join(PaletteUtil.color(lore2builder), "\n");

      // LORE 3 BUILD
      List<String> lore3builder = new ArrayList<>(List.of(
          "|teal||b||ul|" + fish.getName(),
          "",
          "|black|Catch Conditions:"
      ));
      for (Condition condition : fish.getConditions()) {
        lore3builder.add("|black|- " + condition.getDescription());
      }
      lore3builder.addAll(List.of("",
          "|black|Cooking Recipes:",
          "|black|- None"
      ));
      String lore3 = StringUtils.join(PaletteUtil.color(lore3builder), "\n");

      // UI BUILD
      List<String> desc = new ArrayList<>();
      desc.add("");
      desc.add("&fKnowledge Type: &bFish");
      desc.add("");
      desc.add("&7Click to view what you've");
      desc.add("&7learned from this fish!");
      desc.add("");
      LoadedKnowledge fishInfo = new LoadedKnowledge(
          fish.getId(), name, 500 + (int) fish.getLengthMax(), 1, 5, 25,
          lore1, lore2, lore3, TextUtils.color(desc));
      fishInfo.setSource("more-fish");
      fishInfo.setCategory("fish");
      knowledges.add(fishInfo);
    }
    LearninBooksPlugin.instance.getKnowledgeManager().addExternalKnowledge(knowledges);
  }

  private CustomFish createCustomFish(ConfigurationSection section, String key, Rarity rarity) {
    String displayName = section.getString(key + ".display-name", "");
    double lengthMin = section.getDouble(key + ".length-min");
    double lengthMax = section.getDouble(key + ".length-max");
    int modelData = section.getInt(key + ".model-data");
    List<String> lore = section.getStringList(key + ".lore");
    List<String> location = section.getStringList(key + ".location");
    Set<String> regions = new HashSet<>(section.getStringList(key + ".region"));

    Set<Biome> biomes = new HashSet<>();
    Set<String> biomeDescs = new HashSet<>();
    for (String s : location) {
      if (fishZoneMap.containsKey(s)) {
        biomes.addAll(fishZoneMap.get(s).getBiome());
        biomeDescs.add(fishZoneMap.get(s).getDescription());
      } else {
        Bukkit.getLogger().warning("[FaceFish] Invalid location for fish " + key);
      }
    }

    List<String> commands = new ArrayList<>();
    List<Condition> conditions = new ArrayList<>();

    if (section.contains(key + ".command")) {
      commands.addAll(section.getStringList(key + ".command"));
    }
    if (section.contains(key + ".commands")) {
      commands.addAll(section.getStringList(key + ".commands"));
    }

    if (section.contains(key + ".conditions")) {
      List<String> list = section.getStringList(key + ".conditions");

      for (String content : list) {
        Condition condition = getCondition(content);
        conditions.add(condition);
      }
    }

    return new CustomFish(key, displayName, lengthMin, lengthMax, modelData, lore,
        commands, conditions, rarity, biomes, biomeDescs, regions);
  }

  private Condition getCondition(String content) {
    Condition condition;
    String[] values = content.split("\\|");
    String conId = values[0];

    switch (conId) {
      case "raining" -> {
        boolean raining = Boolean.parseBoolean(values[1]);
        condition = new RainingCondition(raining);
      }
      case "thundering" -> {
        boolean thundering = Boolean.parseBoolean(values[1]);
        condition = new ThunderingCondition(thundering);
      }
      case "time" -> {
        String time = values[1].toLowerCase();
        condition = new TimeCondition(time);
      }
      case "enchantment" -> {
        Enchantment ench = IdentityUtils.getEnchantment(values[1].toLowerCase());
        int lv = Integer.parseInt(values[2]);
        condition = new EnchantmentCondition(ench, lv);
      }
      case "level" -> {
        int level = Integer.parseInt(values[1]);
        condition = new LevelCondition(level);
      }
      case "contest" -> {
        boolean ongoing = Boolean.parseBoolean(values[1]);
        condition = new ContestCondition(ongoing);
      }
      case "nearby_blocks" -> condition = new NearbyBlockCondition(getMaterials(values));
      case "potioneffect" -> {
        PotionEffectType effectType = IdentityUtils.getPotionEffectType(values[1]);
        int amplfier = Integer.parseInt(values[2]);
        condition = new PotionEffectCondition(effectType, amplfier);
      }
      case "height" -> {
        int minHeight = Integer.parseInt(values[1]);
        int maxHeight = Integer.parseInt(values[2]);
        condition = new HeightCondition(minHeight, maxHeight);
      }
      case "fishing_skill" -> {
        int skill = Integer.parseInt(values[1]);
        condition = new FishingSkillCondition(skill);
      }
      default -> {
        return null;
      }
    }
    return condition;
  }

  public void filterByBiome(Collection<CustomFish> fish, Biome biome) {
    fish.removeIf(cf -> !cf.getBiomes().contains(biome));
  }

  public void filterByRarity(Collection<CustomFish> fish, Rarity rarity) {
    fish.removeIf(cf -> cf.getRarity() != rarity);
  }

  public void filterByConditions(Collection<CustomFish> fish, Location loc, Player player) {
    fish.removeIf(cf -> {
      for (Condition condition : cf.getConditions()) {
        if (!condition.isSatisfying(player, loc)) {
          return true;
        }
      }
      return false;
    });
  }

  public void filterByRegion(Collection<CustomFish> fish, Location loc) {
    WorldGuardHooker hooker = MoreFish.getInstance().getWorldGuardHooker();
    fish.removeIf(cf -> {
      if (cf.getRegions().isEmpty()) {
        return false;
      }
      for (String s : cf.getRegions()) {
        if (hooker.containsLocation(loc, s)) {
          return false;
        }
      }
      return true;
    });
  }

  public CaughtFish generateRandomFish(Player catcher, Location location) {
    // TODO: Only set level if strife is loaded
    double rodLuck = getLuckFromPlayer(catcher);
    Rarity rarity = getRandomRarity(PlayerDataUtil.getSkillLevels(catcher,
        LifeSkillType.FISHING, true).getLevelWithBonus(), rodLuck);

    List<CustomFish> fishes = new ArrayList<>(fishMap.values());
    filterByRarity(fishes, rarity);
    filterByBiome(fishes, location.getBlock().getBiome());
    filterByRegion(fishes, location);
    filterByConditions(fishes, location, catcher);

    if (fishes.size() < 1) {
      Bukkit.getLogger().warning("[FaceFish] No fish found!? Biome:" + location.getBlock().getBiome() + " Rarity:" + rarity.getId());
      return null;
    }

    CustomFish fish = fishes.get(random.nextInt(fishes.size()));
    return createCaughtFish(fish, catcher, catcher.hasPotionEffect(PotionEffectType.LUCK));
  }

  public Rarity getRarity(String id) {
    return rarityMap.get(id);
  }

  public CustomFish getRandomFish(Rarity rarity) {
    List<CustomFish> fishes = new ArrayList<>(fishMap.values());
    filterByRarity(fishes, rarity);
    Collections.shuffle(fishes);
    return fishes.get(0);
  }

  public CustomFish getCustomFish(String name) {
    return fishMap.get(name);
  }

  public ItemStack buildItemFromFish(CaughtFish fish, String fisher) {

    ItemStack itemStack = new ItemStack(Material.KELP);
    Rarity rarity = fish.getFish().getRarity();

    String name = rarity.getColor() + fish.getFish().getName() + " (" + fish.getLength() + "cm)";
    List<String> lore = new ArrayList<>();
    lore.add("|white|" + rarity.getDisplayName() + "ɾ");
    lore.add("");
    lore.add("|yellow|Caught By: |white|" + fisher);
    lore.add("|yellow|Catch Date: |white|" + dateFormat.format(new Date()));
    if (fish.getFish().getLore().size() > 0) {
      lore.add("");
      lore.addAll(fish.getFish().getLore());
    }
    TextUtils.setLore(itemStack, PaletteUtil.color(lore), false);
    ItemStackExtensionsKt.setDisplayName(itemStack, name);
    ItemStackExtensionsKt.setCustomModelData(itemStack, fish.getFish().getModelData());

    // Ensure no stacking
    itemStack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    ItemStackExtensionsKt.addAttributeModifier(itemStack, Attribute.GENERIC_ARMOR_TOUGHNESS,
        new AttributeModifier(UUID.randomUUID(), "sneed", Math.random(), Operation.ADD_NUMBER));
    return itemStack;
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

  private Rarity getRandomRarity(double skillLevel, double luck) {
    double bonus = skillLevel + luck;
    double cur = 0.0D;
    double randomVar = random.nextDouble() * getTotalRarity(bonus);
    double multiplier = 1 + (Math.floor(bonus) / 100);
    for (Rarity rarity : rarityMap.values()) {
      cur += rarity.getWeight() + smallestRarityWeight * multiplier;
      if (cur >= randomVar) {
        return rarity;
      }
    }
    return null;
  }

  private Set<Material> getMaterials(String[] values) {
    Set<Material> materials = new HashSet<>();
    for (int i = 1; i < values.length; i++) {
      try {
        materials.add(Material.valueOf(values[i].toUpperCase()));
      } catch (Exception e) {
        plugin.getLogger()
            .severe("Error! Fish has invalid material condition '" + values[i] + "'!");
      }
    }
    return materials;
  }

  private double getTotalRarity(double bonus) {
    int level = (int) bonus;
    if (totalWeightMap.containsKey(level)) {
      return totalWeightMap.get(level);
    }
    double total = 0;
    double multiplier = 1 + (Math.floor(bonus) / 100);
    for (Rarity r : rarityMap.values()) {
      total += r.getWeight() + smallestRarityWeight * multiplier;
    }
    totalWeightMap.put(level, total);
    return total;
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
