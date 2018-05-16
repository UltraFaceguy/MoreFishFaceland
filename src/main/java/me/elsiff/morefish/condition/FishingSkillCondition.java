package me.elsiff.morefish.condition;

import info.faceland.strife.util.PlayerDataUtil;
import org.bukkit.entity.Player;

public class FishingSkillCondition implements Condition {
    private final int level;

    public FishingSkillCondition(int level) {
        this.level = level;
    }

    @Override
    public boolean isSatisfying(Player player) {
        return PlayerDataUtil.getFishLevel(player) >= level;
    }
}
