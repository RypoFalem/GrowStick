package io.github.rypofalem.growstick;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Level;

public class CustomConfig {
    private final File file;
    private final JavaPlugin plugin;
    private FileConfiguration config = null;

    public CustomConfig(String file, JavaPlugin plugin){
        this(new File(file), plugin);
    }

    public CustomConfig(File file, JavaPlugin plugin){
        if(file == null) throw new IllegalArgumentException("File cannot be null");
        this.file = file;
        if(plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(file);
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(plugin.getResource(file.getName()), "UTF8");
        } catch (UnsupportedEncodingException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + file.getName(), e);
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        if(config == null) reloadConfig();
        return config;
    }

    public void saveConfig() {
        if(config == null || file == null) return;
        try {
            getConfig().save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + file.getName(), e);
        }
    }

    public void saveDefaultConfig() {
        if(!file.exists()) plugin.saveResource(file.getName(), false);
    }
}
