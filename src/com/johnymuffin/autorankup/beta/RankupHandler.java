package com.johnymuffin.autorankup.beta;

import com.johnymuffin.jperms.beta.JohnyPerms;
import com.johnymuffin.jstats.beta.JStats;
import com.johnymuffin.jstats.beta.PPlayer;
import com.johnymuffin.jstats.core.simplejson.JSONArray;
import com.johnymuffin.jstats.core.simplejson.JSONObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class RankupHandler {

    private String rankupName;
    private JSONArray requirements;
    private JSONArray actions;
    private AutomaticRankup plugin;

    public RankupHandler(AutomaticRankup plugin, JSONObject raw) {
        this.plugin = plugin;
        this.requirements = (JSONArray) raw.getOrDefault("requirements", new JSONArray());
        this.actions = (JSONArray) raw.getOrDefault("actions", new JSONArray());
        this.rankupName = String.valueOf(raw.getOrDefault("rankupName", "unknown"));

    }


    public boolean isPlayerEligible(UUID uuid, JStats jStats, Player bukkitPlayer) {
        PPlayer player = jStats.getPlayerMap().get(uuid);
        if (player == null) {
            return false;
        }

        for (Object rawRequirement : requirements) {
            JSONObject requirement = (JSONObject) rawRequirement;
            if (!doesPlayerMeetRequirement(requirement, uuid, player, bukkitPlayer)) {
                return false;
            }
        }
        return true;
    }

    public void executeActions(UUID uuid, JStats jStats, Player bukkitPlayer) {
        for (Object rawAction : actions) {
            JSONObject action = (JSONObject) rawAction;
            String actionType = String.valueOf(action.get("type"));
            String message;
            switch (actionType) {
                case "broadcast":
                    message = String.valueOf(action.get("value"));
                    message = message.replace("%username%", bukkitPlayer.getName());
                    message = message.replace("%uuid%", String.valueOf(bukkitPlayer.getUniqueId()));
                    message = message.replace("&", "\u00a7");
                    Bukkit.broadcastMessage(message);
                    continue;
                case "command":
                    message = String.valueOf(action.get("value"));
                    message = message.replace("%username%", bukkitPlayer.getName());
                    message = message.replace("%uuid%", String.valueOf(bukkitPlayer.getUniqueId()));
                    Bukkit.getServer().dispatchCommand(plugin.getCommandSender(), message);
                    continue;
            }

            plugin.logger(Level.WARNING, "Unknown Action Type " + actionType + " for: " + rankupName);

        }
    }


    private boolean doesPlayerMeetRequirement(JSONObject requirement, UUID uuid, PPlayer player, Player bukkitPlayer) {
        if (!requirement.containsKey("type")) {
            plugin.logger(Level.WARNING, "Unable to test " + player.getLastUsername() + " as a required field in missing in requirement: " + requirement.toJSONString());
            return false;
        }
        String type = String.valueOf(requirement.get("type"));
        JSONObject rawPlayerStats = player.getPlayer();
        switch (type) {
            case "rankRequirement":
                if (!requirement.containsKey("value")) {
                    plugin.logger(Level.WARNING, "Field value or field missing from rankRequirement: " + requirement.toJSONString());
                    return false;
                }
                String currentRank = JohnyPerms.getJPermsAPI().getUser(uuid).getGroup().getName();
                if (currentRank == null || currentRank.trim().isEmpty()) {
                    plugin.logger(Level.WARNING, "Unable to get the rank of player " + bukkitPlayer.getName() + ".");
                    return false;
                }
                return String.valueOf(requirement.get("value")).equalsIgnoreCase(currentRank);

            case "greaterThenStatistic":
                if (!requirement.containsKey("value") || !requirement.containsKey("field")) {
                    plugin.logger(Level.WARNING, "Field value or field missing from greaterThenStatistic: " + requirement.toJSONString());
                    return false;
                }
                double total = 0;
                if (requirement.get("field") instanceof JSONArray) {
                    for (Object node : (JSONArray) requirement.get("field")) {
                        total = total + getFieldDouble(String.valueOf(node), rawPlayerStats);
                    }
                } else {
                    total = total + getFieldDouble(String.valueOf(requirement.get("field")), rawPlayerStats);
                }

                return total > Double.valueOf(String.valueOf(requirement.get("value")));
        }
        plugin.logger(Level.WARNING, "Unknown Requirement Type.");
        return false;

    }

    private String getFieldString(String field, JSONObject rawStats) {
        if (!rawStats.containsKey(field)) {
            plugin.logger(Level.WARNING, "Unable to find field " + field + " in a JStats profile.");
            return "";
        }
        return String.valueOf(rawStats.get(field));
    }

    private double getFieldDouble(String field, JSONObject rawStats) {
        String raw = getFieldString(field, rawStats);
        if (raw.isEmpty()) {
            return 0D;
        }
        return Double.valueOf(raw);
    }

    public String getRankupName() {
        return rankupName;
    }
}
