package com.johnymuffin.autorankup.beta;

import com.johnymuffin.jstats.beta.JStats;
import com.johnymuffin.jstats.beta.PPlayer;
import com.johnymuffin.jstats.core.simplejson.JSONArray;
import com.johnymuffin.jstats.core.simplejson.JSONObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutomaticRankup extends JavaPlugin {
    //Basic Plugin Info
    private static AutomaticRankup plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;

    private ARConfig config;
    private ARCommandSender commandSender;

    private JStats jStats;
    private RankupHandler[] rankupHandlers;


    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("JStats")) {
            log.info("}---------------ERROR---------------{");
            log.info("This Plugin Requires JStats");
            log.info("}---------------ERROR---------------{");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        jStats = (JStats) Bukkit.getServer().getPluginManager().getPlugin("JStats");
        config = new ARConfig(new File(plugin.getDataFolder(), "config.json"));
        commandSender = new ARCommandSender(Bukkit.getServer());


        //Create Handlers
        JSONArray rawRankups = (JSONArray) plugin.getConfig().get("rankups");
        rankupHandlers = new RankupHandler[rawRankups.size()];
        int count = 0;
        for (Object rawRankup : rawRankups) {
            rankupHandlers[count] = new RankupHandler(plugin, (JSONObject) rawRankup);
            logger(Level.INFO, "Loaded a rankup called " + rankupHandlers[count].getRankupName() + " into memory.");
            count = count + 1;
        }


        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player bukkitPlayer : Bukkit.getOnlinePlayers()) {
                PPlayer player = getjStats().getPlayerMap().get(bukkitPlayer.getUniqueId());
                if (player == null) {
                    logger(Level.WARNING, "Unable to check if " + bukkitPlayer.getName() + " can be promoted as their stat profile isn't stored locally.");
                    continue;
                }
                for (RankupHandler handler : rankupHandlers) {
                    if (handler.isPlayerEligible(bukkitPlayer.getUniqueId(), jStats, bukkitPlayer)) {
                        handler.executeActions(bukkitPlayer.getUniqueId(), jStats, bukkitPlayer);
                        logger(Level.INFO, bukkitPlayer.getName() + " has met the requirements for " + handler.getRankupName() + " and the appropriate actions have been taken.");
                        break; //Only allow a player to get promoted once per check
                    }
                }
            }


        }, 20L, config.getConfigInteger("checkFrequency") * 20);

        logger(Level.INFO, "Is Loaded.");

    }

    @Override
    public void onDisable() {

    }

    public void logger(Level level, String message) {
        Bukkit.getLogger().log(level, "[" + pluginName + "] " + message);
    }


    public ARConfig getConfig() {
        return config;
    }

    public ARCommandSender getCommandSender() {
        return commandSender;
    }

    public JStats getjStats() {
        return jStats;
    }
}
