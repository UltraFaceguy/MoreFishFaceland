package me.elsiff.morefish.command;

import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.manager.ContestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GeneralCommands implements CommandExecutor, TabCompleter {

  private final MoreFish plugin;
  private final ContestManager contest;

  public GeneralCommands(MoreFish plugin) {
    this.plugin = plugin;
    this.contest = plugin.getContestManager();
  }

  public List<String> onTabComplete(CommandSender sender, Command cmd, String label,
      String[] args) {
    List<String> list = new ArrayList<>();

    if (args.length < 2) {
      if (sender.hasPermission("morefish.admin")) {
        list.add("help");
        list.add("start");
        list.add("stop");
        list.add("clear");
        list.add("rewards");
      }

      if (sender.hasPermission("morefish.top")) {
        list.add("top");
      }

      if (sender.hasPermission("morefish.shop")) {
        list.add("shop");
      }
    }

    String finalArg = args[args.length - 1];
    Iterator<String> it = list.iterator();
    while (it.hasNext()) {
      if (!it.next().startsWith(finalArg)) {
        it.remove();
      }
    }

    return list;
  }

  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.help")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      String prefix = "§b[MoreFish]§r ";
      sender.sendMessage(
          prefix + "§3> ===== §b§lMoreFish §bv" + plugin.getDescription().getVersion()
              + "§3 ===== <");
      sender.sendMessage(prefix + "/" + label + " help");
      sender.sendMessage(prefix + "/" + label + " start [runningTime (sec)]");
      sender.sendMessage(prefix + "/" + label + " stop");
      sender.sendMessage(prefix + "/" + label + " rewards");
      sender.sendMessage(prefix + "/" + label + " clear");
      sender.sendMessage(prefix + "/" + label + " reload");
      sender.sendMessage(prefix + "/" + label + " top");
      sender.sendMessage(prefix + "/" + label + " shop [player]");

      return true;
    } else if ("start".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.admin")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      if (contest.hasStarted()) {
        sender.sendMessage(plugin.getFishConfiguration().getString("already-ongoing"));
        return true;
      }

      boolean hasTimer = false;
      long sec = 0;

      if (args.length > 1) {
        try {
          sec = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
          sender.sendMessage(
              String.format(plugin.getFishConfiguration().getString("not-number"), args[1]));
          return true;
        }

        if (sec <= 0) {
          sender.sendMessage(plugin.getFishConfiguration().getString("not-positive"));
          return true;
        }

        hasTimer = true;
        contest.startWithTimer(sec);
      } else {
        contest.start();
      }

      String msg = plugin.getFishConfiguration().getString("contest-start");
      boolean broadcast = plugin.getConfig().getBoolean("messages.broadcast-start");

      if (broadcast) {
        plugin.getServer().broadcastMessage(msg);
      } else {
        sender.sendMessage(msg);
      }

      if (hasTimer) {
        String msgTimer = plugin.getFishConfiguration().getString("contest-start-timer")
            .replaceAll("%sec%", Long.toString(sec))
            .replaceAll("%time%", plugin.getTimeString(sec));

        if (broadcast) {
          plugin.getServer().broadcastMessage(msgTimer);
        } else {
          sender.sendMessage(msgTimer);
        }
      }

      return true;
    } else if ("stop".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.admin")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      if (!contest.hasStarted()) {
        sender.sendMessage(plugin.getFishConfiguration().getString("already-stopped"));
        return true;
      }

      String msg = plugin.getFishConfiguration().getString("contest-stop");
      boolean showRanking = plugin.getConfig().getBoolean("messages.show-top-on-ending");
      boolean broadcast = plugin.getConfig().getBoolean("messages.broadcast-stop");

      if (broadcast) {
        plugin.getServer().broadcastMessage(msg);
      } else {
        sender.sendMessage(msg);
      }

      if (showRanking) {
        sendRankingMessage(sender, broadcast);
      }

      contest.stop();

      return true;
    } else if ("clear".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.admin")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      if (!contest.hasStarted()) {
        sender.sendMessage(plugin.getFishConfiguration().getString("not-ongoing"));
        return true;
      }

      contest.clearRecords();

      sender.sendMessage(plugin.getFishConfiguration().getString("clear-records"));

      return true;
    } else if ("reload".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.admin")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      plugin.reloadConfig();
      boolean loaded = plugin.getFishConfiguration().loadFiles();

      if (!loaded) {
        sender.sendMessage(plugin.getFishConfiguration().getString("failed-to-reload"));
        return true;
      }

      plugin.onDisable();
      plugin.onEnable();

      sender.sendMessage(plugin.getFishConfiguration().getString("reload-config"));

      return true;
    } else if ("top".equalsIgnoreCase(args[0])) {
      if (!sender.hasPermission("morefish.top")) {
        sender.sendMessage(plugin.getFishConfiguration().getString("no-permission"));
        return true;
      }

      if (!contest.hasStarted()) {
        sender.sendMessage(plugin.getFishConfiguration().getString("not-ongoing"));
        return true;
      }

      if (contest.getRecordAmount() < 1) {
        String msg = plugin.getFishConfiguration().getString("top-no-record");
        sender.sendMessage(msg);
      } else {
        sendRankingMessage(sender, false);
      }

      return true;
    } else {
      sender.sendMessage(plugin.getFishConfiguration().getString("invalid-command"));
      return true;
    }
  }

  private Player getPlayer(String name) {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (player.getName().equalsIgnoreCase(name)) {
        return player;
      }
    }
    return null;
  }

  private void sendRankingMessage(CommandSender sender, boolean broadcast) {
    String format = plugin.getFishConfiguration().getString("top-list");
    int limit = plugin.getConfig().getInt("messages.top-number");

    for (int i = 1; i < limit + 1; i++) {
      ContestManager.Record record = contest.getRecord(i);
      if (record == null) {
        break;
      }
      String msg = format.replaceAll("%ordinal%", plugin.getOrdinal(i))
          .replaceAll("%number%", Integer.toString(i))
          .replaceAll("%player%", record.getName())
          .replaceAll("%length%", record.getLength() + "")
          .replaceAll("%fish%", record.getFishName());
      if (broadcast) {
        plugin.getServer().broadcastMessage(msg);
      } else {
        sender.sendMessage(msg);
      }
    }

    if (broadcast) {
      for (Player player : plugin.getServer().getOnlinePlayers()) {
        sendCurrentPositionMessage(player);
      }
    } else {
      if (sender instanceof Player player) {
        sendCurrentPositionMessage(player);
      }
    }
  }

  private void sendCurrentPositionMessage(Player player) {
    String msg;
    if (plugin.getContestManager().hasRecord(player)) {
      int number = contest.getNumber(player);
      ContestManager.Record record = contest.getRecord(number);
      msg = plugin.getFishConfiguration().getString("top-mine")
          .replaceAll("%ordinal%", plugin.getOrdinal(number))
          .replaceAll("%number%", Integer.toString(number))
          .replaceAll("%player%", record.getName())
          .replaceAll("%length%", record.getLength() + "")
          .replaceAll("%fish%", record.getFishName());
    } else {
      msg = plugin.getFishConfiguration().getString("top-mine-no-record");
    }
    player.sendMessage(msg);
  }
}
