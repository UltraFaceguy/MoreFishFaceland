package me.elsiff.morefish.condition;

import me.elsiff.morefish.MoreFish;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ContestCondition implements Condition {
    private final boolean ongoing;

    public ContestCondition(boolean ongoing) {
        this.ongoing = ongoing;
    }

    @Override
    public boolean isSatisfying(Player player, Location fishLocation) {
        return (ongoing == MoreFish.getInstance().getContestManager().hasStarted());
    }

    @Override
    public String getDescription() {
        return ongoing ? "Contests only" : "Hides from contests";
    }
}