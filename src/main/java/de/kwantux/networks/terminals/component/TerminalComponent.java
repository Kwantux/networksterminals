package de.kwantux.networks.terminals.component;


import de.kwantux.networks.component.BasicComponent;
import de.kwantux.networks.component.module.ActiveModule;
import de.kwantux.networks.component.util.ComponentType;
import de.kwantux.networks.component.module.Donator;
import de.kwantux.networks.component.module.Requestor;
import de.kwantux.networks.terminals.util.PlayerOrigin;
import de.kwantux.networks.utils.BlockLocation;
import de.kwantux.networks.utils.NamespaceUtils;
import de.kwantux.networks.utils.Origin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TerminalComponent extends BasicComponent implements ActiveModule {


    public static ComponentType type;

    public ComponentType type() {
        return type;
    }

    private final Player player;

    public TerminalComponent(Player player) {
        this.player = player;
    }

    private static final Map<String, Object> defaultProperties = new HashMap<>();

    static {
        defaultProperties.put(NamespaceUtils.RANGE.name, 0);
    }

    public static ComponentType register() {
        type = ComponentType.register(
                TerminalComponent.class,
                "terminal",
                Component.text("Input Container"),
                false,
                false,
                false,
                false,
                false,
                null, // non-persistent
                defaultProperties
        );
        return type;
    }

    @Override
    public int range() {
        return -1;
    }

    @Override
    public Map<String, Object> properties() {
        return new HashMap<>();
    }

    /**
     * @return the position of the player that opened the terminal
     */
    @Override
    public BlockLocation pos() {
        return new BlockLocation(player.getLocation());
    }

    @Override
    public Origin origin() {
        return new PlayerOrigin(player);
    }

    /**
     * @return true if the component is in a loaded chunk
     */
    @Override
    public boolean isLoaded() {
        return false;
    }

    /**
     * @return true if the component is ready for a transaction
     */
    @Override
    public boolean ready() {
        return false;
    }

    @Override
    public Inventory inventory() {
        return null;
    }
}
