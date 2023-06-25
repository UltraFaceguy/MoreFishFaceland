package me.elsiff.morefish.manager;

import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import land.face.mail.MailTimePlugin;
import land.face.mail.pojo.ManagedLetter;
import land.face.strife.StrifePlugin;
import land.face.strife.managers.GuiManager;
import lombok.Getter;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.pojo.CaughtFish;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ContestManager {

  private final MoreFish plugin;
  private final RecordComparator comparator = new RecordComparator();
  private final List<Record> recordList = new ArrayList<>();
  private final File fileRewards;
  private final FileConfiguration configRewards;
  private boolean hasStarted = false;
  private TimerTask task = null;

  public ContestManager(MoreFish plugin) {
    this.plugin = plugin;
    if (plugin.getConfig().getBoolean("general.auto-start")) {
      hasStarted = true;
    }
    fileRewards = new File(plugin.getDataFolder(), "rewards.yml");
    createFile(fileRewards);
    configRewards = YamlConfiguration.loadConfiguration(fileRewards);
  }

  private void createFile(File file) {
    if (!file.exists()) {
      try {
        boolean created = file.createNewFile();

        if (!created) {
          plugin.getLogger().warning("Failed to create " + file.getName() + "!");
        }
      } catch (IOException e) {
        plugin.getLogger().severe(e.getMessage());
      }
    }
  }

  public boolean hasStarted() {
    return hasStarted;
  }

  public boolean hasTimer() {
    return (task != null);
  }

  public void start() {
    hasStarted = true;
  }

  public void startWithTimer(long sec) {
    task = new TimerTask(sec);
    task.runTaskTimer(plugin, 20, 20);
    start();
  }

  public void stop() {
    if (task != null) {
      task.cancel();
      task = null;
    }
    giveRewards();
    recordList.clear();
    hasStarted = false;
  }

  private void giveRewards() {
    int ranking = 1;
    for (Record record : recordList) {
      if (ranking > 3) {
        break;
      }
      String letterId = "fish-reward-" + ranking;
      ranking++;
      ManagedLetter letter = MailTimePlugin.getInstance().getLetterManager().getManagedLetter(letterId);
      if (letter == null) {
        Bukkit.getLogger().info("[MoreFish] Failed to find letter " + letterId + ", it's null.");
        return;
      }
      Player p = Bukkit.getPlayer(record.getUuid());
      if (p != null && p.isOnline()) {
        MailTimePlugin.getInstance().getLetterManager().sendManagedLetter(letter, p, () ->
            PaletteUtil.sendMessage(p, "|lime|Your contest rewards have been sent to your mailbox!"));
      }
    }
  }

  public boolean isNew1st(CaughtFish fish) {
    Record record = getRecord(1);
    return (record == null || record.getLength() < fish.getLength());
  }

  public void addRecord(Player player, CaughtFish fish) {
    for (Record r : recordList) {
      if (player.getUniqueId().equals(r.getUuid())) {
        if (!(r.getLength() >= fish.getLength())) {
          r.replace(fish);
          recordList.sort(comparator);
        }
        return;
      }
    }
    recordList.add(new Record(player, fish));
    recordList.sort(comparator);
  }

  public Record getRecord(int number) {
    if (recordList.size() == 0) {
      return null;
    }
    return ((recordList.size() >= number) ? recordList.get(number - 1) : null);
  }

  public int getRecordAmount() {
    return recordList.size();
  }

  public boolean hasRecord(OfflinePlayer player) {
    for (Record record : recordList) {
      if (record.getUuid().equals(player)) {
        return true;
      }
    }

    return false;
  }

  public double getRecordLength(Player player) {
    for (Record record : recordList) {
      if (record.getUuid().equals(player)) {
        return record.getLength();
      }
    }
    return 0.0D;
  }

  public Record getTopRecord() {
    if (recordList.size() == 0) {
      return null;
    }
    return recordList.get(0);
  }

  public int getNumber(OfflinePlayer player) {
    for (int i = 0; i < recordList.size(); i++) {
      if (recordList.get(i).getUuid().equals(player.getUniqueId())) {
        return (i + 1);
      }
    }
    return 0;
  }

  public void clearRecords() {
    recordList.clear();
  }

  private class RecordComparator implements Comparator<Record> {

    public int compare(Record arg0, Record arg1) {
      if (arg0.getLength() < arg1.getLength()) {
        return 1;
      } else if ((arg0.getLength() > arg1.getLength())) {
        return -1;
      }
      return 0;
    }
  }

  public class Record {

    @Getter
    private final UUID uuid;
    @Getter
    private final String name;
    @Getter
    private CaughtFish fish;

    public Record(Player player, CaughtFish fish) {
      this.uuid = player.getUniqueId();
      this.name = player.getName();
      this.fish = fish;
    }

    public String getFishName() {
      return fish.getFish().getName();
    }

    public double getLength() {
      return fish.getLength();
    }

    public void replace(CaughtFish newFish) {
      fish = newFish;
    }
  }

  private class TimerTask extends BukkitRunnable {

    private final long timer;
    private long passed = 0;
    private String latestBar = "";
    private int latestStage = -1;

    public TimerTask(long sec) {
      this.timer = sec;
    }

    public void run() {
      passed++;

      long left = timer - passed;
      String title = plugin.getFishConfiguration().getString("timer-boss-bar")
          .replaceAll("%time%", plugin.getTimeString(left));

      double progress = (double) left / timer;
      int stage = (int) (138D * progress);
      if (stage != latestStage) {
        latestStage = stage;
        latestBar = "♅" + GuiManager.HEALTH_BAR_TARGET.get(138 - stage);
      }
      boolean finished = passed >= timer;

      for (Player p : Bukkit.getOnlinePlayers()) {
        if (finished) {
          StrifePlugin.getInstance().getBossBarManager().updateBar(p, 2, 3, "", 0);
          StrifePlugin.getInstance().getBossBarManager().updateBar(p, 3, 3, "", 0);
        } else {
          StrifePlugin.getInstance().getBossBarManager().updateBar(p, 2, 3, title, 25);
          StrifePlugin.getInstance().getBossBarManager().updateBar(p, 3, 3, latestBar, 25);
        }
      }

      if (finished) {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "morefish stop");
        this.cancel();
      }
    }
  }
}
