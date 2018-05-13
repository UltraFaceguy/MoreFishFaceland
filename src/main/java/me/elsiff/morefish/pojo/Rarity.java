package me.elsiff.morefish.pojo;

import org.bukkit.ChatColor;

public class Rarity {
    private final String name;
    private final String displayName;
    private final double weight;
    private final ChatColor color;
    private final double additionalPrice;
    private final boolean noBroadcast;
    private final boolean noDisplay;
    private final boolean firework;

    public Rarity(Rarity rarity, double weight) {
        this(rarity.name, rarity.displayName, weight, rarity.color, rarity.additionalPrice,
                rarity.noBroadcast, rarity.noDisplay, rarity.firework);
    }

    public Rarity(String name, String displayName, double weight, ChatColor color, double additionalPrice,
                  boolean noBroadcast, boolean noDisplay, boolean firework) {
        this.name = name;
        this.displayName = displayName;
        this.weight = weight;
        this.color = color;
        this.additionalPrice = additionalPrice;
        this.noBroadcast = noBroadcast;
        this.noDisplay = noDisplay;
        this.firework = firework;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getWeight() {
        return weight;
    }

    public ChatColor getColor() {
        return color;
    }

    public double getAdditionalPrice() {
        return additionalPrice;
    }

    public boolean isNoBroadcast() {
        return noBroadcast;
    }

    public boolean isNoDisplay() {
        return noDisplay;
    }

    public boolean hasFirework() {
        return firework;
    }
}
