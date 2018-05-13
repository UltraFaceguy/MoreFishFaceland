package me.elsiff.morefish.pojo;

import me.elsiff.morefish.MoreFish;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FishConfiguration {
    private final MoreFish plugin;
    private final File folder;
    private final String langPath;
    private final String fishPath;
    private final String rarityPath;
    private FileConfiguration lang;
    private FileConfiguration fish;
    private FileConfiguration rarity;

    public FishConfiguration(MoreFish plugin) {
        this.plugin = plugin;

        String locale = plugin.getConfig().getString("general.locale");
        folder = plugin.getDataFolder();
        langPath = "locale\\lang_" + locale + ".yml";
        fishPath = "fish.yml";
        rarityPath = "rarity.yml";

        loadFiles();
    }

    private FileConfiguration loadConfiguration(File folder, String path) throws IOException, InvalidConfigurationException {
        File file = new File(folder, path);

        if (!file.exists()) {
            plugin.saveResource(path, false);
        }

        YamlConfiguration config = new YamlConfiguration();
        config.load(file);

        return config;
    }

    public FileConfiguration getLangConfig() {
        return lang;
    }

    public FileConfiguration getFishConfig() {
        return fish;
    }

    public FileConfiguration getRarityConfig() {
        return rarity;
    }

    public boolean loadFiles() {
        try {
            this.lang = loadConfiguration(folder, langPath);
            this.fish = loadConfiguration(folder, fishPath);
            this.rarity = loadConfiguration(folder, rarityPath);
            return true;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe(e.getMessage());
            return false;
        }
    }

    public String getString(String path) {
        String value = lang.getString(path);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public List<String> getStringList(String path) {
        List<String> list = new ArrayList<>();

        for (String value : lang.getStringList(path)) {
            list.add(ChatColor.translateAlternateColorCodes('&', value));
        }

        return list;
    }

    public int getLangVersion() {
        return lang.getInt("version");
    }

    public int getFishVersion() {
        return fish.getInt("version");
    }

    public int getRarityVersion() {
        return rarity.getInt("version");
    }

    public String getLangPath() {
        return langPath;
    }

    public String getFishPath() {
        return fishPath;
    }

    public String getRarityPath() {
        return rarityPath;
    }
}
