package de.kwantux.networks.terminals.util;

import de.kwantux.networks.Network;
import de.kwantux.networks.terminals.TerminalsPlugin;
import org.bukkit.entity.Player;

public final class NetworkAccessPolicy {
    private NetworkAccessPolicy() {}

    public static boolean canOpen(Player player, Network network) {
        return isPublic(network)
                || network.owner().equals(player.getUniqueId())
                || network.users().contains(player.getUniqueId());
    }

    public static boolean isPublic(Network network) {
        return TerminalsPlugin.instance.getConfig().getStringList("public-networks").contains(network.name());
    }
}
