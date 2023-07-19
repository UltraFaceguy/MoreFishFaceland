package me.elsiff.morefish;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import land.face.strife.StrifePlugin;
import lombok.Getter;
import me.elsiff.morefish.command.GeneralCommands;
import me.elsiff.morefish.hooker.LootHooker;
import me.elsiff.morefish.hooker.PlaceholderAPIHooker;
import me.elsiff.morefish.hooker.StrifeHooker;
import me.elsiff.morefish.hooker.VaultHooker;
import me.elsiff.morefish.hooker.WorldGuardHooker;
import me.elsiff.morefish.listener.FishingListener;
import me.elsiff.morefish.listener.PlayerListener;
import me.elsiff.morefish.listener.TreasureListener;
import me.elsiff.morefish.manager.ContestManager;
import me.elsiff.morefish.manager.FishManager;
import me.elsiff.morefish.pojo.FishConfiguration;
import me.elsiff.morefish.protocol.UpdateChecker;
import me.joshuaemq.TogglePickupsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MoreFish extends JavaPlugin {

  private static MoreFish instance;
  private PluginManager manager;
  private int taskId = -1;

  private FishConfiguration fishConfiguration;
  private FishManager fishManager;
  private ContestManager contestManager;
  @Getter
  private FishingListener fishingListener;
  private UpdateChecker updateChecker;

  private VaultHooker vaultHooker;
  private PlaceholderAPIHooker placeholderAPIHooker;
  private WorldGuardHooker worldGuardHooker;
  private StrifeHooker strifeHooker;
  private LootHooker lootHooker;

  @Getter
  private TogglePickupsPlugin togglePickupsPlugin;

  public static void setInstance(MoreFish moreFish) {
    instance = moreFish;
  }

  public static MoreFish getInstance() {
    return instance;
  }

  @Override
  public void onEnable() {
    setInstance(this);

    saveDefaultConfig();
    this.fishConfiguration = new FishConfiguration(this);

    updateConfigFiles();

    this.fishManager = new FishManager(this);
    this.contestManager = new ContestManager(this);
    this.updateChecker = new UpdateChecker(this);

    getCommand("morefish").setExecutor(new GeneralCommands(this));

    manager = getServer().getPluginManager();
    if (manager.getPlugin("Vault") != null && manager.getPlugin("Vault").isEnabled()) {
      vaultHooker = new VaultHooker(this);
      if (vaultHooker.setupEconomy()) {
        getLogger().info("Found Vault for economy support.");
      } else {
        vaultHooker = null;
      }
    }
    if (manager.getPlugin("PlaceholderAPI") != null && manager.getPlugin("PlaceholderAPI")
        .isEnabled()) {
      placeholderAPIHooker = new PlaceholderAPIHooker(this);
      getLogger().info("Found PlaceholderAPI for placeholders support.");
    }
    if (manager.getPlugin("WorldGuard") != null && manager.getPlugin("WorldGuard").isEnabled()) {
      worldGuardHooker = new WorldGuardHooker();
      getLogger().info("Found WorldGuard for regions support.");
    }
    if (manager.getPlugin("Strife") != null && manager.getPlugin("Strife").isEnabled()) {
      strifeHooker = new StrifeHooker((StrifePlugin) manager.getPlugin("Strife"));
      getLogger().info("Found Strife for fishXP support.");
    }
    if (manager.getPlugin("Loot") != null && manager.getPlugin("Loot").isEnabled()) {
      lootHooker = new LootHooker();
      getLogger().info("Found Loot for treasure support.");
    }

    fishingListener = new FishingListener(this);
    manager.registerEvents(fishingListener, this);
    manager.registerEvents(new PlayerListener(this), this);
    manager.registerEvents(new TreasureListener(this), this);

    Plugin pickupPlugin = Bukkit.getServer().getPluginManager().getPlugin("TogglePickups");
    if (pickupPlugin != null) {
      togglePickupsPlugin = (TogglePickupsPlugin) pickupPlugin;
    }

    scheduleAutoRunning();
    getLogger().info("Plugin has been enabled!");
  }

  private void updateConfigFiles() {
    final int verConfig = 210;
    final int verLang = 211;
    final int verFish = 1;
    final int verRarity = 1;
    String msg = fishConfiguration.getString("old-file");
    ConsoleCommandSender console = getServer().getConsoleSender();
    if (getConfig().getInt("version") != verConfig) {
      // Update
      console.sendMessage(String.format(msg, "config.yml"));
    }
    if (fishConfiguration.getLangVersion() != verLang) {
      // Update
      console.sendMessage(String.format(msg, fishConfiguration.getLangPath()));
    }
    if (fishConfiguration.getFishVersion() != verFish) {
      // Update
      console.sendMessage(String.format(msg, fishConfiguration.getFishPath()));
    }
  }

  public void scheduleAutoRunning() {
    if (taskId != -1) {
      getServer().getScheduler().cancelTask(taskId);
    }

    if (getConfig().getBoolean("auto-running.enable")) {
      final int required = getConfig().getInt("auto-running.required-players");
      final long timer = getConfig().getLong("auto-running.timer");
      final List<String> startTime = getConfig().getStringList("auto-running.start-time");
      final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

      taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
        String now = dateFormat.format(new Date());

        if (startTime.contains(now) && getServer().getOnlinePlayers().size() >= required) {
          getServer().dispatchCommand(getServer().getConsoleSender(), "morefish start " + timer);
        }
      }, 0L, 1200L);
    }
  }

  @Override
  public void onDisable() {
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().cancelTasks(this);
    getServer().getScheduler().cancelTask(taskId);
    getLogger().info("[MoreFish] Plugin has been disabled!");
  }

  public FishConfiguration getFishConfiguration() {
    return fishConfiguration;
  }

  public FishManager getFishManager() {
    return fishManager;
  }

  public ContestManager getContestManager() {
    return contestManager;
  }

  public UpdateChecker getUpdateChecker() {
    return updateChecker;
  }

  public String getOrdinal(int number) {
    switch (number) {
      case 1 -> {
        return "1st";
      }
      case 2 -> {
        return "2nd";
      }
      case 3 -> {
        return "3rd";
      }
      default -> {
        if (number > 20) {
          return (number / 10) + getOrdinal(number % 10);
        } else {
          return number + "th";
        }
      }
    }
  }

  public String getTimeString(long sec) {
    StringBuilder builder = new StringBuilder();

    int minutes = (int) (sec / 60);
    int second = (int) (sec - minutes * 60);

    if (minutes > 0) {
      builder.append(minutes);
      builder.append(getFishConfiguration().getString("time-format-minutes"));
      builder.append(" ");
    }

    builder.append(second);
    builder.append(getFishConfiguration().getString("time-format-seconds"));

    return builder.toString();
  }

  public boolean hasEconomy() {
    return (vaultHooker != null);
  }

  public VaultHooker getVaultHooker() {
    return vaultHooker;
  }

  public PlaceholderAPIHooker getPlaceholderAPIHooker() {
    return placeholderAPIHooker;
  }

  public WorldGuardHooker getWorldGuardHooker() {
    return worldGuardHooker;
  }

  public StrifeHooker getStrifeHooker() {
    return strifeHooker;
  }

  public LootHooker getLootHooker() {
    return lootHooker;
  }
}
