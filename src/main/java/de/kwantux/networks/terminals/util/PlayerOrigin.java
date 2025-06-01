package de.kwantux.networks.terminals.util;

import de.kwantux.networks.utils.Origin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlayerOrigin extends Origin {

    private final Player player;

    public PlayerOrigin(Player player) {
        this.player = player;
    }

    @Override
    public String toString() {
        return player.getName();
    }

    @Override
    public Component displayText() {
        return player.displayName();
    }
}
