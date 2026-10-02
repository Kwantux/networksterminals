package de.kwantux.networks.terminals.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public final class BedrockCheck {

    private static FloodgateApi floodgateApi;
    private static boolean floodgatePresent = false;

    private BedrockCheck() {}

    public static void init(Plugin plugin) {
        Plugin floodgate = plugin.getServer().getPluginManager().getPlugin("floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            floodgateApi = FloodgateApi.getInstance();
            floodgatePresent = true;
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (floodgatePresent) {
            return floodgateApi.isFloodgatePlayer(uuid);
        }
        return uuid.getLeastSignificantBits() >= 0;
    }
}