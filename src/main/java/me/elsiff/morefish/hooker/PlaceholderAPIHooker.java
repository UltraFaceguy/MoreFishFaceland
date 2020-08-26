package me.elsiff.morefish.hooker;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.manager.ContestManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHooker extends PlaceholderExpansion {

  private final ContestManager contest;

  public PlaceholderAPIHooker(MoreFish plugin) {
    this.contest = plugin.getContestManager();
  }

  @Override
  public @NotNull String getAuthor() {
    return "Faceguy";
  }

  @Override
  public @NotNull String getIdentifier() {
    return "fish";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0.0";
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public String onPlaceholderRequest(Player p, @NotNull String placeholder) {
    if (placeholder.startsWith("top_player_")) {
      if (!contest.hasStarted()) {
        return "";
      }
      int number = Integer.parseInt(placeholder.substring(11));

      if (number > contest.getRecordAmount()) {
        return "";
      }
      ContestManager.Record record = contest.getRecord(number);

      return record.getPlayer().getName();
    } else if (placeholder.startsWith("top_fish_")) {
      if (!contest.hasStarted()) {
        return "";
      }
      int number = Integer.parseInt(placeholder.substring(9));

      if (number > contest.getRecordAmount()) {
        return "";
      }
      ContestManager.Record record = contest.getRecord(number);

      return record.getFishName();
    } else if (placeholder.startsWith("top_length_")) {
      if (!contest.hasStarted()) {
        return "";
      }
      int number = Integer.parseInt(placeholder.substring(11));

      if (number > contest.getRecordAmount()) {
        return "";
      }
      ContestManager.Record record = contest.getRecord(number);

      return record.getLength() + "";
    } else if (placeholder.startsWith("rank") && p != null) {
      if (!contest.hasStarted()) {
        return "";
      }

      return (contest.hasRecord(p) ? contest.getNumber(p) : 0) + "";
    }

    return null;
  }
}