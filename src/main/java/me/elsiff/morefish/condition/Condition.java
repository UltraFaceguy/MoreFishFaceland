package me.elsiff.morefish.condition;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Condition {
    boolean isSatisfying(Player player, Location fishLocation);

    String getDescription();
}
